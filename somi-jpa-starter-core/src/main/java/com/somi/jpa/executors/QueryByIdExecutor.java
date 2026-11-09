package com.somi.jpa.executors;

import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import com.somi.jpa.handler.QueryMethod;
import com.somi.jpa.mappers.UniversalResultTransformer;
import com.somi.jpa.mappers.converters.ValueConversionService;
import com.somi.jpa.providers.QueryByIdProvider;
import com.somi.jpa.utils.TypeUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.hibernate.query.NativeQuery;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryByIdExecutor {
    private static final Logger log = LoggerFactory.getLogger(QueryByIdExecutor.class);
    private static final Pattern PARAMETER = Pattern.compile("\\?(\\d*)");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TIMEOUT_HINT = "jakarta.persistence.query.timeout";

    private final QueryByIdProvider queryByIdProvider;
    private final EntityManager entityManager;
    private final ValueConversionService conversionService;
    private final QueryExecutionSettings settings;
    private final PersistenceExceptionTranslator exceptionTranslator = new HibernateJpaDialect();
    private final @Nullable TransactionTemplate readTransaction;
    private final @Nullable TransactionTemplate writeTransaction;
    private final Map<Class<?>, UniversalResultTransformer<?>> transformers = new ConcurrentHashMap<>();

    public QueryByIdExecutor(QueryByIdProvider queryByIdProvider, EntityManager entityManager,
                             ValueConversionService conversionService,
                             @Nullable PlatformTransactionManager transactionManager,
                             QueryExecutionSettings settings) {
        this.queryByIdProvider = queryByIdProvider;
        this.entityManager = entityManager;
        this.conversionService = conversionService;
        this.settings = settings;
        if (transactionManager != null) {
            this.writeTransaction = new TransactionTemplate(transactionManager);
            this.readTransaction = new TransactionTemplate(transactionManager);
            this.readTransaction.setReadOnly(true);
        } else {
            this.writeTransaction = null;
            this.readTransaction = null;
        }
    }

    public void validate(QueryMethod method) {
        if (!queryByIdProvider.contains(method.queryId())) {
            throw NativeQueryException.of(ErrorMessage.QUERY_ID_NOT_FOUND_FOR_METHOD, method.queryId(), method.name());
        }
    }

    public @Nullable Object execute(QueryMethod method, @Nullable Object @Nullable [] args) {
        TransactionTemplate transaction = method.modifying() ? writeTransaction : readTransaction;
        try {
            return transaction == null
                    ? doExecute(method, args)
                    : transaction.execute(_ -> doExecute(method, args));
        } catch (DataAccessException | NativeQueryException e) {
            throw e;
        } catch (RuntimeException e) {
            DataAccessException translated = exceptionTranslator.translateExceptionIfPossible(e);
            throw translated != null ? translated : e;
        }
    }

    private @Nullable Object doExecute(QueryMethod method, @Nullable Object @Nullable [] args) {
        String sql = queryByIdProvider.getQuery(method.queryId());
        logQuery(method, sql, args);
        if (method.modifying()) {
            return executeUpdate(method, sql, args);
        }
        Class<?> resultType = method.resultType();
        boolean entity = resultType.isAnnotationPresent(Entity.class);
        boolean scalar = !entity && TypeUtils.isSimpleType(resultType);
        NativeQuery<?> query = unwrap(entity ? entityManager.createNativeQuery(sql, resultType) : entityManager.createNativeQuery(sql));
        bind(query, method, args);
        applyTimeout(query);
        applyRowLimit(query, method);
        if (!entity && !scalar) {
            query.setTupleTransformer(transformerFor(resultType));
        }
        List<?> rows = query.getResultList();
        if (scalar) {
            rows = rows.stream().map(value -> conversionService.convert(value, resultType)).toList();
        }
        return shapeResult(method, rows);
    }

    private @Nullable Object executeUpdate(QueryMethod method, String sql, @Nullable Object @Nullable [] args) {
        if (method.flushAutomatically()) {
            entityManager.flush();
        }
        NativeQuery<?> query = unwrap(entityManager.createNativeQuery(sql));
        bind(query, method, args);
        applyTimeout(query);
        int rowsAffected = query.executeUpdate();
        if (method.clearAutomatically()) {
            entityManager.clear();
        }
        log.debug("Query [{}] affected {} rows", method.queryId(), rowsAffected);
        Class<?> containerType = method.containerType();
        if (containerType == int.class || containerType == Integer.class) return rowsAffected;
        if (containerType == long.class || containerType == Long.class) return (long) rowsAffected;
        return null;
    }

    private void applyTimeout(NativeQuery<?> query) {
        if (settings.queryTimeout() != null) {
            query.setHint(TIMEOUT_HINT, Math.toIntExact(settings.queryTimeout().toMillis()));
        }
    }

    private void applyRowLimit(NativeQuery<?> query, QueryMethod method) {
        if (!method.returnsCollection()) {
            query.setMaxResults(2);
        } else if (settings.maxRows() != null) {
            query.setMaxResults(settings.maxRows() + 1);
        }
    }

    private @Nullable Object shapeResult(QueryMethod method, List<?> rows) {
        if (method.returnsCollection()) {
            if (settings.maxRows() != null && rows.size() > settings.maxRows()) {
                throw NativeQueryException.of(ErrorMessage.MAX_ROWS_EXCEEDED, method.queryId(), settings.maxRows());
            }
            return toCollection(method, rows);
        }
        if (rows.size() > 1) {
            throw new IncorrectResultSizeDataAccessException(
                    "Query " + method.queryId() + " returned more than one row for " + method.name(), 1, rows.size());
        }
        Object first = rows.isEmpty() ? null : rows.getFirst();
        return method.returnsOptional() ? Optional.ofNullable(first) : first;
    }

    private static Collection<Object> toCollection(QueryMethod method, List<?> rows) {
        Class<?> containerType = method.containerType();
        Collection<Object> collection = containerType == Collection.class || containerType == List.class
                ? new ArrayList<>(rows.size())
                : CollectionFactory.createCollection(containerType, method.resultType(), rows.size());
        collection.addAll(rows);
        return collection;
    }

    private UniversalResultTransformer<?> transformerFor(Class<?> resultType) {
        return transformers.computeIfAbsent(resultType, type -> new UniversalResultTransformer<>(type, conversionService));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bind(NativeQuery query, QueryMethod method, @Nullable Object @Nullable [] args) {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            int position = i + 1;
            Object value = args[i];
            Class<?> declaredType = method.parameterTypes().get(i);
            if (value instanceof Enum<?> constant) {
                query.setParameter(position, constant.name());
            } else if (value != null || Collection.class.isAssignableFrom(declaredType) || declaredType.isArray()
                    || declaredType == Object.class) {
                query.setParameter(position, value);
            } else {
                Class bindType = declaredType.isEnum() ? String.class : ClassUtils.resolvePrimitiveIfNecessary(declaredType);
                query.setParameter(position, null, bindType);
            }
        }
    }

    private static NativeQuery<?> unwrap(Query query) {
        try {
            return query.unwrap(NativeQuery.class);
        } catch (PersistenceException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.FAILED_TO_UNWRAP_QUERY);
        }
    }

    private void logQuery(QueryMethod method, String sql, @Nullable Object @Nullable [] args) {
        if (!log.isDebugEnabled()) return;
        if (settings.logParameterValues()) {
            log.debug("Executing query [{}]:\n{}", method.queryId(), expandSql(sql, args));
        } else {
            log.debug("Executing query [{}] with {} parameter(s):\n{}", method.queryId(), args == null ? 0 : args.length, sql);
        }
    }

    private static String expandSql(String sql, @Nullable Object @Nullable [] args) {
        if (args == null || args.length == 0) return sql;
        AtomicInteger next = new AtomicInteger();
        Matcher matcher = PARAMETER.matcher(sql);
        return matcher.replaceAll(match -> {
            int index = match.group(1).isEmpty() ? next.getAndIncrement() : Integer.parseInt(match.group(1)) - 1;
            String replacement = index >= 0 && index < args.length ? toLiteral(args[index]) : match.group();
            return Matcher.quoteReplacement(replacement);
        });
    }

    private static String toLiteral(@Nullable Object param) {
        return switch (param) {
            case null -> "null";
            case Boolean b -> Boolean.TRUE.equals(b) ? "1" : "0";
            case BigDecimal bd -> bd.toPlainString();
            case Number n -> n.toString();
            case Enum<?> e -> quote(e.name());
            case UUID uuid -> quote(uuid.toString());
            case java.sql.Date d -> quote(d.toLocalDate().format(DateTimeFormatter.ISO_DATE));
            case java.sql.Time t -> quote(t.toLocalTime().format(DateTimeFormatter.ISO_TIME));
            case java.util.Date d -> quote(d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_TIME));
            case LocalDateTime ldt -> quote(ldt.format(DATE_TIME));
            case LocalDate ld -> quote(ld.format(DateTimeFormatter.ISO_DATE));
            case LocalTime lt -> quote(lt.format(DateTimeFormatter.ISO_TIME));
            default -> quote(param.toString());
        };
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
