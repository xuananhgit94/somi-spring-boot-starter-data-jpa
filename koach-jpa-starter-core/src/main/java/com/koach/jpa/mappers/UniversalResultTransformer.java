package com.koach.jpa.mappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
public class UniversalResultTransformer<T> implements TupleTransformer<T>, ResultListTransformer<T> {
    private final Class<T> targetClass;
    private final boolean isInterface;
    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    private final Map<String, Method> setterCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UniversalResultTransformer(Class<T> targetClass, boolean isInterface) {
        this.targetClass = targetClass;
        this.isInterface = isInterface;
    }
    @Override
    public List<T> transformList(List<T> list) {
        return list;
    }

    @Override
    public T transformTuple(Object[] tuple, String[] aliases) {
        try {
            if (isInterface) {
                return createInterfaceProxy(tuple, aliases);
            }
            if (targetClass.isRecord()) {
                return createRecordInstance(tuple, aliases);
            } else {
                return createDtoInstance(tuple, aliases);
            }
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.FAILED_TO_MAPPER, targetClass.getSimpleName());
        }
    }
    private T createDtoInstance(Object[] tuple, String[] aliases) throws Exception {
        T instance = targetClass.getDeclaredConstructor().newInstance();
        Map<String, Object> dataMap = buildDataMap(tuple, aliases);
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String camelCaseAlias = toCamelCase(entry.getKey());
            Field field = getField(camelCaseAlias);
            if (field != null) {
                Method setter = getSetter(field);
                if (setter != null) {
                    Object convertedValue = convertValue(entry.getValue(), field.getType());
                    setter.invoke(instance, convertedValue);
                }
            }
        }
        return instance;
    }
    private Method getSetter(Field field) {
        return setterCache.computeIfAbsent(field.getName(), key -> {
            String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            try {
                return targetClass.getMethod(setterName, field.getType());
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }
    private String toCamelCase(String s) {
        if (s == null) return null;
        if (!s.contains("_")) {
            if (Character.isUpperCase(s.charAt(0))) {
                return Character.toLowerCase(s.charAt(0)) + s.substring(1);
            }
            return s;
        }
        String[] parts = s.split("_");
        StringBuilder camelCaseString = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            camelCaseString.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1).toLowerCase());
        }
        return camelCaseString.toString();
    }
    private Field getField(String alias) {
        return fieldCache.computeIfAbsent(alias, key -> {
            for (Field field : targetClass.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(key)) {
                    return field;
                }
            }
            return null;
        });
    }
    private T createRecordInstance(Object[] tuple, String[] aliases) throws Exception {
        RecordComponent[] components = targetClass.getRecordComponents();
        Class<?>[] paramTypes = Stream.of(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] args = new Object[components.length];
        Map<String, Object> dataMap = buildDataMap(tuple, aliases);
        for (int i = 0; i < components.length; i++) {
            String componentName = components[i].getName().toLowerCase();
            Object rawValue = dataMap.get(componentName);
            if (rawValue == null) {
                rawValue = dataMap.get(fromCamelCaseToSnakeCase(componentName));
            }
            args[i] = convertValue(rawValue, components[i].getType());
        }
        Constructor<T> constructor = targetClass.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }
    @SuppressWarnings("unchecked")
    private T createInterfaceProxy(Object[] tuple, String[] aliases) {
        Map<String, Object> dataMap = buildDataMap(tuple, aliases);
        return (T) Proxy.newProxyInstance(targetClass.getClassLoader(), new Class<?>[]{targetClass}, new InterfaceInvocationHandler(dataMap, this));
    }
    private Map<String, Object> buildDataMap(Object[] tuple, String[] aliases) {
        Map<String, Object> dataMap = new HashMap<>();
        for (int i = 0; i < aliases.length; i++) {
            if (aliases[i] != null) {
                dataMap.put(aliases[i].toLowerCase(), tuple[i]);
            }
        }
        return dataMap;
    }
    private String fromCamelCaseToSnakeCase(String camelCaseString) {
        if (camelCaseString == null || camelCaseString.isEmpty()) {
            return camelCaseString;
        }
        return camelCaseString.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
    Object convertValue(Object raw, Class<?> targetType) {
        if (raw == null || targetType.isAssignableFrom(raw.getClass())) {
            return raw;
        }

        return switch (raw) {
            case Number num -> convertNumber(num, targetType);
            case Boolean b -> b;
            case String s -> handleStringBased(targetType, s);
            case UUID uuid -> (targetType == String.class) ? uuid.toString() : raw;
            case java.sql.Date d -> handleSqlDate(d, targetType);
            case Timestamp ts -> handleTimestamp(ts, targetType);
            case Time t -> {
                if (targetType == LocalTime.class) yield t.toLocalTime();
                yield null;
            }
            case Array arr -> handleSqlArray(arr, targetType);

            default -> {
                if (isComplexObject(targetType)) {
                    try {
                        yield objectMapper.convertValue(raw, objectMapper.constructType(targetType));
                    } catch (IllegalArgumentException e) {
                        yield raw;
                    }
                }
                yield raw;
            }
        };
    }
    private boolean isComplexObject(Class<?> targetType) {
        return targetType != String.class &&
                (Collection.class.isAssignableFrom(targetType) || Map.class.isAssignableFrom(targetType) ||
                        (!targetType.isPrimitive() && !targetType.getName().startsWith("java.lang.")));
    }
    private Object handleSqlArray(Array sqlArray, Class<?> targetType) {
        if (!Collection.class.isAssignableFrom(targetType)) {
            return null;
        }
        try {
            Object[] array = (Object[]) sqlArray.getArray();
            return objectMapper.convertValue(Arrays.asList(array), objectMapper.constructType(targetType));
        } catch (SQLException e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.SQL_TO_ARRAY_FAIL);
        }
    }
    private Object handleTimestamp(Timestamp ts, Class<?> targetType) {
        if (targetType == Date.class) return new Date(ts.getTime());
        if (targetType == LocalDateTime.class) return ts.toLocalDateTime();
        return null;
    }
    private Object handleSqlDate(java.sql.Date date, Class<?> targetType) {
        if (targetType == Date.class) return new Date(date.getTime());
        if (targetType == LocalDate.class) return date.toLocalDate();
        return null;
    }
    private Object handleStringBased(Class<?> targetType, String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        String trimmedStr = str.trim();

        if (targetType.isEnum()) {
            return convertEnum(targetType.asSubclass(Enum.class), trimmedStr);
        }
        if (targetType == UUID.class) {
            try {
                return UUID.fromString(trimmedStr);
            } catch (IllegalArgumentException e) {
                throw DefaultNativeQueryException.of(e, ErrorMessage.SQL_TO_ARRAY_FAIL);
            }
        }
        if (targetType == Character.class || targetType == char.class) {
            if (trimmedStr.length() == 1) {
                return trimmedStr.charAt(0);
            }
            throw DefaultNativeQueryException.of(ErrorMessage.STRING_TO_CHARACTER_FAIL, trimmedStr);
        }
        if (isJsonToPojo(targetType)) {
            return handleJson(trimmedStr, targetType);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return "1".equals(trimmedStr) || "true".equalsIgnoreCase(trimmedStr) || "T".equalsIgnoreCase(trimmedStr);
        }
        return trimmedStr;
    }
    private Object handleJson(String jsonStr, Class<?> targetType) {
        try {
            return objectMapper.readValue(jsonStr, objectMapper.constructType(targetType));
        } catch (Exception e) {
            throw DefaultNativeQueryException.of(e, ErrorMessage.STRING_TO_JSON_FAIL, targetType.getSimpleName(), jsonStr);
        }
    }
    private boolean isJsonToPojo(Class<?> targetType) {
        return !targetType.isPrimitive() &&
                !targetType.getName().startsWith("java.lang.") &&
                !targetType.isEnum() &&
                !Date.class.isAssignableFrom(targetType) &&
                !LocalDate.class.isAssignableFrom(targetType) &&
                !LocalDateTime.class.isAssignableFrom(targetType) &&
                !LocalTime.class.isAssignableFrom(targetType) &&
                !UUID.class.isAssignableFrom(targetType);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertEnum(Class<? extends Enum> enumClass, String str) {
        try {
            return Enum.valueOf(enumClass, str.toUpperCase());
        } catch (IllegalArgumentException e) {
            for(Enum<?> constant : enumClass.getEnumConstants()){
                if(constant.name().equalsIgnoreCase(str)){
                    return constant;
                }
            }
            throw DefaultNativeQueryException.of(e, ErrorMessage.STRING_TO_ENUM_FAIL, str, enumClass.getSimpleName());
        }
    }
    private Object convertNumber(Number num, Class<?> targetType) {
        if (num == null) return null;
        if (targetType.isInstance(num)) return num;
        return switch (targetType.getName()) {
            case "java.lang.Long", "long" -> num.longValue();
            case "java.lang.Integer", "int" -> num.intValue();
            case "java.lang.Double", "double" -> num.doubleValue();
            case "java.lang.Float", "float" -> num.floatValue();
            case "java.lang.Short", "short" -> num.shortValue();
            case "java.lang.Byte", "byte" -> num.byteValue();
            case "java.math.BigDecimal" -> (num instanceof BigDecimal) ? num : new BigDecimal(num.toString());
            case "java.math.BigInteger" -> (num instanceof BigInteger) ? num : BigInteger.valueOf(num.longValue());
            default -> num;
        };
    }
    private static class InterfaceInvocationHandler implements InvocationHandler {
        private final Map<String, Object> dataMap;
        private final UniversalResultTransformer<?> parentTransformer;
        private final Map<Method, String> methodToPropertyNameCache = new ConcurrentHashMap<>();
        public InterfaceInvocationHandler(Map<String, Object> dataMap, UniversalResultTransformer<?> parentTransformer) {
            this.dataMap = dataMap;
            this.parentTransformer = parentTransformer;
        }
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            String propertyName = getPropertyNameFromMethod(method);
            Object rawValue = dataMap.get(propertyName.toLowerCase());

            if (rawValue == null) {
                String snakeCasePropertyName = parentTransformer.fromCamelCaseToSnakeCase(propertyName);
                rawValue = dataMap.get(snakeCasePropertyName);
            }
            return parentTransformer.convertValue(rawValue, method.getReturnType());
        }
        private String getPropertyNameFromMethod(Method method) {
            return methodToPropertyNameCache.computeIfAbsent(method, m -> {
                String methodName = m.getName();
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                } else if (methodName.startsWith("is") && methodName.length() > 2) {
                    return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                }
                return methodName;
            });
        }
    }
}
