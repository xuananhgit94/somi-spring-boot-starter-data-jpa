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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the native query behind a {@link QueryMethod} and shapes the result to the declared return type.
 */
public class QueryByIdExecutor {
    private static final Logger log = LoggerFactory.getLogger(QueryByIdExecutor.class);
    private static final Pattern PARAMETER = Pattern.compile("\\?(\\d*)");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final QueryByIdProvider queryByIdProvider;
    private final EntityManager entityManager;
    private final ValueConversionService conversionService;
    // Transformers are thread-safe and hold per-type reflection metadata, so share them across calls
    private final Map<Class<?>, UniversalResultTransformer<?>> transformers = new ConcurrentHashMap<>();

    /**
     * @param entityManager a shared, transaction-bound EntityManager (e.g. from {@code SharedEntityManagerCreator})
     */
    public QueryByIdExecutor(QueryByIdProvider queryByIdProvider, EntityManager entityManager,
                             ValueConversionService conversionService) {
        this.queryByIdProvider = queryByIdProvider;
        this.entityManager = entityManager;
        this.conversionService = conversionService;
    }

    public @Nullable Object execute(QueryMethod method, @Nullable Object @Nullable [] args) {
        String sql = queryByIdProvider.getQuery(method.queryId());
        if (log.isDebugEnabled()) {
            log.debug("Executing query [{}]:\n{}", method.queryId(), expandSql(sql, args));
        }
        if (method.modifying()) {
            return executeUpdate(method, sql, args);
        }
        Class<?> resultType = method.resultType();
        List<?> rows;
        if (resultType.isAnnotationPresent(Entity.class)) {
            rows = bind(entityManager.createNativeQuery(sql, resultType), args).getResultList();
        } else if (TypeUtils.isSimpleType(resultType)) {
            rows = bind(entityManager.createNativeQuery(sql), args).getResultList().stream()
                    .map(value -> conversionService.convert(value, resultType))
                    .toList();
        } else {
            NativeQuery<?> nativeQuery = unwrap(bind(entityManager.createNativeQuery(sql), args));
            nativeQuery.setTupleTransformer(transformerFor(resultType));
            rows = nativeQuery.getResultList();
        }
        return shapeResult(method, rows);
    }

    private @Nullable Object executeUpdate(QueryMethod method, String sql, @Nullable Object @Nullable [] args) {
        int rowsAffected = bind(entityManager.createNativeQuery(sql), args).executeUpdate();
        log.debug("Query [{}] affected {} rows", method.queryId(), rowsAffected);
        Class<?> containerType = method.containerType();
        if (containerType == int.class || containerType == Integer.class) return rowsAffected;
        if (containerType == long.class || containerType == Long.class) return (long) rowsAffected;
        return null;
    }

    private static @Nullable Object shapeResult(QueryMethod method, List<?> rows) {
        if (method.returnsCollection()) {
            return Set.class.isAssignableFrom(method.containerType()) ? new LinkedHashSet<>(rows) : rows;
        }
        Object first = rows.isEmpty() ? null : rows.getFirst();
        return method.returnsOptional() ? Optional.ofNullable(first) : first;
    }

    private UniversalResultTransformer<?> transformerFor(Class<?> resultType) {
        return transformers.computeIfAbsent(resultType, type -> new UniversalResultTransformer<>(type, conversionService));
    }

    private static Query bind(Query query, @Nullable Object @Nullable [] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                query.setParameter(i + 1, args[i]);
            }
        }
        return query;
    }

    private static NativeQuery<?> unwrap(Query query) {
        try {
            return query.unwrap(NativeQuery.class);
        } catch (PersistenceException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.FAILED_TO_UNWRAP_QUERY);
        }
    }

    /** Inlines parameters into the SQL for debug logging only; supports both {@code ?} and {@code ?1}. */
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
