package com.somi.jpa.executors;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.enums.QueryType;
import com.somi.jpa.exceptions.DefaultNativeQueryException;
import com.somi.jpa.mappers.UniversalResultTransformer;
import com.somi.jpa.providers.QueryByIdProvider;
import com.somi.jpa.providers.SafeEntityManagerProvider;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@RequiredArgsConstructor
@Slf4j
public class QueryByIdExecutor {
    private final QueryByIdProvider queryByIdProvider;
    private SafeEntityManagerProvider safeEntityManagerProvider;

    @Autowired
    public void setSafeEntityManagerProvider(SafeEntityManagerProvider safeEntityManagerProvider) {
        this.safeEntityManagerProvider = safeEntityManagerProvider;
    }
    @SuppressWarnings("unchecked")
    public <T> T execute(String queryId, Object[] args, Class<?> containerType, Class<T> resultType, QueryType queryType) {
        String sql = queryByIdProvider.getQuery(queryId);
        if (StringUtils.isBlank(sql)) {
            DefaultNativeQueryException.fail(ErrorMessage.QUERY_ID_NOT_FOUND, queryId);
        }
        if (log.isDebugEnabled()) {
            logExpandedSql(sql, args, queryId);
        }
        QueryType effectiveQueryType = determineEffectiveQueryType(queryType, containerType);
        Query query = prepareQuery(sql, args);
        if (effectiveQueryType == QueryType.MODIFIER) {
            int rowsAffected = query.executeUpdate();
            log.debug("Executed modifying query [{}], rows affected: {}", queryId, rowsAffected);
            if (containerType == void.class || containerType == Void.class) {
                return null;
            } else if (containerType == int.class || containerType == Integer.class) {
                return (T) Integer.valueOf(rowsAffected);
            } else {
                throw DefaultNativeQueryException.of(ErrorMessage.NOT_MODIFYING_QUERY, containerType.getName());
            }
        } else {
            boolean isOptional = containerType == Optional.class;
            boolean isCollection = Collection.class.isAssignableFrom(containerType);
            boolean isEntity = resultType.isAnnotationPresent(Entity.class);
            boolean isInterface = resultType.isInterface();
            if (isEntity) {
                return (T) handleEntityResult(query.unwrap(NativeQuery.class).addEntity(resultType), isCollection, isOptional);
            } else {
                return (T) handleDtoResult(query, resultType, isCollection, isOptional, isInterface);
            }
        }
    }
    private Object handleDtoResult(Query query, Class<?> dtoClass, boolean isCollection, boolean isOptional, boolean isInterface) {
        NativeQuery<Object> nativeQuery = unwrapTypedNativeQuery(query);
        nativeQuery.setTupleTransformer(new UniversalResultTransformer<>(dtoClass, isInterface));
        List<Object> resultList = nativeQuery.getResultList();
        if (isCollection) return resultList;
        Object single = resultList.isEmpty() ? null : resultList.getFirst();
        return isOptional ? Optional.ofNullable(single) : single;
    }
    @SuppressWarnings("unchecked")
    private NativeQuery<Object> unwrapTypedNativeQuery(Query query) {
        try {
            return query.unwrap(NativeQuery.class);
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.FAILED_TO_UNWRAP_QUERY);
        }
    }
    private Object handleEntityResult(Query query, boolean isCollection, boolean isOptional) {
        if (isCollection) return query.getResultList();
        Object single = safeSingle(query);
        return isOptional ? Optional.ofNullable(single) : single;
    }
    private Object safeSingle(Query query) {
        List<?> results = query.getResultList();
        return results.stream().findFirst().orElse(null);
    }
    private void logExpandedSql(String sql, Object[] args, String queryId) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 0;
        if (args == null || args.length == 0) {
            sb.append(sql);
        } else {
            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);
                if (c == '?') {
                    if (paramIndex < args.length) {
                        sb.append(toLiteral(args[paramIndex++]));
                    } else {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
        }
        log.debug("Expanded SQL for QueryId [{}]:\n{}", queryId, sb);
    }
    private QueryType determineEffectiveQueryType(QueryType declaredQueryType, Class<?> containerType) {
        if (declaredQueryType == QueryType.AUTO) {
            return (containerType == void.class || containerType == Void.class) ? QueryType.MODIFIER : QueryType.SELECT;
        }
        return declaredQueryType;
    }
    private Query prepareQuery(String sql, Object[] args) {
        Query query = safeEntityManagerProvider.getEntityManager().createNativeQuery(sql);
        if (args == null || args.length == 0) {
            return query;
        }
        for (int i = 0; i < args.length; i++) {
            query.setParameter(i + 1, args[i]);
        }
        return query;
    }
    private String toLiteral(Object param) {
        if (param == null) return "null";

        if (param instanceof Boolean b) {
            return b ? "1" : "0";
        }

        return switch (param) {
            case Number n -> toNumberLiteral(n);
            case CharSequence cs -> quote(cs.toString());
            case Enum<?> e -> quote(e.name());
            case UUID uuid -> quote(uuid.toString());
            case java.util.Date d -> quote(formatDate(d));
            case LocalDateTime ldt -> quote(ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            case LocalDate ld -> quote(ld.format(DateTimeFormatter.ISO_DATE));
            case LocalTime lt -> quote(lt.format(DateTimeFormatter.ISO_TIME));
            default -> quote(param.toString());
        };
    }
    private String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
    private String formatDate(java.util.Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    private String toNumberLiteral(Number n) {
        return (n instanceof BigDecimal bd) ? bd.toPlainString() : n.toString();
    }
}
