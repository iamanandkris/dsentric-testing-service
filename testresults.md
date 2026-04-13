# Load Test Results

Date: 2026-04-13

Environment:
- Local Docker Compose stack
- Spring Boot app container + PostgreSQL 15 container
- Load test driver: `POST /load-test`
- Concurrency: `10`
- Requests per endpoint: `100`
- Benchmark style: single fresh run after the latest persistence-path changes

Notes:
- These numbers are local environment measurements, not synthetic framework-benchmark results.
- The latest run reflects the current write path, where validated internal payloads are persisted and sanitized views are returned from the API.
- Load-test payloads were refreshed so product `POST` and `PATCH` routes now benchmark with contract-valid bodies across Scala, Java, and Kotlin.

## Summary

| Endpoint | Method | Dsentric features exercised | Avg latency ms | Total duration ms | Req/s | Success rate |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| `/users-scala` | `GET` | nested contract sanitize on read, annotation validation on stored records | 19.00 | 211 | 473.93 | 100/100 |
| `/users-java` | `GET` | nested JVM contract sanitize on read, annotation validation on stored records | 16.27 | 189 | 529.10 | 100/100 |
| `/users-kotlin` | `GET` | nested JVM contract sanitize on read, annotation validation on stored records | 11.40 | 133 | 751.88 | 100/100 |
| `/users-scala` | `POST` | create validation, nested contracts, persisted internal payload + sanitized response | 9.93 | 112 | 892.86 | 100/100 |
| `/users-java` | `POST` | create validation, nested JVM contracts, persisted internal payload + sanitized response | 7.33 | 89 | 1123.60 | 100/100 |
| `/users-kotlin` | `POST` | create validation, nested JVM contracts, persisted internal payload + sanitized response | 6.44 | 76 | 1315.79 | 100/100 |
| `/users-scala/1` | `PATCH` | `validatePatch`, nested patch validation, sanitized response | 3.99 | 54 | 1851.85 | 100/100 |
| `/users-java/1` | `PATCH` | `validatePatch`, nested JVM patch validation, sanitized response | 6.84 | 83 | 1204.82 | 100/100 |
| `/users-kotlin/1` | `PATCH` | `validatePatch`, nested JVM patch validation, sanitized response | 5.98 | 72 | 1388.89 | 100/100 |
| `/products-scala` | `POST` | nested contracts, inventory validator, persisted internal payload + sanitized response | 10.08 | 114 | 877.19 | 100/100 |
| `/products-java` | `POST` | nested JVM contracts, inventory validator, persisted internal payload + sanitized response | 5.79 | 69 | 1449.28 | 100/100 |
| `/products-kotlin` | `POST` | nested JVM contracts, inventory validator, persisted internal payload + sanitized response | 6.33 | 80 | 1250.00 | 100/100 |
| `/products-scala/1` | `PATCH` | `validatePatch`, inventory validator, nested patch validation, sanitized response | 7.89 | 93 | 1075.27 | 100/100 |
| `/products-java/1` | `PATCH` | `validatePatch`, inventory validator, nested JVM patch validation, sanitized response | 5.79 | 76 | 1315.79 | 100/100 |
| `/products-kotlin/1` | `PATCH` | `validatePatch`, inventory validator, nested JVM patch validation, sanitized response | 7.94 | 95 | 1052.63 | 100/100 |
| `/orders-scala` | `POST` | nested contracts, totals validator, masked/internal response sanitization, persisted internal payload | 10.36 | 114 | 877.19 | 100/100 |
| `/orders-java` | `POST` | nested JVM contracts, totals validator, masked/internal response sanitization, persisted internal payload | 5.07 | 73 | 1369.86 | 100/100 |
| `/orders-kotlin` | `POST` | nested JVM contracts, totals validator, masked/internal response sanitization, persisted internal payload | 4.73 | 58 | 1724.14 | 100/100 |
| `/orders-scala/1` | `PATCH` | `validatePatch`, totals validation, sanitized response | 5.37 | 63 | 1587.30 | 100/100 |
| `/orders-java/1` | `PATCH` | `validatePatch`, totals validation, nested JVM patch validation, sanitized response | 6.17 | 79 | 1265.82 | 100/100 |
| `/orders-kotlin/1` | `PATCH` | `validatePatch`, totals validation, nested JVM patch validation, sanitized response | 6.37 | 74 | 1351.35 | 100/100 |

## Findings

1. All benchmarked endpoints completed with `100%` success in the latest run.
2. The latest benchmark reflects the new split between persistence and presentation:
   validated internal payloads are stored in PostgreSQL `JSONB`, while API responses are still sanitized by `dsentric`.
3. Kotlin was fastest on `GET /users`, `POST /users`, and `POST /orders` in this run.
4. Java led the product benchmarks in this run, especially `POST /products-java` and `PATCH /products-java/1`.
5. Scala remained the slowest path overall in this local setup, which still suggests the benchmark is dominated by the shared Spring/Jackson/JDBC/PostgreSQL stack rather than purely by language syntax.
6. The corrected product load-test bodies removed the previous benchmark artifact where some product `POST` and `PATCH` requests failed due to under-specified nested payloads.

## Persistence Split

This benchmark run was executed after the service changed from:

- persist sanitized payloads

to:

- persist validated internal payloads
- sanitize only at the API response boundary

That means the current service now demonstrates two separate `dsentric` views:

- internal persisted view
- public returned view

Examples verified after the run:

- `users.data.internalNotes` is stored in PostgreSQL but omitted from API responses
- `products.data.internalCost` is stored in PostgreSQL but omitted from API responses
- `orders.data.paymentInfo.last4` is stored as the raw value in PostgreSQL but returned as `****`
- `orders.data.paymentInfo.gatewayReference` is stored in PostgreSQL but omitted from API responses

## Coverage Notes

The load-tested paths above cover the main CRUD hot paths and a meaningful subset of the service's `dsentric` usage. Additional `dsentric` features are still exercised functionally by the API script rather than load-tested:

- JSON schema generation via `/contracts/{entity}/schema`
- partial/draft validation via `/contracts/{entity}/draft/{language}`
- contract-level validator failures for inventory and totals
- `@immutable` rejection checks
- `@reserved`, `@internal`, and `@masked` behavior
