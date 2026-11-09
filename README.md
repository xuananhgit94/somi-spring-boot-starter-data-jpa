# somi-spring-boot-starter-data-jpa

Declarative native SQL for Spring Boot: write queries in XML, call them through an interface, get DTOs,
records, projections, entities or scalars back.

Requires Java 25 and Spring Boot 4.1+.

Full guide (Vietnamese): [docs/huong-dan-su-dung.md](docs/huong-dan-su-dung.md)

## Usage

```xml
<dependency>
    <groupId>com.somi</groupId>
    <artifactId>somi-spring-boot-starter-data-jpa</artifactId>
    <version>0.0.1</version>
</dependency>
```

`src/main/resources/queries/person.xml`:

```xml
<queries>
    <query id="person.findActive"><![CDATA[
        SELECT id, full_name, created_at FROM person WHERE status = ?1
    ]]></query>
    <query id="person.deactivate"><![CDATA[
        UPDATE person SET status = 'INACTIVE' WHERE id = ?1
    ]]></query>
</queries>
```

```java
@SpringBootApplication
@EnableNativeQueries
class Application { }

interface PersonQueries {
    record PersonRow(Long id, String fullName, LocalDateTime createdAt) {}

    @QueryId("person.findActive")
    List<PersonRow> findActive(String status);        // also Set, Optional, single value, scalar (Long, String...)

    @QueryId(value = "person.deactivate", type = QueryType.MODIFIER)
    int deactivate(Long id);                           // void, int or long

    default Optional<PersonRow> firstActive() {        // default methods run as written
        return findActive("ACTIVE").stream().findFirst();
    }
}
```

Columns map to record components, setters and projection getters by name, ignoring case and
underscores (`full_name` and `address_line_1` match `fullName` and `addressLine1`).

- A method returning a single value fails with `IncorrectResultSizeDataAccessException` when several rows match.
- Collection return types may be `List`, `Set`, `Collection` or concrete/sorted types such as `TreeSet`.
- Enum parameters are bound by `name()`; `null` parameters are bound with the declared parameter type.
- Every `@QueryId` is checked against the loaded XML at startup, so a typo fails the application start.
- Repository interfaces may extend generic base interfaces; generic interfaces themselves are not registered.

## Transactions

When the application has a single `PlatformTransactionManager`, read queries run in a read-only
transaction and modifying queries in a read-write one. Both join the caller's transaction when there is one,
so calling a repository from a `@Transactional` service behaves as usual.

Native `UPDATE`/`DELETE` statements bypass the persistence context. Use the `@QueryId` flags when entities
loaded in the same transaction must see the change:

```java
@QueryId(value = "person.rename", type = QueryType.MODIFIER,
         flushAutomatically = true, clearAutomatically = true)
int rename(Long id, String name);
```

JPA and Hibernate errors are translated to Spring's `DataAccessException` hierarchy
(e.g. `DataIntegrityViolationException`).

## Configuration

| Property                         | Default                        | Meaning                                                      |
|----------------------------------|--------------------------------|--------------------------------------------------------------|
| `somi.jpa.query-locations`       | `classpath*:/queries/**/*.xml` | XML query files                                              |
| `somi.jpa.log-parameter-values`  | `false`                        | Inline parameter values into the DEBUG SQL log               |
| `somi.jpa.query-timeout`         | none                           | Timeout for every query, e.g. `10s`                          |
| `somi.jpa.max-rows`              | none                           | Collection queries returning more rows fail with `MAX_ROWS_EXCEEDED` |

Keep `log-parameter-values` off in production: parameters often hold personal data.
Query ids are global across all jars on the classpath, so prefix them (`orders.findById`).

JSON columns are read with the application's Jackson 3 `JsonMapper` when one is defined.

## Errors

Every failure is a `com.somi.jpa.exceptions.NativeQueryException`. Its message is English and never
contains row values. It implements `MessageSourceResolvable`, so to localize it add its code
(`exception.getError().getCode()`, e.g. `somi.jpa.error.query.not-found`) to your `messages.properties`
and call `messageSource.getMessage(exception, locale)`.
