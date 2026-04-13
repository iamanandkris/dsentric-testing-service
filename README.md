# Dsentric Testing Service

This repository is a polyglot test setup for `dsentric`. The service exposes the same e-commerce style API through Scala, Java, and Kotlin routes so the contract model, validation behavior, patch behavior, sanitization, schema generation, draft validation, and load-test behavior can be compared across all three JVM entry points.

The main goal is not the CRUD service itself. The main goal is to exercise `dsentric` features through realistic HTTP payloads and verify how those payloads are converted into `dsentric` contracts in:

- Scala via `Contract.derived` in [Contracts.scala](/Users/anand.krishnan/example/dsentric-testing-service/src/main/scala/com/example/dsentrictestservice/contracts/Contracts.scala)
- Java via `JvmContract.ofRecord(...)` in [JavaContractModels.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/service/javaimpl/JavaContractModels.java)
- Kotlin via `JvmContract.ofPrimary(...)` in [KotlinContractModels.kt](/Users/anand.krishnan/example/dsentric-testing-service/src/main/kotlin/com/example/dsentrictestservice/service/kotlinimpl/KotlinContractModels.kt)

## What This Service Tests

`dsentric` features exercised by this repo:

| Feature | Where it is exercised |
| --- | --- |
| Contract derivation | `UserPayload`, `ProductPayload`, `OrderPayload` and nested types in Scala/Java/Kotlin |
| Nested decoding | `address`, `preferences`, `price`, `inventory`, `items`, `totals`, `paymentInfo` |
| Field validation annotations | `@email`, `@nonEmpty`, `@min`, `@positive` |
| Patch validation | `PATCH` routes call `validatePatch(...)` |
| Sanitization | All create/read/update/patch responses pass through `sanitize(...)` |
| `@reserved` | `preferences.internalSegment`, `internalStatus` |
| `@internal` | `internalNotes`, `internalCost`, `gatewayReference` |
| `@masked` | `paymentInfo.last4` |
| `@immutable` | `registeredAt`, `createdAt`, `orderNumber` |
| Contract-level validators | inventory and order totals validators |
| Draft validation | `POST /contracts/{entity}/draft/{language}` |
| JSON schema generation | `GET /contracts/{entity}/schema` |
| Load testing | `POST /load-test` against GET, POST, PATCH routes |

## Route Families

Language-specific CRUD routes:

| Entity | Scala | Java | Kotlin |
| --- | --- | --- | --- |
| Users | `/users-scala` | `/users-java` | `/users-kotlin` |
| Products | `/products-scala` | `/products-java` | `/products-kotlin` |
| Orders | `/orders-scala` | `/orders-java` | `/orders-kotlin` |

Each family supports:

| Method | Route |
| --- | --- |
| `POST` | `/{entity}-{language}` |
| `GET` | `/{entity}-{language}/{id}` |
| `PUT` | `/{entity}-{language}/{id}` |
| `PATCH` | `/{entity}-{language}/{id}` |
| `DELETE` | `/{entity}-{language}/{id}` |
| `GET` | `/{entity}-{language}` |

Contract utility routes:

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/contracts/users/schema?language={scala\|java\|kotlin}` | User contract schema |
| `GET` | `/contracts/products/schema?language={scala\|java\|kotlin}` | Product contract schema |
| `GET` | `/contracts/orders/schema?language={scala\|java\|kotlin}` | Order contract schema |
| `POST` | `/contracts/users/draft/{language}` | Partial user validation |
| `POST` | `/contracts/products/draft/{language}` | Partial product validation |
| `POST` | `/contracts/orders/draft/{language}` | Partial order validation |

Operational routes:

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/health` | Health check |
| `GET` | `/metrics/stats` | Aggregate metrics |
| `POST` | `/load-test` | Concurrent benchmark runner |
| `POST` | `/seed` | Re-seed test data |

## How Payloads Become Contracts

The same JSON payload enters the service as `Map<String, Object>` from Spring MVC.

The contract conversion path is:

1. Controller receives JSON body as `Map<String, Object>`.
2. The route delegates into the shared CRUD engine in [GenericCrudEngine.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/core/GenericCrudEngine.java).
3. The engine chooses the language-specific contract facade.
4. Scala routes validate through [ContractFacade.scala](/Users/anand.krishnan/example/dsentric-testing-service/src/main/scala/com/example/dsentrictestservice/service/ContractFacade.scala).
5. Java and Kotlin routes validate through [JvmContractSupport.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/service/jvm/JvmContractSupport.java).
6. `dsentric` validates the raw payload, returns contract violations if invalid, and the service applies sanitization at the API response boundary.

In Scala:

- nested payloads are decoded using `RawDecoder` helpers for contract-derived case classes
- `validate(...)`, `validatePatch(...)`, `validatePartial(...)`, `sanitize(...)`, and `jsonSchema` are called on `Contract[T]`

In Java:

- nested payloads are decoded into records through `JvmContract.ofRecord(...)`
- `validate(...)`, `validatePatch(...)`, `validatePartial(...)`, `sanitize(...)`, and `jsonSchema()` are called on `JvmContract<T>`

In Kotlin:

- nested payloads are decoded into data classes through `JvmContract.ofPrimary(...)`
- the same `JvmContract<T>` flow is used as Java

## Internal vs Public View

This service is intentionally configured as a test harness to show two different views of the same payload:

1. the validated internal payload that gets persisted in PostgreSQL `JSONB`
2. the sanitized public payload returned by the API

For this repo:

- validation happens before persistence
- persistence keeps internal and sensitive fields that passed validation
- API responses are sanitized on the way out

That means the repository can store fields such as:

- `internalNotes`
- `internalCost`
- `paymentInfo.gatewayReference`
- raw `paymentInfo.last4`

while the API still returns the `dsentric` public contract view:

- `@internal` fields removed
- `@masked` fields transformed

### Verified example: seeded user

Stored in PostgreSQL:

```json
{
  "age": 18,
  "name": "User 0",
  "email": "user0@example.com",
  "address": {
    "city": "City 0",
    "street": "0 Main St",
    "zipCode": "10000"
  },
  "preferences": {
    "newsletter": true,
    "notifications": true
  },
  "registeredAt": "2026-04-13T09:20:49.769468508Z",
  "internalNotes": "seed-note-0"
}
```

Returned by `GET /users-scala/1`:

```json
{
  "data": {
    "name": "User 0",
    "email": "user0@example.com",
    "age": 18,
    "registeredAt": "2026-04-13T09:20:49.769468508Z",
    "preferences": {
      "newsletter": true,
      "notifications": true
    },
    "address": {
      "city": "City 0",
      "street": "0 Main St",
      "zipCode": "10000"
    },
    "id": 1
  },
  "executionTimeMs": 3,
  "language": "scala"
}
```

Effect:

- `internalNotes` is persisted
- `internalNotes` is hidden from the API response because it is `@internal`

### Verified example: seeded product

Stored in PostgreSQL:

```json
{
  "sku": "SKU-0000",
  "name": "Product 0",
  "tags": ["seeded", "featured"],
  "price": {
    "amount": 1.0,
    "currency": "USD"
  },
  "category": "electronics",
  "createdAt": "2026-04-13T09:20:49.927097508Z",
  "inventory": {
    "reserved": 126,
    "available": 130
  },
  "description": "Generated product 0",
  "internalCost": 0.5
}
```

Returned by `GET /products-scala/1`:

```json
{
  "data": {
    "inventory": {
      "reserved": 126,
      "available": 130
    },
    "price": {
      "amount": 1.0,
      "currency": "USD"
    },
    "createdAt": "2026-04-13T09:20:49.927097508Z",
    "category": "electronics",
    "name": "Product 0",
    "sku": "SKU-0000",
    "description": "Generated product 0",
    "tags": ["seeded", "featured"],
    "id": 1
  },
  "executionTimeMs": 1,
  "language": "scala"
}
```

Effect:

- `internalCost` is persisted
- `internalCost` is hidden from the API response because it is `@internal`

### Verified example: seeded order

Stored in PostgreSQL:

```json
{
  "items": [
    { "sku": "SKU-0759", "quantity": 1, "productId": 759, "unitPrice": 87.0 },
    { "sku": "SKU-0087", "quantity": 2, "productId": 87, "unitPrice": 81.0 },
    { "sku": "SKU-0395", "quantity": 2, "productId": 395, "unitPrice": 111.0 }
  ],
  "status": "cancelled",
  "totals": {
    "tax": 47.1,
    "total": 518.1,
    "shipping": 0.0,
    "subtotal": 471.0
  },
  "userId": 464,
  "placedAt": "2026-04-13T09:20:50.153115883Z",
  "orderNumber": "ORD-00000",
  "paymentInfo": {
    "last4": "0000",
    "method": "credit_card",
    "gatewayReference": "gw-seed-0"
  },
  "shippingAddress": {
    "city": "Ship City",
    "street": "0 Market St",
    "zipCode": "90210"
  }
}
```

Returned by `GET /orders-scala/1`:

```json
{
  "data": {
    "orderNumber": "ORD-00000",
    "items": [
      { "sku": "SKU-0759", "quantity": 1, "productId": 759, "unitPrice": 87.0 },
      { "sku": "SKU-0087", "quantity": 2, "productId": 87, "unitPrice": 81.0 },
      { "sku": "SKU-0395", "quantity": 2, "productId": 395, "unitPrice": 111.0 }
    ],
    "placedAt": "2026-04-13T09:20:50.153115883Z",
    "status": "cancelled",
    "shippingAddress": {
      "city": "Ship City",
      "street": "0 Market St",
      "zipCode": "90210"
    },
    "userId": 464,
    "paymentInfo": {
      "last4": "****",
      "method": "credit_card"
    },
    "totals": {
      "tax": 47.1,
      "total": 518.1,
      "shipping": 0.0,
      "subtotal": 471.0
    },
    "id": 1
  },
  "executionTimeMs": 1,
  "language": "scala"
}
```

Effect:

- raw `paymentInfo.last4` is persisted as `"0000"`
- `paymentInfo.gatewayReference` is persisted
- the API response masks `last4` to `****`
- the API response removes `gatewayReference`

## Contract Shapes

The three languages model the same business objects.

### User contract

Fields:

| Field | Dsentric behavior |
| --- | --- |
| `email` | `@email` |
| `name` | `@nonEmpty` |
| `age` | `@min(0)` |
| `address` | nested contract |
| `preferences` | nested contract |
| `registeredAt` | `@immutable` |
| `internalNotes` | `@internal` |
| `preferences.internalSegment` | `@reserved` |

Create payload:

```json
{
  "email": "api-test-scala@example.com",
  "name": "API Test scala",
  "age": 31,
  "address": {
    "street": "1 Test St",
    "city": "London",
    "zipCode": "EC1A1AA"
  },
  "preferences": {
    "newsletter": true,
    "notifications": false
  }
}
```

PATCH payload:

```json
{
  "age": 33,
  "preferences": {
    "newsletter": true
  }
}
```

What `dsentric` does:

- validates `email` format
- rejects blank `name`
- rejects negative `age`
- decodes `address` and `preferences` as nested contracts
- rejects `preferences.internalSegment` because it is reserved
- strips `internalNotes` from sanitized output
- rejects mutation of `registeredAt` once it exists

### Product contract

Fields:

| Field | Dsentric behavior |
| --- | --- |
| `sku` | required |
| `name` | `@nonEmpty` |
| `description` | optional |
| `category` | optional |
| `price` | nested contract |
| `inventory` | nested contract |
| `tags` | optional list |
| `createdAt` | `@immutable` |
| `internalCost` | `@internal` |

Create payload:

```json
{
  "sku": "SKU-SCALA-001",
  "name": "Product scala",
  "description": "Product created by API test",
  "category": "test",
  "price": {
    "amount": 19.99,
    "currency": "USD"
  },
  "inventory": {
    "available": 100,
    "reserved": 5
  },
  "tags": ["api-test", "scala"]
}
```

PATCH payload:

```json
{
  "price": {
    "amount": 24.99
  },
  "inventory": {
    "available": 95
  }
}
```

Load-test PATCH payload:

```json
{
  "price": {
    "amount": 24.99,
    "currency": "USD"
  },
  "inventory": {
    "available": 60,
    "reserved": 5
  }
}
```

What `dsentric` does:

- validates non-empty `name`
- decodes nested `price` and `inventory`
- strips `internalCost` from sanitized output
- rejects mutation of `createdAt`
- runs the inventory contract validator

Inventory validator rule:

```text
inventory.reserved must not exceed inventory.available
```

### Order contract

Fields:

| Field | Dsentric behavior |
| --- | --- |
| `userId` | required |
| `orderNumber` | `@immutable` |
| `status` | required |
| `items` | `@nonEmpty` list of nested contracts |
| `totals` | nested contract |
| `shippingAddress` | nested contract |
| `paymentInfo.last4` | `@masked("****")` |
| `paymentInfo.gatewayReference` | `@internal` |
| `internalStatus` | `@reserved` |

Create payload:

```json
{
  "userId": 1,
  "orderNumber": "ORD-SCALA-001",
  "status": "pending",
  "items": [
    {
      "productId": 1,
      "sku": "SKU-SCALA-001",
      "quantity": 2,
      "unitPrice": 24.99
    }
  ],
  "totals": {
    "subtotal": 49.98,
    "tax": 5.0,
    "shipping": 10.0,
    "total": 64.98
  },
  "shippingAddress": {
    "street": "3 Order St",
    "city": "Bristol",
    "zipCode": "BS11AA"
  },
  "paymentInfo": {
    "method": "credit_card",
    "last4": "1234"
  }
}
```

PATCH payload:

```json
{
  "status": "completed"
}
```

What `dsentric` does:

- validates `items` is non-empty
- validates each `OrderItem.quantity` with `@positive`
- decodes `items`, `totals`, `shippingAddress`, and `paymentInfo` as nested contracts
- masks `paymentInfo.last4` as `****` in sanitized output
- strips `paymentInfo.gatewayReference` from sanitized output
- rejects `internalStatus`
- rejects mutation of `orderNumber`
- runs the order totals validator

Order totals validator rules:

```text
totals.subtotal must equal the sum of line items
totals.total must equal subtotal + tax + shipping
```

## API Payloads By Operation

The payload shapes below apply to all three language route families unless the route name changes.

### Users

| Method | Routes | Payload |
| --- | --- | --- |
| `POST` | `/users-scala`, `/users-java`, `/users-kotlin` | user create payload above |
| `PUT` | `/users-{language}/{id}` | full user payload with updated fields |
| `PATCH` | `/users-{language}/{id}` | partial user patch payload above |
| `GET` | `/users-{language}`, `/users-{language}/{id}` | no body |
| `DELETE` | `/users-{language}/{id}` | no body |

### Products

| Method | Routes | Payload |
| --- | --- | --- |
| `POST` | `/products-scala`, `/products-java`, `/products-kotlin` | product create payload above |
| `PUT` | `/products-{language}/{id}` | full product payload |
| `PATCH` | `/products-{language}/{id}` | product patch payload above |
| `GET` | `/products-{language}`, `/products-{language}/{id}` | no body |
| `DELETE` | `/products-{language}/{id}` | no body |

### Orders

| Method | Routes | Payload |
| --- | --- | --- |
| `POST` | `/orders-scala`, `/orders-java`, `/orders-kotlin` | order create payload above |
| `PUT` | `/orders-{language}/{id}` | full order payload |
| `PATCH` | `/orders-{language}/{id}` | order patch payload above |
| `GET` | `/orders-{language}`, `/orders-{language}/{id}` | no body |
| `DELETE` | `/orders-{language}/{id}` | no body |

## Feature-Probe APIs

These routes exist primarily to exercise `dsentric` behavior directly.

### Schema generation

Examples:

```http
GET /contracts/users/schema?language=scala
GET /contracts/products/schema?language=java
GET /contracts/orders/schema?language=kotlin
```

The schema output is used to verify metadata such as:

- `x-immutable`
- `x-internal`
- `x-reserved`
- `x-masked`

### Draft validation

Example payloads:

```json
{"email":"draft-scala@example.com"}
```

```json
{"sku":"DRAFT-001"}
```

```json
{"status":"pending"}
```

These are sent to:

- `POST /contracts/users/draft/{language}`
- `POST /contracts/products/draft/{language}`
- `POST /contracts/orders/draft/{language}`

They test `validatePartial(...)`, allowing incomplete payloads that would fail full contract validation.

## Validation Scenarios Covered By The Script

The functional script is [scripts/run-api-tests.sh](/Users/anand.krishnan/example/dsentric-testing-service/scripts/run-api-tests.sh).

It verifies:

| Scenario | Probe payload | Expected outcome |
| --- | --- | --- |
| Immutable product field | `{"createdAt":"2027-01-01T00:00:00Z"}` | rejected |
| Reserved user field | `{"preferences":{"internalSegment":"secret"}}` | rejected |
| Inventory validator | `{"inventory":{"available":2,"reserved":5}}` | rejected |
| Masked/internal payment info | `{"paymentInfo":{"method":"credit_card","last4":"1234","gatewayReference":"gw-123"}}` | `last4` masked, `gatewayReference` stripped |
| Bad order totals | mismatched `subtotal` and `total` | rejected |

## Load-Test API

The service includes a built-in benchmark endpoint:

```http
POST /load-test
```

Request shape:

```json
{
  "endpoint": "/users-scala",
  "method": "GET",
  "concurrency": 10,
  "requests": 100,
  "body": null
}
```

Response shape:

```json
{
  "totalRequests": 100,
  "successfulRequests": 100,
  "failedRequests": 0,
  "totalDurationMs": 166,
  "avgLatencyMs": 14.96,
  "requestsPerSecond": 602.41
}
```

### Endpoints covered in load tests

| Benchmark type | Endpoints covered | Payload |
| --- | --- | --- |
| `GET` list | `/users-scala`, `/users-java`, `/users-kotlin` | none |
| `POST` create user | `/users-scala`, `/users-java`, `/users-kotlin` | user load payload |
| `POST` create product | `/products-scala`, `/products-java`, `/products-kotlin` | product load payload |
| `POST` create order | `/orders-scala`, `/orders-java`, `/orders-kotlin` | order load payload |
| `PATCH` user | `/users-scala/1`, `/users-java/1`, `/users-kotlin/1` | user patch load payload |
| `PATCH` product | `/products-scala/1`, `/products-java/1`, `/products-kotlin/1` | full nested product patch payload |
| `PATCH` order | `/orders-scala/1`, `/orders-java/1`, `/orders-kotlin/1` | `{"status":"completed"}` |

User load payload:

```json
{
  "email": "loadtest-users-scala-user@example.com",
  "name": "Load Test User",
  "age": 34,
  "preferences": {
    "newsletter": false,
    "notifications": true
  }
}
```

Product load payload:

```json
{
  "sku": "loadtest-products-scala-sku",
  "name": "Load Test Product",
  "description": "Benchmark product payload",
  "category": "benchmark",
  "price": {
    "amount": 19.99,
    "currency": "USD"
  },
  "inventory": {
    "available": 50,
    "reserved": 5
  },
  "tags": ["load-test", "products-scala"]
}
```

Order load payload:

```json
{
  "userId": 1,
  "orderNumber": "LOADTEST-orders-scala-ORDER",
  "status": "pending",
  "items": [
    {
      "productId": 1,
      "sku": "SKU-0000",
      "quantity": 2,
      "unitPrice": 20.0
    }
  ],
  "totals": {
    "subtotal": 40.0,
    "tax": 4.0,
    "shipping": 1.0,
    "total": 45.0
  },
  "paymentInfo": {
    "method": "credit_card",
    "last4": "1234"
  }
}
```

Product PATCH load payload:

```json
{
  "price": {
    "amount": 24.99,
    "currency": "USD"
  },
  "inventory": {
    "available": 60,
    "reserved": 5
  }
}
```

## Where To Look In Code

If the goal is to understand `dsentric` behavior rather than the HTTP service, start here:

- Scala contracts: [Contracts.scala](/Users/anand.krishnan/example/dsentric-testing-service/src/main/scala/com/example/dsentrictestservice/contracts/Contracts.scala)
- Scala contract flow: [ContractFacade.scala](/Users/anand.krishnan/example/dsentric-testing-service/src/main/scala/com/example/dsentrictestservice/service/ContractFacade.scala)
- Java JVM contracts: [JavaContractModels.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/service/javaimpl/JavaContractModels.java)
- Kotlin JVM contracts: [KotlinContractModels.kt](/Users/anand.krishnan/example/dsentric-testing-service/src/main/kotlin/com/example/dsentrictestservice/service/kotlinimpl/KotlinContractModels.kt)
- Shared JVM support: [JvmContractSupport.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/service/jvm/JvmContractSupport.java)
- CRUD dispatch: [GenericCrudEngine.java](/Users/anand.krishnan/example/dsentric-testing-service/src/main/java/com/example/dsentrictestservice/core/GenericCrudEngine.java)
- Benchmark and feature script: [run-api-tests.sh](/Users/anand.krishnan/example/dsentric-testing-service/scripts/run-api-tests.sh)
- Current benchmark results: [testresults.md](/Users/anand.krishnan/example/dsentric-testing-service/testresults.md)
