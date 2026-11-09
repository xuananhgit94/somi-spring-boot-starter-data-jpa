# Hướng dẫn sử dụng somi-spring-boot-starter-data-jpa

Thư viện cho phép viết SQL native trong file XML và gọi chúng qua một interface Java. Kết quả được map tự động
sang record, DTO, interface projection, entity JPA hoặc giá trị đơn (số, chuỗi, ngày giờ...).

## Mục lục

1. [Yêu cầu](#1-yêu-cầu)
2. [Cài đặt](#2-cài-đặt)
3. [Bắt đầu nhanh](#3-bắt-đầu-nhanh)
4. [Viết file query XML](#4-viết-file-query-xml)
5. [Khai báo repository](#5-khai-báo-repository)
6. [Kiểu trả về](#6-kiểu-trả-về)
7. [Quy tắc map cột](#7-quy-tắc-map-cột)
8. [Tham số](#8-tham-số)
9. [Câu lệnh thay đổi dữ liệu và transaction](#9-câu-lệnh-thay-đổi-dữ-liệu-và-transaction)
10. [Cấu hình](#10-cấu-hình)
11. [Xử lý lỗi](#11-xử-lý-lỗi)
12. [Log và an toàn dữ liệu](#12-log-và-an-toàn-dữ-liệu)
13. [Viết test](#13-viết-test)
14. [Câu hỏi thường gặp](#14-câu-hỏi-thường-gặp)

## 1. Yêu cầu

| Thành phần   | Phiên bản                      |
|--------------|--------------------------------|
| Java         | 25 trở lên                     |
| Spring Boot  | 4.1 trở lên                    |
| Hibernate    | 7.x (đi kèm Spring Boot)       |
| Jackson      | 3.x (đi kèm Spring Boot)       |

Ứng dụng cần có đúng một `EntityManagerFactory` (cấu hình JPA mặc định của Spring Boot là đủ).

## 2. Cài đặt

Thư viện chưa được phát hành lên Maven Central. Cài vào Maven local từ mã nguồn:

```bash
git clone https://github.com/xuananhgit94/somi-spring-boot-starter-data-jpa.git
cd somi-spring-boot-starter-data-jpa
mvn install
```

Sau đó thêm vào `pom.xml` của ứng dụng:

```xml
<dependency>
    <groupId>com.somi</groupId>
    <artifactId>somi-spring-boot-starter-data-jpa</artifactId>
    <version>0.0.1</version>
</dependency>
```

Starter đã kéo theo `spring-boot-starter-data-jpa`; chỉ cần thêm driver của database (PostgreSQL, MySQL...).

## 3. Bắt đầu nhanh

**Bước 1:** bật tính năng trên lớp ứng dụng.

```java
@SpringBootApplication
@EnableNativeQueries
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

`@EnableNativeQueries` quét package của lớp được đánh dấu và các package con. Muốn quét package khác:

```java
@EnableNativeQueries(basePackages = {"com.example.orders", "com.example.billing"})
```

**Bước 2:** viết query trong `src/main/resources/queries/user.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<queries>
    <query id="FIND_BY_ID"><![CDATA[
        SELECT id, full_name, created_at
        FROM users
        WHERE id = ?1
    ]]></query>

    <query id="FIND_ACTIVE"><![CDATA[
        SELECT id, full_name, created_at
        FROM users
        WHERE status = ?1
        ORDER BY id
    ]]></query>
</queries>
```

**Bước 3:** khai báo interface, gắn `@QueryNamespace` bằng tên file (`user.xml` → `"user"`).

```java
@QueryNamespace("user")
public interface UserQueries {

    record UserRow(Long id, String fullName, LocalDateTime createdAt) {}

    @QueryId("FIND_BY_ID")
    Optional<UserRow> findById(Long id);

    @QueryId("FIND_ACTIVE")
    List<UserRow> findActive(String status);
}
```

**Bước 4:** inject và sử dụng như một bean bình thường.

```java
@Service
public class UserService {
    private final UserQueries userQueries;

    public UserService(UserQueries userQueries) {
        this.userQueries = userQueries;
    }

    public List<UserQueries.UserRow> activeUsers() {
        return userQueries.findActive("ACTIVE");
    }
}
```

## 4. Viết file query XML

- Mặc định thư viện đọc mọi file khớp `classpath*:/queries/**/*.xml`, kể cả file nằm trong các jar khác.
- Mỗi `<query>` cần thuộc tính `id` và nội dung SQL. Nên bọc SQL trong `<![CDATA[ ... ]]>` để dùng được `<`, `>`, `&`.
- `<query>` thiếu `id` hoặc thiếu SQL sẽ bị bỏ qua kèm cảnh báo trong log.
- File có `<!DOCTYPE ...>` bị từ chối để chống tấn công XXE. Không khai báo DOCTYPE trong file query.

### Namespace và tính duy nhất của `id`

Mỗi file XML là một **namespace**. Mặc định namespace là tên file bỏ phần đuôi:

| File                          | Namespace  |
|-------------------------------|------------|
| `queries/user.xml`            | `user`     |
| `queries/orders/order.xml`    | `order`    |
| `queries/report/revenue.xml`  | `revenue`  |

`id` chỉ cần **duy nhất trong một file** (chính xác hơn: trong một namespace). Hai file khác nhau được
dùng cùng tên `id`:

```xml
<!-- queries/user.xml -->
<queries>
    <query id="FIND_BY_ID">SELECT id, full_name FROM users WHERE id = ?1</query>
</queries>
```

```xml
<!-- queries/order.xml -->
<queries>
    <query id="FIND_BY_ID">SELECT id, total FROM orders WHERE id = ?1</query>
</queries>
```

Bên trong thư viện, mỗi query được định danh bằng `namespace.id`: `user.FIND_BY_ID` và `order.FIND_BY_ID`.

Đặt namespace khác tên file bằng thuộc tính `namespace`, ví dụ khi hai thư mục có file cùng tên:

```xml
<!-- queries/admin/user.xml -->
<queries namespace="admin-user">
    <query id="FIND_BY_ID">...</query>
</queries>
```

Ứng dụng **không khởi động được** nếu:

- một file khai báo cùng `id` hai lần;
- hai file có cùng namespace (cùng tên file, hoặc cùng giá trị `namespace`) cùng khai báo một `id`.

`id` phân biệt hoa thường (`FIND_BY_ID` khác `find_by_id`). Nên thống nhất một kiểu đặt tên, ví dụ
chữ hoa và gạch dưới: `FIND_BY_ID`, `FIND_ACTIVE`, `COUNT_BY_STATUS`, `UPDATE_STATUS`.

Có thể chia query ra nhiều file và nhiều thư mục con:

```
src/main/resources/queries/
├── user.xml           → namespace "user"
├── order.xml          → namespace "order"
└── report/
    └── revenue.xml    → namespace "revenue"
```

## 5. Khai báo repository

Một interface được coi là repository khi nó có ít nhất một method gắn `@QueryId` và nằm trong package được quét.
Thư viện tạo bean với tên là tên interface viết thường chữ đầu (`OrderQueries` → `orderQueries`).

```java
@QueryNamespace("order")
public interface OrderQueries {

    @QueryId("FIND_BY_ID")
    Optional<OrderRow> findById(Long id);

    @QueryId(value = "CANCEL", type = QueryType.MODIFIER)
    int cancel(Long id);

    @QueryId(value = "FIND_BY_ID", namespace = "user")
    Optional<UserRow> findCustomer(Long userId);

    default boolean exists(Long id) {
        return findById(id).isPresent();
    }
}
```

**Cách xác định query của một method**, theo thứ tự ưu tiên:

| Khai báo                                              | Query được dùng      |
|-------------------------------------------------------|----------------------|
| `@QueryId(value = "FIND_BY_ID", namespace = "user")`  | `user.FIND_BY_ID`    |
| `@QueryNamespace("order")` trên interface + `@QueryId("FIND_BY_ID")` | `order.FIND_BY_ID` |
| Không có namespace nào: `@QueryId("order.FIND_BY_ID")` | `order.FIND_BY_ID` (ghi đầy đủ) |

`@QueryNamespace` đặt trên interface cha cũng được áp dụng cho interface con.

Các thuộc tính của `@QueryId`:

| Thuộc tính            | Mặc định        | Ý nghĩa                                                                  |
|-----------------------|-----------------|--------------------------------------------------------------------------|
| `value`               | bắt buộc        | `id` của query trong file XML                                            |
| `namespace`           | `""`            | Namespace riêng cho method này, ghi đè `@QueryNamespace` của interface   |
| `type`                | `AUTO`          | `SELECT`, `MODIFIER` hoặc `AUTO` (method trả về `void` được coi là `MODIFIER`) |
| `flushAutomatically`  | `false`         | Chỉ cho `MODIFIER`: flush persistence context trước khi chạy             |
| `clearAutomatically`  | `false`         | Chỉ cho `MODIFIER`: clear persistence context sau khi chạy               |

Nhiều method có thể dùng chung một query, ví dụ để nhận kết quả ở các dạng khác nhau:

```java
@QueryId("FIND_ACTIVE")
List<UserRow> findActive(String status);

@QueryId("FIND_ACTIVE")
List<User> findActiveEntities(String status);
```

Các quy tắc khác:

- Method `default` chạy đúng phần thân của nó, không gọi query.
- Có thể kế thừa từ interface cha generic, ví dụ `interface UserQueries extends BaseQueries<UserRow>`.
  Interface generic (có tham số kiểu) không tự trở thành bean.
- Method không có `@QueryId` (ví dụ kế thừa từ interface khác) không làm ứng dụng lỗi khi khởi động,
  nhưng sẽ ném `NativeQueryException` nếu bị gọi.
- Nếu đã có bean khác trùng tên với repository, ứng dụng không khởi động được. Đổi tên interface để tránh trùng.

**Kiểm tra lúc khởi động.** Khi ứng dụng khởi động, mọi method `@QueryId` đều được kiểm tra:
query `namespace.id` phải tồn tại, kiểu trả về phải xác định được, và method `MODIFIER` phải trả về
`void`, `int` hoặc `long`. Lỗi sai chính tả `id` hoặc namespace vì vậy được phát hiện ngay chứ không đợi tới lúc chạy.

## 6. Kiểu trả về

**Kiểu bao ngoài:**

| Khai báo                                      | Kết quả                                                       |
|-----------------------------------------------|---------------------------------------------------------------|
| `List<T>`, `Collection<T>`                    | `ArrayList`, giữ thứ tự của SQL                               |
| `Set<T>`                                      | `LinkedHashSet`, giữ thứ tự, loại phần tử trùng               |
| `SortedSet<T>`, `TreeSet<T>`, `ArrayList<T>`… | Đúng kiểu đã khai báo                                         |
| `Optional<T>`                                 | `Optional.empty()` khi không có dòng nào                      |
| `T`                                           | `null` khi không có dòng nào                                  |

Method trả về **một** giá trị (`T` hoặc `Optional<T>`) mà query ra từ hai dòng trở lên sẽ ném
`IncorrectResultSizeDataAccessException`. Hãy thêm điều kiện `WHERE` hoặc `LIMIT 1` nếu chỉ cần một dòng.

**Kiểu phần tử `T`:**

| Loại                | Điều kiện                                         | Ví dụ                                   |
|---------------------|---------------------------------------------------|-----------------------------------------|
| Record              | Mỗi component khớp một cột                        | `record PersonRow(Long id, String fullName)` |
| DTO                 | Có constructor không tham số và setter public     | `class PersonDto { setId(..); setFullName(..); }` |
| Interface projection| Getter `getXxx()` / `isXxx()` khớp cột            | `interface PersonView { Long getId(); }`|
| Entity JPA          | Lớp có `@Entity`; SQL phải trả đủ cột của entity  | `List<Person>`                          |
| Giá trị đơn         | Query trả về một cột                              | `Long`, `String`, `BigDecimal`, `LocalDate`, `UUID`, enum |

Entity trả về là entity được quản lý trong transaction hiện tại. Gọi ngoài transaction, entity sẽ ở trạng thái
detached: truy cập quan hệ lazy sẽ ném `LazyInitializationException`.

Interface projection hỗ trợ method `default`, `equals` và `hashCode` theo dữ liệu.
`toString()` chỉ in tên cột, không in giá trị.

## 7. Quy tắc map cột

Tên cột được so khớp với tên thuộc tính **không phân biệt hoa thường và bỏ qua dấu gạch dưới**:

| Cột trong SQL        | Thuộc tính Java     |
|----------------------|---------------------|
| `full_name`          | `fullName`          |
| `FULL_NAME`          | `fullName`          |
| `address_line_1`     | `addressLine1`      |
| `fullname`           | `fullName`          |

Cột không khớp thuộc tính nào bị bỏ qua. Thuộc tính không có cột tương ứng nhận `null`
(record có component kiểu nguyên thủy như `int`, `boolean` sẽ báo lỗi map nếu thiếu cột).

Dùng alias trong SQL khi tên cột khác tên thuộc tính:

```sql
SELECT p.id, p.name AS full_name, COUNT(o.id) AS order_count
FROM person p LEFT JOIN orders o ON o.person_id = p.id
GROUP BY p.id, p.name
```

**Chuyển đổi kiểu dữ liệu tự động:**

| Giá trị từ database                           | Kiểu Java nhận được                                   |
|-----------------------------------------------|-------------------------------------------------------|
| Số (`BIGINT`, `NUMERIC`, `INTEGER`...)        | `int`, `long`, `Integer`, `Long`, `BigDecimal`, `BigInteger`, `double`... |
| Chuỗi                                         | `String`, `char`, `boolean` (`1`, `true`, `T`), enum (không phân biệt hoa thường), `UUID` |
| `TIMESTAMP`, `DATE`, `TIME`                   | `LocalDateTime`, `LocalDate`, `LocalTime`, `Instant`, `java.util.Date` |
| Chuỗi ngày giờ (`2024-01-02 10:15:30`)         | Các kiểu ngày giờ ở trên                             |
| JSON (chuỗi, cột `json`/`jsonb` của PostgreSQL)| POJO, `Map`, `List` (đọc bằng Jackson)               |
| Mảng SQL (`text[]`, `int[]`...)                | `List<T>`, `Set<T>`, mảng Java                       |

Chuỗi rỗng hoặc chỉ có khoảng trắng được chuyển thành `null` (trừ khi kiểu đích là `String`).
Thời gian không có múi giờ được hiểu theo múi giờ của JVM.

## 8. Tham số

Tham số được truyền theo **vị trí**, khớp với `?1`, `?2`... trong SQL theo thứ tự tham số của method:

```xml
<!-- queries/order.xml -->
<query id="SEARCH"><![CDATA[
    SELECT id, total FROM orders
    WHERE customer_id = ?1 AND created_at >= ?2 AND status IN (?3)
]]></query>
```

```java
@QueryNamespace("order")
public interface OrderQueries {
    @QueryId("SEARCH")
    List<OrderRow> search(Long customerId, LocalDateTime from, List<String> statuses);
}
```

- Một tham số có thể dùng nhiều lần trong SQL (`?1 ... ?1`).
- `Collection` có thể dùng cho mệnh đề `IN (?n)`.
- Enum được truyền theo `name()` (ví dụ `Status.ACTIVE` → `'ACTIVE'`).
- `null` được truyền kèm kiểu đã khai báo của tham số, nên dùng được với mẫu lọc tùy chọn:

```sql
WHERE ?1 IS NULL OR status = ?1
```

- Tham số luôn được bind qua JDBC, không ghép chuỗi, nên an toàn trước SQL injection.
  Không hỗ trợ SQL động (ghép tên bảng, tên cột, `ORDER BY` theo tham số).

## 9. Câu lệnh thay đổi dữ liệu và transaction

**Transaction tự động.** Khi ứng dụng có đúng một `PlatformTransactionManager` (cấu hình JPA mặc định):

| Loại method  | Transaction                                                   |
|--------------|---------------------------------------------------------------|
| `SELECT`     | Transaction chỉ đọc                                           |
| `MODIFIER`   | Transaction đọc-ghi                                           |

Cả hai đều **tham gia transaction đang có** của nơi gọi. Gọi từ một method `@Transactional` của service thì
toàn bộ chạy trong transaction đó và rollback cùng nhau khi có lỗi.

```java
@Transactional
public void closeOrder(Long id) {
    orderQueries.cancel(id);
    auditQueries.log(id, "CANCELLED");
}
```

Nếu ứng dụng có nhiều `PlatformTransactionManager`, thư viện không tự mở transaction;
hãy gọi repository từ service có `@Transactional` chỉ rõ transaction manager.

**Đồng bộ với persistence context.** Câu `UPDATE`/`DELETE` native ghi thẳng xuống database, không cập nhật các
entity đã được nạp trong cùng transaction. Dùng hai cờ sau khi cần:

```java
@QueryId(value = "RENAME", type = QueryType.MODIFIER,
         flushAutomatically = true, clearAutomatically = true)
int rename(Long id, String fullName);
```

- `flushAutomatically = true`: ghi các thay đổi entity đang chờ xuống database trước khi chạy câu lệnh.
- `clearAutomatically = true`: xóa persistence context sau khi chạy, để lần đọc entity tiếp theo lấy dữ liệu mới.
  Lưu ý: mọi entity đang được quản lý trong transaction sẽ bị detached.

**Giá trị trả về của `MODIFIER`:** `int`/`Integer` hoặc `long`/`Long` là số dòng bị ảnh hưởng; `void` không trả về gì.

## 10. Cấu hình

Khai báo trong `application.yml` hoặc `application.properties`:

```yaml
somi:
  jpa:
    query-locations:
      - classpath*:/queries/**/*.xml
      - classpath*:/reports/**/*.xml
    log-parameter-values: false
    query-timeout: 10s
    max-rows: 10000
```

| Thuộc tính                       | Mặc định                       | Ý nghĩa                                                                 |
|----------------------------------|--------------------------------|-------------------------------------------------------------------------|
| `somi.jpa.query-locations`       | `classpath*:/queries/**/*.xml` | Vị trí các file query. Khai báo thuộc tính này sẽ **thay thế** giá trị mặc định |
| `somi.jpa.log-parameter-values`  | `false`                        | In giá trị tham số vào log DEBUG                                        |
| `somi.jpa.query-timeout`         | không giới hạn                 | Thời gian tối đa cho mỗi query, ví dụ `5s`, `500ms`                     |
| `somi.jpa.max-rows`              | không giới hạn                 | Query trả về tập hợp có nhiều hơn số dòng này sẽ ném lỗi `MAX_ROWS_EXCEEDED` thay vì nạp hết vào bộ nhớ |

**Jackson.** Cột JSON được đọc bằng `JsonMapper` của ứng dụng nếu có đúng một bean `JsonMapper`
(cấu hình `spring.jackson.*` được áp dụng); nếu không, thư viện dùng `JsonMapper` mặc định.

**Thay thế bean.** Các bean `QueryByIdProvider`, `ValueConversionService` và `QueryByIdExecutor` đều có
`@ConditionalOnMissingBean`, có thể tự định nghĩa để thay thế khi cần.

## 11. Xử lý lỗi

**Lỗi database** (vi phạm ràng buộc, sai cú pháp SQL, timeout...) được chuyển sang cây exception chuẩn
`DataAccessException` của Spring:

```java
try {
    orderQueries.insert(id, total);
} catch (DataIntegrityViolationException e) {
    throw new OrderAlreadyExistsException(id);
}
```

**Lỗi của thư viện** (sai khai báo, thiếu query, chuyển đổi kiểu thất bại...) là
`com.somi.jpa.exceptions.NativeQueryException`. Dùng `getError()` để biết loại lỗi:

```java
catch (NativeQueryException e) {
    if (e.getError() == ErrorMessage.MAX_ROWS_EXCEEDED) { ... }
}
```

Thông báo lỗi bằng tiếng Anh và **không chứa dữ liệu của dòng**, an toàn để ghi log.

**Dịch thông báo lỗi.** `NativeQueryException` implement `MessageSourceResolvable`. Thêm mã lỗi vào
`messages.properties` (hoặc `messages_vi.properties`) của ứng dụng:

```properties
somi.jpa.error.query.not-found=Không tìm thấy câu truy vấn {0}
somi.jpa.error.query.max-rows=Câu truy vấn {0} trả về quá {1} dòng
somi.jpa.error.convert.value=Không chuyển được giá trị kiểu {0} sang {1}
```

```java
String message = messageSource.getMessage(exception, LocaleContextHolder.getLocale());
```

Danh sách mã lỗi nằm trong enum `com.somi.jpa.enums.ErrorMessage`.

## 12. Log và an toàn dữ liệu

Bật log SQL:

```yaml
logging:
  level:
    com.somi.jpa.executors: DEBUG
```

- Mặc định log chỉ in SQL và số lượng tham số, **không in giá trị**.
- `somi.jpa.log-parameter-values: true` in SQL đã điền giá trị tham số, tiện khi debug ở máy cá nhân.
  **Không bật trên production**: tham số thường chứa dữ liệu cá nhân (email, số điện thoại, CCCD...).
- SQL in ra log chỉ để đọc, không phải câu lệnh thực sự gửi tới database.

Khuyến nghị cho production:

- Đặt `somi.jpa.query-timeout` để một query chậm không giữ kết nối mãi mãi.
- Đặt `somi.jpa.max-rows` để tránh vô tình nạp hàng triệu dòng vào bộ nhớ.
- Đặt tên file (namespace) theo nghiệp vụ, tránh tên chung chung như `common.xml` dễ trùng với file trong các jar khác.

## 13. Viết test

Repository hoạt động như bean bình thường trong `@SpringBootTest`:

```java
@SpringBootTest
@Transactional
class PersonQueriesTest {

    @Autowired
    PersonQueries personQueries;

    @Test
    void findsActivePeople() {
        assertThat(personQueries.findActive("ACTIVE")).isNotEmpty();
    }
}
```

Nên chạy test trên cùng loại database với production (ví dụ PostgreSQL qua Testcontainers), vì SQL native
phụ thuộc vào cú pháp và kiểu dữ liệu của từng database.

## 14. Câu hỏi thường gặp

**Ứng dụng báo `Query id user.FIND_BY_ID used by ... not found` khi khởi động.**
Không có query `FIND_BY_ID` trong namespace `user`. Kiểm tra chính tả `id`, giá trị `@QueryNamespace`
có khớp tên file (hoặc thuộc tính `namespace` của file) không, vị trí file và `somi.jpa.query-locations`
(khai báo thuộc tính này sẽ thay thế vị trí mặc định).

**Ứng dụng báo `Query id user.FIND_BY_ID duplicated in file ...`.**
Cùng một file khai báo `id` hai lần, hoặc hai file có cùng namespace (cùng tên file ở hai thư mục / hai jar,
hoặc cùng giá trị `namespace`). Đổi `id`, hoặc đặt `namespace` khác cho một trong hai file.

**Ứng dụng báo `Cannot derive a namespace for query file ...`.**
Không lấy được tên file của resource. Khai báo `<queries namespace="...">` trong file đó.

**Thuộc tính trong DTO luôn `null`.**
Kiểm tra tên cột (dùng alias), DTO có setter public và constructor không tham số.

**`IncorrectResultSizeDataAccessException`.**
Method trả về một giá trị nhưng query ra nhiều dòng. Đổi kiểu trả về thành `List<T>` hoặc sửa SQL.

**`TransactionRequiredException` hoặc `LazyInitializationException`.**
Ứng dụng có nhiều `PlatformTransactionManager` nên thư viện không tự mở transaction, hoặc entity được dùng
ngoài transaction. Gọi repository từ method `@Transactional`.

**Sau `UPDATE` vẫn đọc ra dữ liệu cũ của entity.**
Thêm `clearAutomatically = true` vào `@QueryId` của câu `UPDATE`.

**Tham số enum không khớp dữ liệu.**
Enum được truyền theo `name()`. Nếu database lưu dạng số, hãy truyền `status.ordinal()` với tham số kiểu `int`.
