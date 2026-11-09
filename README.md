# somi-spring-boot-starter-data-jpa

Declarative native SQL for Spring Boot: write queries in XML, call them through an interface, get DTOs,
records, projections, entities or scalars back.

Requires Java 25 and Spring Boot 4.1+.

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

Columns map to properties by name, ignoring case and underscores (`full_name` -> `fullName`).
Modifying queries must run inside a transaction.

## Configuration

| Property                   | Default                         |
|----------------------------|---------------------------------|
| `somi.jpa.query-locations` | `classpath*:/queries/**/*.xml`  |

JSON columns are read with the application's Jackson 3 `JsonMapper` when one is defined.

## Errors

Every failure is a `com.somi.jpa.exceptions.NativeQueryException`. Its message is English and never
contains row values. It implements `MessageSourceResolvable`, so to localize it add its code
(`exception.getError().getCode()`, e.g. `somi.jpa.error.query.not-found`) to your `messages.properties`
and call `messageSource.getMessage(exception, locale)`.

## Migrating from koach-spring-boot-starter-data-jpa

| Before                                         | After                                              |
|------------------------------------------------|----------------------------------------------------|
| `com.koach:koach-spring-boot-starter-data-jpa` | `com.somi:somi-spring-boot-starter-data-jpa`       |
| package `com.koach.jpa.*`                      | `com.somi.jpa.*`                                   |
| `com.koach.jpa.anotations`                     | `com.somi.jpa.annotations`                         |
| `com.koach.jpa.configs.NativeQueryException`   | `com.somi.jpa.exceptions.NativeQueryException`     |
| property `queries.path-queries`                | `somi.jpa.query-locations`                         |
| default `classpath:/queries/**/*.xml`          | `classpath*:/queries/**/*.xml` (scans all jars)    |
| `MessageResolver` / `DefaultNativeQueryException` | removed, see [Errors](#errors)                  |
| message keys `error.*`                         | `somi.jpa.error.*`, `{0}`-style placeholders       |
| Java 21, Spring Boot 3.5, Jackson 2            | Java 25, Spring Boot 4.1, Jackson 3                |
