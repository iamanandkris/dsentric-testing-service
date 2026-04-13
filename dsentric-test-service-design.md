# TestServiceCreation Design Document

## 1. App Overview

TestServiceCreation is a comprehensive polyglot testing harness for the dsentric library (https://github.com/iamanandkris/dsentric). It exposes a REST API implemented across three JVM languages—Scala, Java, and Kotlin—to validate dsentric's contract validation, delta computation, sanitization, nested object manipulation, and performance characteristics.

**Core Purpose:** Stress-test all dsentric capabilities through real-world CRUD operations on an e-commerce domain (Users, Products, Orders) with data persisted as JSONB in PostgreSQL.

**Who Uses It:** Developers testing dsentric locally via curl/Postman and automated bash scripts.

**Core Interaction Loop:**
1. Application starts with seed data (hundreds of users, products, orders)
2. User invokes language-specific endpoints (`/users-scala`, `/products-java`, `/orders-kotlin`)
3. Each request:
   - Validates input using dsentric contracts at HTTP boundary
   - Routes to language-specific service implementation (Scala/Java/Kotlin)
   - Performs JDBC operations on PostgreSQL JSONB columns
   - Measures execution time within service method
   - Logs performance metrics to `metrics` table
   - Returns result with embedded timing and language identifier
4. Aggregate stats endpoint (`/metrics/stats`) computes p50/p99/avg across all logged operations
5. Load test endpoint (`/load-test`) triggers concurrent requests for benchmarking
6. Automated bash script executes comprehensive test suite against all endpoints

**Key Testing Dimensions:**
- **Contract validation**: Required/optional fields, type safety, nested structures
- **Delta operations**: PATCH endpoints using dsentric's native delta format
- **Sanitization**: Stripping unexpected fields per contract definitions
- **Language ergonomics**: Compare Scala/Java/Kotlin API usage patterns
- **Performance**: Per-request timing + aggregate statistics + load testing
- **JSONB integration**: Round-trip serialization between dsentric contracts and PostgreSQL

## 2. Tech Stack

### Core Dependencies

```scala
// build.sbt
scalaVersion := "3.4.2"

// Dsentric (publishLocal dependency)
libraryDependencies += "com.github.iamanandkris" %% "dsentric" % "0.1.0-SNAPSHOT"

// Spring Boot (polyglot HTTP layer + DI container)
libraryDependencies ++= Seq(
  "org.springframework.boot" % "spring-boot-starter-web" % "3.2.0",
  "org.springframework.boot" % "spring-boot-starter-jdbc" % "3.2.0"
)

// PostgreSQL Driver
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.1"

// Jackson for JSON serialization
libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.16.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.16.0"
)

// Kotlin support
libraryDependencies += "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.9.21"

// SBT plugins
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5") // Fat JAR
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16") // Docker
```

### Why These Choices

**Scala 3.4.2**: Matches dsentric's development version; TASTy compatibility ensures macro expansion works correctly. Avoid 3.5.x (potential TASTy version mismatches).

**Spring Boot 3.2.0**: 
- Mature polyglot support (Scala/Java/Kotlin coexist naturally)
- Simplest HTTP layer for Java/Kotlin services
- Built-in connection pooling (HikariCP)
- **Must use Java 17+ (we use Java 21)**

**PostgreSQL 42.7.1**: Latest stable driver with JSONB operator support. Earlier versions (<42.2.x) have issues with `?` operator in PreparedStatements (affects GIN index queries if added later).

**Jackson 2.16.0**: 
- Spring Boot's default serializer
- Scala module provides case class support
- **Critical**: Must register `jackson-module-scala` in Spring context or Scala collections serialize incorrectly

**Kotlin 1.9.21**: Latest stable compatible with Spring Boot 3.2.x. Kotlin 2.x requires Spring Boot 3.3+.

### Compatibility Caveats

1. **Dsentric publishLocal**: This design assumes dsentric artifacts are in `~/.ivy2/local`. If dsentric changes package structure, update group ID in `build.sbt`.

2. **Scala 3 + Spring Boot**: Spring's reflection-based DI has issues with Scala 3 `object` singletons. Services MUST be classes (not objects) annotated with `@Component`.

3. **Jackson + Dsentric**: No built-in converter exists. We implement custom `JsonNode ↔ dsentric DObject` converters (see Section 7).

4. **JDBC Transaction Management**: Spring's `@Transactional` works in Java/Kotlin but Scala services need manual `DataSource.getConnection()` handling.

5. **SBT + Spring Boot**: Use `sbt-assembly` with `MergeStrategy` for `module-info.class` conflicts:
```scala
assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x => (assemblyMergeStrategy in assembly).value(x)
}
```

### Must-Follow Patterns

- **Service Layer**: All services implement a common trait/interface defining CRUD methods + timing
- **Connection Pooling**: Each language has separate `DataSource` bean (`scalaDataSource`, `javaDataSource`, `kotlinDataSource`)
- **Error Handling**: All services throw `ContractValidationException` (custom) on dsentric validation failure → Spring `@ExceptionHandler` converts to HTTP 400
- **Logging**: Use SLF4J (Spring Boot's default) — avoid `println` in production code paths

## 3. Cross-Cutting Concerns

### Authentication & Authorization
**None.** This is a single-user local test service with all endpoints publicly accessible. No JWT, API keys, or session management.

### Multi-Tenancy
**None.** Single-tenant design. All data belongs to the test operator.

### Per-Request Data Flow

```
HTTP Request (JSON body)
    ↓
Spring @RestController (language-specific)
    ↓
Jackson deserializes to JsonNode
    ↓
JsonNode → Dsentric DObject (via custom converter)
    ↓
Dsentric Contract Validation (throws if invalid)
    ↓
Service Layer (Scala/Java/Kotlin implementation)
    ├─ Start timing
    ├─ JDBC operation (read/write JSONB)
    ├─ Dsentric operations (delta, sanitize, etc.)
    ├─ Stop timing
    └─ Log metrics to DB
    ↓
Dsentric DObject → JsonNode (via custom converter)
    ↓
Wrap in ResponseWrapper(data, executionTimeMs, language)
    ↓
Jackson serializes to JSON
    ↓
HTTP Response
```

### Encryption
**At-Rest Only.** PostgreSQL database files are unencrypted (acceptable for local testing). In production dsentric usage, entire JSONB columns would be encrypted; this test assumes plaintext storage to simplify debugging.

**In-Transit:** No HTTPS (local testing on `http://localhost:8080`).

### Authorization
Not applicable (no auth).

### Idempotency
- **POST** (create): Not idempotent; repeated calls create duplicate records
- **GET**: Idempotent
- **PUT/PATCH**: Idempotent if request body is identical
- **DELETE**: Idempotent (deleting non-existent ID returns 404 but doesn't error)

### Concurrency
Spring Boot uses Tomcat's thread pool (default 200 threads). Each request gets isolated JDBC connection from HikariCP pool (default 10 connections per DataSource). No distributed locking—concurrent updates to same record may overwrite each other (acceptable for test workload).

### Observability
- **Metrics Table**: Captures timestamp, endpoint, language, execution time, success/failure per request
- **Aggregate Endpoint**: `/metrics/stats?endpoint=/users-scala&language=scala` computes avg/p50/p99
- **Logs**: SLF4J to console (Docker logs), includes request IDs via Spring Boot's default MDC

## 4. Data Model

### Database: PostgreSQL 15+
Use JSONB type for all domain data; PostgreSQL 15 offers improved JSONB performance.

### Schema DDL

```sql
-- Run this manually before starting the application
CREATE DATABASE testservice;

\c testservice;

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    data JSONB NOT NULL
);

-- Products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    data JSONB NOT NULL
);

-- Orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    data JSONB NOT NULL
);

-- Metrics table (performance logging)
CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    endpoint TEXT NOT NULL,
    language TEXT NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL
);

-- Optional: Create indexes on metrics for aggregate queries
CREATE INDEX idx_metrics_endpoint_language ON metrics(endpoint, language);
CREATE INDEX idx_metrics_timestamp ON metrics(timestamp);
```

### Entity Descriptions

#### Users
Represents customers in the e-commerce system.

**JSONB Structure (managed by dsentric contract):**
```json
{
  "email": "user@example.com",
  "name": "John Doe",
  "age": 30,
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "zipCode": "12345"
  },
  "preferences": {
    "newsletter": true,
    "notifications": false
  },
  "registeredAt": "2024-01-15T10:30:00Z"
}
```

**Required Fields (per dsentric contract):** `email`, `name`  
**Optional Fields:** `age`, `address`, `preferences`, `registeredAt`

#### Products
Items available for purchase.

**JSONB Structure:**
```json
{
  "sku": "WIDGET-001",
  "name": "Super Widget",
  "description": "A fantastic widget",
  "category": "electronics",
  "price": {
    "amount": 99.99,
    "currency": "USD"
  },
  "inventory": {
    "available": 150,
    "reserved": 10
  },
  "tags": ["featured", "new-arrival"],
  "createdAt": "2024-01-10T08:00:00Z"
}
```

**Required Fields:** `sku`, `name`, `price.amount`, `price.currency`  
**Optional Fields:** `description`, `category`, `inventory`, `tags`, `createdAt`

#### Orders
Purchase transactions linking users to products.

**JSONB Structure:**
```json
{
  "userId": 42,
  "orderNumber": "ORD-2024-001",
  "status": "pending",
  "items": [
    {
      "productId": 10,
      "sku": "WIDGET-001",
      "quantity": 2,
      "unitPrice": 99.99
    }
  ],
  "totals": {
    "subtotal": 199.98,
    "tax": 15.00,
    "shipping": 10.00,
    "total": 224.98
  },
  "shippingAddress": {
    "street": "456 Oak Ave",
    "city": "Portland",
    "zipCode": "97201"
  },
  "paymentInfo": {
    "method": "credit_card",
    "last4": "1234"
  },
  "placedAt": "2024-01-20T14:22:00Z"
}
```

**Required Fields:** `userId`, `orderNumber`, `status`, `items` (non-empty array), `totals.total`  
**Optional Fields:** `shippingAddress`, `paymentInfo`, `placedAt`

**Database Constraint:** `orders.user_id` foreign key ensures referential integrity; `orders.data.userId` JSONB field matches `user_id` column (validated by application, not DB constraint).

**Items Array:** Each item references `product_id` (foreign key enforced via separate junction table would be typical, but for simplicity we store in JSONB; application validates product existence).

#### Metrics
Performance telemetry for all API operations.

**Columns:**
- `id`: Primary key
- `timestamp`: When the request was processed
- `endpoint`: API path (e.g., `/users-scala`, `/orders-kotlin/42`)
- `language`: `scala`, `java`, or `kotlin`
- `execution_time_ms`: Time spent in service layer (excludes HTTP serialization)
- `success`: `true` if operation completed without exception, `false` if error

**Retention Policy:** No automatic cleanup (manual `DELETE FROM metrics WHERE timestamp < NOW() - INTERVAL '30 days'` if needed).

### Migration Strategy
**None.** This is a test service. Operator runs the DDL script manually before first run. Schema changes require dropping and recreating tables (destructive). No Flyway/Liquibase.

### Seed Data Generation
On application startup, `DataSeeder` component:
1. Checks if `users` table is empty
2. If empty, inserts:
   - 500 users (realistic email/name/address distributions)
   - 1000 products (varied SKUs, prices $1-$999, categories: electronics, clothing, home, books)
   - 2000 orders (random user/product associations, realistic order totals)
3. Uses batch JDBC inserts for performance (`INSERT INTO users (data) VALUES (?), (?), ...`)

**Seed Data Characteristics:**
- Users: 20% have full address, 80% partial; 50% opt into newsletter
- Products: 10% high-value (>$500), inventory ranges 0-500
- Orders: 60% "completed", 30% "pending", 10% "cancelled"; 1-5 items per order

## 5. Service Layer Design

### Architecture
Three services (User, Product, Order), each implemented in three languages (Scala, Java, Kotlin). Total: 9 service classes. Each service is a Spring `@Component` with language-suffixed bean name.

### Shared Interface (Scala trait, Java/Kotlin equivalent)

```scala
trait CrudService[T] {
  def create(entity: T): ServiceResult[T]
  def read(id: Long): ServiceResult[T]
  def update(id: Long, entity: T): ServiceResult[T]
  def partialUpdate(id: Long, delta: DObject): ServiceResult[T]
  def delete(id: Long): ServiceResult[Unit]
  def list(): ServiceResult[List[T]]
}

case class ServiceResult[T](
  data: T,
  executionTimeMs: Long,
  success: Boolean
)
```

### Service Implementations

#### UserService (Scala, Java, Kotlin)

**Responsibility:** Manage user CRUD operations with dsentric `UserContract` validation and sanitization.

**Methods:**
- `create(user: DObject): ServiceResult[DObject]`
  - Validate against `UserContract` (required: email, name)
  - Sanitize (remove unexpected fields)
  - Insert into `users` table: `INSERT INTO users (data) VALUES (?) RETURNING id`
  - Measure execution time
  - Log to `metrics` table
  - Return created user with generated ID

- `read(id: Long): ServiceResult[DObject]`
  - Query: `SELECT data FROM users WHERE id = ?`
  - Deserialize JSONB to dsentric `DObject`
  - Return 404 if not found

- `update(id: Long, user: DObject): ServiceResult[DObject]`
  - Validate full contract
  - Sanitize
  - Update: `UPDATE users SET data = ? WHERE id = ?`
  - Return updated entity

- `partialUpdate(id: Long, delta: DObject): ServiceResult[DObject]`
  - Fetch existing user
  - Apply dsentric delta merge: `existingUser.applyDelta(delta)`
  - Validate merged result
  - Sanitize
  - Persist merged entity

- `delete(id: Long): ServiceResult[Unit]`
  - `DELETE FROM users WHERE id = ?`
  - Return success (idempotent—no error if ID doesn't exist)

- `list(): ServiceResult[List[DObject]]`
  - `SELECT id, data FROM users LIMIT 100` (prevent unbounded result sets)

**Dependencies:**
- `DataSource` (language-specific: `scalaDataSource`, `javaDataSource`, `kotlinDataSource`)
- `MetricsLogger` (writes to `metrics` table)
- `DsentricJacksonConverter` (see Section 7)

**Dsentric Contract (Scala example):**
```scala
object UserContract extends Contract {
  val email = \[String]("email")
  val name = \[String]("name")
  val age = \?[Int]("age")
  val address = new \\("address") {
    val street = \?[String]("street")
    val city = \?[String]("city")
    val zipCode = \?[String]("zipCode")
  }
  val preferences = new \\("preferences") {
    val newsletter = \?[Boolean]("newsletter")
    val notifications = \?[Boolean]("notifications")
  }
  val registeredAt = \?[String]("registeredAt") // ISO8601 timestamp
}
```

#### ProductService (Scala, Java, Kotlin)

**Responsibility:** Manage product catalog with nested pricing and inventory structures.

**Methods:** Same CRUD operations as `UserService`, adapted for `ProductContract`.

**Key Dsentric Feature Usage:**
- **Nested objects:** `price { amount, currency }`, `inventory { available, reserved }`
- **Arrays:** `tags: List[String]`
- **Sanitization:** Remove any unexpected fields (e.g., if client sends `{"internalCost": 50}`, strip it)

**Dsentric Contract (Scala):**
```scala
object ProductContract extends Contract {
  val sku = \[String]("sku")
  val name = \[String]("name")
  val description = \?[String]("description")
  val category = \?[String]("category")
  val price = new \\ {
    val amount = \[Double]("amount")
    val currency = \[String]("currency")
  }
  val inventory = new \\?("inventory") {
    val available = \[Int]("available")
    val reserved = \[Int]("reserved")
  }
  val tags = \?[List[String]]("tags")
  val createdAt = \?[String]("createdAt")
}
```

#### OrderService (Scala, Java, Kotlin)

**Responsibility:** Manage orders with foreign key validation and complex nested structures.

**Methods:** CRUD operations with additional validation logic.

**Special Logic in `create`:**
1. Validate `OrderContract`
2. **Verify `userId` exists:** `SELECT 1 FROM users WHERE id = ?`
3. **Verify all `productId` in `items` array exist:** `SELECT id FROM products WHERE id IN (...)`
4. If validation passes, insert into `orders` table with `user_id` foreign key column set to `data.userId`

**PATCH Behavior:**
- Delta may modify `status` field (e.g., `{"status": "completed"}`)
- Delta may add/remove items from `items` array (tests dsentric's array delta capabilities)

**Dsentric Contract (Scala):**
```scala
object OrderContract extends Contract {
  val userId = \[Long]("userId")
  val orderNumber = \[String]("orderNumber")
  val status = \[String]("status") // pending, completed, cancelled
  val items = \[List[DObject]]("items") // each item has productId, sku, quantity, unitPrice
  val totals = new \\ {
    val subtotal = \?[Double]("subtotal")
    val tax = \?[Double]("tax")
    val shipping = \?[Double]("shipping")
    val total = \[Double]("total")
  }
  val shippingAddress = new \\?("shippingAddress") {
    val street = \[String]("street")
    val city = \[String]("city")
    val zipCode = \[String]("zipCode")
  }
  val paymentInfo = new \\?("paymentInfo") {
    val method = \[String]("method")
    val last4 = \[String]("last4")
  }
  val placedAt = \?[String]("placedAt")
}
```

### MetricsLogger (Shared Component)

**Responsibility:** Persist performance metrics to `metrics` table.

**Method:**
```scala
def log(endpoint: String, language: String, executionTimeMs: Long, success: Boolean): Unit
```

**Implementation:** Simple JDBC insert, executed asynchronously (Spring's `@Async`) to avoid blocking service responses.

### Layer Dependency Graph

```
HTTP Controllers (Spring Boot)
    ├─ ScalaUserController → UserServiceScala
    ├─ JavaUserController → UserServiceJava
    ├─ KotlinUserController → UserServiceKotlin
    ├─ ScalaProductController → ProductServiceScala
    ├─ ...
    └─ MetricsController → MetricsService
         ↓
Service Layer (Scala/Java/Kotlin implementations)
    ↓
JDBC (via DataSource beans)
    ↓
PostgreSQL
```

**No cross-language service calls.** Each language's service is self-contained.

## 6. API Surface

All endpoints return JSON. Default port: **8080**.

### Response Wrapper (All Successful Responses)

```json
{
  "data": { /* entity or list */ },
  "executionTimeMs": 42,
  "language": "scala" | "java" | "kotlin"
}
```

### Error Response (HTTP 400, 404, 500)

```json
{
  "error": "Validation failed",
  "details": ["field 'email' is required", "field 'age' must be positive"],
  "language": "scala"
}
```

### User Endpoints

#### Create User (Scala)
- **POST** `/users-scala`
- **Request Body:**
  ```json
  {
    "email": "alice@example.com",
    "name": "Alice",
    "age": 28,
    "address": {
      "street": "789 Elm St",
      "city": "Austin",
      "zipCode": "73301"
    }
  }
  ```
- **Response:** 201 Created
  ```json
  {
    "data": {
      "id": 501,
      "email": "alice@example.com",
      "name": "Alice",
      "age": 28,
      "address": { "street": "789 Elm St", "city": "Austin", "zipCode": "73301" }
    },
    "executionTimeMs": 15,
    "language": "scala"
  }
  ```
- **Auth:** None
- **Validation:** Dsentric `UserContract` applied; missing `email` or `name` → 400

#### Get User (Java)
- **GET** `/users-java/{id}`
- **Response:** 200 OK (same wrapper structure with `"language": "java"`)
- **404** if user not found

#### Update User (Kotlin)
- **PUT** `/users-kotlin/{id}`
- **Request Body:** Full user object (all required fields must be present)
- **Response:** 200 OK

#### Partial Update User (Scala)
- **PATCH** `/users-scala/{id}`
- **Request Body (Dsentric Delta Format):**
  ```json
  {
    "age": 29,
    "preferences": {
      "newsletter": false
    }
  }
  ```
- **Behavior:** Merges delta into existing user, validates result, persists
- **Response:** 200 OK with merged entity

#### Delete User (Java)
- **DELETE** `/users-java/{id}`
- **Response:** 204 No Content

#### List Users (Kotlin)
- **GET** `/users-kotlin`
- **Response:** 200 OK
  ```json
  {
    "data": [
      { "id": 1, "email": "user1@example.com", "name": "User One" },
      ...
    ],
    "executionTimeMs": 8,
    "language": "kotlin"
  }
  ```

### Product Endpoints

Same CRUD structure as Users, with language suffixes (`-scala`, `-java`, `-kotlin`):
- **POST** `/products-{lang}`
- **GET** `/products-{lang}/{id}`
- **PUT** `/products-{lang}/{id}`
- **PATCH** `/products-{lang}/{id}`
- **DELETE** `/products-{lang}/{id}`
- **GET** `/products-{lang}`

**Example PATCH Request (tests nested delta):**
```json
{
  "price": {
    "amount": 89.99
  },
  "inventory": {
    "available": 200
  }
}
```

### Order Endpoints

Same CRUD structure:
- **POST** `/orders-{lang}`
- **GET** `/orders-{lang}/{id}`
- **PUT** `/orders-{lang}/{id}`
- **PATCH** `/orders-{lang}/{id}`
- **DELETE** `/orders-{lang}/{id}`
- **GET** `/orders-{lang}`

**Special Validation in POST:**
- Verifies `userId` exists in `users` table (returns 400 if not)
- Verifies all `productId` in `items` array exist in `products` table

**Example PATCH (change order status):**
```json
{
  "status": "completed"
}
```

### Metrics Endpoints

#### Get Aggregate Stats
- **GET** `/metrics/stats?endpoint=/users-scala&language=scala`
- **Query Params:**
  - `endpoint` (optional): Filter by endpoint path
  - `language` (optional): Filter by language
- **Response:** 200 OK
  ```json
  {
    "count": 1500,
    "avgMs": 12.4,
    "p50Ms": 10,
    "p95Ms": 18,
    "p99Ms": 25,
    "minMs": 5,
    "maxMs": 45
  }
  ```
- **Calculation:** Queries `metrics` table, computes percentiles using `percentile_cont()`

#### Trigger Load Test
- **POST** `/load-test`
- **Request Body:**
  ```json
  {
    "endpoint": "/users-scala",
    "method": "GET",
    "concurrency": 10,
    "requests": 100,
    "body": null
  }
  ```
- **Behavior:**
  - Spawns `concurrency` threads
  - Each thread executes `requests / concurrency` HTTP calls to `endpoint`
  - Aggregates results (total time, success rate, avg latency)
- **Response:** 200 OK (after all requests complete)
  ```json
  {
    "totalRequests": 100,
    "successfulRequests": 98,
    "failedRequests": 2,
    "totalDurationMs": 1250,
    "avgLatencyMs": 12.5,
    "requestsPerSecond": 80
  }
  ```
- **Implementation:** Uses Java's `ExecutorService` with fixed thread pool

### Health Check
- **GET** `/health`
- **Response:** 200 OK
  ```json
  {
    "status": "UP",
    "database": "connected"
  }
  ```
- **Used by:** Test script to wait for service readiness before running tests

### Seed Data Endpoint (Optional)
- **POST** `/seed`
- **Behavior:** Deletes all data, re-runs seed data generation
- **Response:** 200 OK
  ```json
  {
    "usersCreated": 500,
    "productsCreated": 1000,
    "ordersCreated": 2000
  }
  ```

## 7. Serialization Strategy

### Library: Jackson 2.16.0

**Codec Pattern:**
All HTTP request/response bodies use Jackson's `ObjectMapper`. Spring Boot auto-configures Jackson with default settings.

### Custom Converters: Dsentric ↔ Jackson

Dsentric's internal representation (`DObject`, `DArray`, etc.) must convert to/from Jackson's `JsonNode` for HTTP I/O.

#### DsentricJacksonConverter (Scala Implementation)

```scala
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.{ObjectNode, ArrayNode, JsonNodeFactory}
import dsentric._ // Adjust import based on dsentric's actual package

object DsentricJacksonConverter {
  private val mapper = new ObjectMapper()
  private val nodeFactory = JsonNodeFactory.instance

  def toJsonNode(dobj: DObject): JsonNode = {
    val objNode = nodeFactory.objectNode()
    dobj.value.foreach { case (key, value) =>
      objNode.set(key, convertValue(value))
    }
    objNode
  }

  def toDObject(node: JsonNode): DObject = {
    require(node.isObject, "JsonNode must be an object")
    val fields = node.fields()
    val map = scala.collection.mutable.Map[String, Any]()
    while (fields.hasNext) {
      val entry = fields.next()
      map(entry.getKey) = convertNode(entry.getValue)
    }
    DObject(map.toMap) // Adjust based on dsentric's actual DObject constructor
  }

  private def convertValue(value: Any): JsonNode = value match {
    case s: String => nodeFactory.textNode(s)
    case i: Int => nodeFactory.numberNode(i)
    case l: Long => nodeFactory.numberNode(l)
    case d: Double => nodeFactory.numberNode(d)
    case b: Boolean => nodeFactory.booleanNode(b)
    case obj: DObject => toJsonNode(obj)
    case arr: DArray => 
      val arrayNode = nodeFactory.arrayNode()
      arr.value.foreach(v => arrayNode.add(convertValue(v)))
      arrayNode
    case null => nodeFactory.nullNode()
    case _ => throw new IllegalArgumentException(s"Unsupported type: ${value.getClass}")
  }

  private def convertNode(node: JsonNode): Any = {
    if (node.isObject) toDObject(node)
    else if (node.isArray) {
      val arr = node.elements()
      val list = scala.collection.mutable.ListBuffer[Any]()
      while (arr.hasNext) list += convertNode(arr.next())
      DArray(list.toList) // Adjust based on dsentric's DArray constructor
    }
    else if (node.isTextual) node.asText()
    else if (node.isInt) node.asInt()
    else if (node.isLong) node.asLong()
    else if (node.isDouble) node.asDouble()
    else if (node.isBoolean) node.asBoolean()
    else if (node.isNull) null
    else throw new IllegalArgumentException(s"Unsupported JsonNode type: ${node.getNodeType}")
  }
}
```

**Java/Kotlin Equivalents:** Similar converters implemented in respective languages, following the same logic.

### Registration with Spring Boot

```scala
@Configuration
class JacksonConfig {
  @Bean
  def objectMapper(): ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule) // jackson-module-scala
    mapper
  }
}
```

### Contract Validation Integration

In each controller's request handler:

```scala
@PostMapping(Array("/users-scala"))
def createUser(@RequestBody jsonNode: JsonNode): ResponseEntity[ResponseWrapper] = {
  try {
    val dobj = DsentricJacksonConverter.toDObject(jsonNode)
    
    // Validate against contract
    UserContract.validate(dobj) match {
      case Success(_) =>
        val sanitized = UserContract.sanitize(dobj)
        val result = user