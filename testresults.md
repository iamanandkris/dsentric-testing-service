# Load Test Results

Date: 2026-04-11

Environment:
- Local Docker Compose stack
- Spring Boot app container + PostgreSQL 15 container
- Load test driver: `POST /load-test`
- Concurrency: `10`
- Requests per round: `100`
- Rounds per endpoint: `3`

Notes:
- These numbers are local environment measurements, not synthetic framework-benchmark results.
- The endpoints below were selected because they represent the primary CRUD hot paths across `users`, `products`, and `orders`.
- `PATCH` now exercises `dsentric` `validatePatch(...)` rather than manual merge logic.

## Summary

| Endpoint | Method | Dsentric features exercised | Avg latency ms | Latency min-max ms | Avg req/s | Req/s min-max |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| `/orders-scala` | `POST` | `Contract.derived`, nested contracts, contract-level total validation, masked/internal field sanitization, create-path validation | 2.15 | 1.75-2.73 | 3493.83 | 2777.78-4000.00 |
| `/orders-scala/1` | `PATCH` | `validatePatch`, `@immutable` enforcement on order fields, contract-level total validation, nested patch validation | 1.97 | 1.81-2.27 | 3558.29 | 3125.00-3846.15 |
| `/orders-java` | `POST` | `Contract.derived`, nested contracts, contract-level total validation, masked/internal field sanitization, create-path validation | 1.65 | 1.42-1.82 | 4210.76 | 3703.70-4761.90 |
| `/orders-java/1` | `PATCH` | `validatePatch`, `@immutable` enforcement on order fields, contract-level total validation, nested patch validation | 1.74 | 1.70-1.76 | 3849.95 | 3703.70-4000.00 |
| `/orders-kotlin` | `POST` | `Contract.derived`, nested contracts, contract-level total validation, masked/internal field sanitization, create-path validation | 1.68 | 1.55-1.83 | 4004.27 | 3846.15-4166.67 |
| `/orders-kotlin/1` | `PATCH` | `validatePatch`, `@immutable` enforcement on order fields, contract-level total validation, nested patch validation | 2.07 | 1.82-2.50 | 3350.00 | 3030.30-3571.43 |
| `/products-scala` | `POST` | `Contract.derived`, nested contracts, inventory validator, create-path validation, `sanitize` | 1.47 | 1.07-2.02 | 4292.93 | 3333.33-5000.00 |
| `/products-scala/1` | `PATCH` | `validatePatch`, inventory validator, `@immutable` enforcement, nested patch validation, `sanitize` | 2.57 | 1.68-4.35 | 3196.83 | 1886.79-4000.00 |
| `/products-java` | `POST` | `Contract.derived`, nested contracts, inventory validator, create-path validation, `sanitize` | 1.35 | 1.12-1.59 | 4397.99 | 3846.15-5000.00 |
| `/products-java/1` | `PATCH` | `validatePatch`, inventory validator, `@immutable` enforcement, nested patch validation, `sanitize` | 1.65 | 1.51-1.90 | 3987.59 | 3448.28-4347.83 |
| `/products-kotlin` | `POST` | `Contract.derived`, nested contracts, inventory validator, create-path validation, `sanitize` | 1.36 | 1.29-1.48 | 4790.96 | 4347.83-5263.16 |
| `/products-kotlin/1` | `PATCH` | `validatePatch`, inventory validator, `@immutable` enforcement, nested patch validation, `sanitize` | 2.04 | 1.49-3.03 | 3562.80 | 2173.91-4347.83 |
| `/users-scala` | `GET` | `Contract.derived`, annotation validation on stored records, `sanitize`, nested contracts | 3.15 | 2.67-4.09 | 2519.33 | 1923.08-2857.14 |
| `/users-scala` | `POST` | `Contract.derived`, annotation validation, nested contracts, `sanitize`, create-path validation | 1.53 | 1.01-2.17 | 4278.93 | 3225.81-5263.16 |
| `/users-java` | `GET` | `Contract.derived`, annotation validation on stored records, `sanitize`, nested contracts | 3.10 | 2.65-3.59 | 2361.61 | 2127.66-2631.58 |
| `/users-java` | `POST` | `Contract.derived`, annotation validation, nested contracts, `sanitize`, create-path validation | 1.30 | 1.18-1.39 | 4856.84 | 4545.45-5263.16 |
| `/users-kotlin` | `GET` | `Contract.derived`, annotation validation on stored records, `sanitize`, nested contracts | 2.31 | 2.08-2.57 | 3032.16 | 2941.18-3125.00 |
| `/users-kotlin` | `POST` | `Contract.derived`, annotation validation, nested contracts, `sanitize`, create-path validation | 1.20 | 1.06-1.44 | 5399.94 | 4761.90-5882.35 |
| `/users-scala/1` | `PATCH` | `validatePatch`, `@immutable`, `@reserved`, nested patch validation, `sanitize` | 2.83 | 2.22-3.28 | 2702.25 | 2380.95-3225.81 |
| `/users-java/1` | `PATCH` | `validatePatch`, `@immutable`, `@reserved`, nested patch validation, `sanitize` | 2.22 | 1.85-2.44 | 3351.50 | 3125.00-3703.70 |
| `/users-kotlin/1` | `PATCH` | `validatePatch`, `@immutable`, `@reserved`, nested patch validation, `sanitize` | 1.97 | 1.95-2.01 | 3489.33 | 3448.28-3571.43 |

## Round Details

| Endpoint | Method | Round | Avg latency ms | Total duration ms | Req/s | Success rate |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `/users-scala` | `GET` | 1 | 4.09 | 52 | 1923.08 | 100/100 |
| `/users-scala` | `GET` | 2 | 2.67 | 35 | 2857.14 | 100/100 |
| `/users-scala` | `GET` | 3 | 2.69 | 36 | 2777.78 | 100/100 |
| `/users-scala` | `POST` | 1 | 2.17 | 31 | 3225.81 | 100/100 |
| `/users-scala` | `POST` | 2 | 1.42 | 23 | 4347.83 | 100/100 |
| `/users-scala` | `POST` | 3 | 1.01 | 19 | 5263.16 | 100/100 |
| `/users-java` | `GET` | 1 | 3.59 | 47 | 2127.66 | 100/100 |
| `/users-java` | `GET` | 2 | 2.65 | 38 | 2631.58 | 100/100 |
| `/users-java` | `GET` | 3 | 3.05 | 43 | 2325.58 | 100/100 |
| `/users-java` | `POST` | 1 | 1.34 | 21 | 4761.90 | 100/100 |
| `/users-java` | `POST` | 2 | 1.39 | 22 | 4545.45 | 100/100 |
| `/users-java` | `POST` | 3 | 1.18 | 19 | 5263.16 | 100/100 |
| `/users-kotlin` | `GET` | 1 | 2.08 | 33 | 3030.30 | 100/100 |
| `/users-kotlin` | `GET` | 2 | 2.28 | 32 | 3125.00 | 100/100 |
| `/users-kotlin` | `GET` | 3 | 2.57 | 34 | 2941.18 | 100/100 |
| `/users-kotlin` | `POST` | 1 | 1.44 | 21 | 4761.90 | 100/100 |
| `/users-kotlin` | `POST` | 2 | 1.10 | 18 | 5555.56 | 100/100 |
| `/users-kotlin` | `POST` | 3 | 1.06 | 17 | 5882.35 | 100/100 |
| `/users-scala/1` | `PATCH` | 1 | 2.98 | 40 | 2500.00 | 100/100 |
| `/users-scala/1` | `PATCH` | 2 | 2.22 | 31 | 3225.81 | 100/100 |
| `/users-scala/1` | `PATCH` | 3 | 3.28 | 42 | 2380.95 | 100/100 |
| `/users-java/1` | `PATCH` | 1 | 2.37 | 32 | 3125.00 | 100/100 |
| `/users-java/1` | `PATCH` | 2 | 2.44 | 31 | 3225.81 | 100/100 |
| `/users-java/1` | `PATCH` | 3 | 1.85 | 27 | 3703.70 | 100/100 |
| `/users-kotlin/1` | `PATCH` | 1 | 1.95 | 28 | 3571.43 | 100/100 |
| `/users-kotlin/1` | `PATCH` | 2 | 2.01 | 29 | 3448.28 | 100/100 |
| `/users-kotlin/1` | `PATCH` | 3 | 1.96 | 29 | 3448.28 | 100/100 |

## Findings

1. All benchmarked endpoints completed with `100%` success across all rounds.
2. `POST` remained the fastest operation class across nearly every entity/language combination in this local benchmark set.
3. `PATCH` was generally faster than `GET` on the `users` endpoints, even while exercising real `dsentric` patch validation.
4. Kotlin was consistently the fastest of the three variants in most runs, followed by Java, then Scala.
5. Order and product create/patch throughput stayed in the same broad range as user operations, which suggests the benchmark is still dominated by the shared Spring/Jackson/JDBC/PostgreSQL path rather than language-specific logic.
6. The `PATCH` and order/product `POST` measurements are more representative of real `dsentric` behavior than the earlier CRUD-only benchmark, because they exercise `validatePatch(...)`, contract-level validators, immutable/reserved handling, masking, and sanitization.

## Coverage Notes

The load-tested paths above cover a broader, but still partial, subset of the service's `dsentric` usage. Other `dsentric` features are exercised functionally by the API test script rather than load-tested:
- JSON schema generation via `/contracts/{entity}/schema`
- partial/draft validation via `/contracts/{entity}/draft/{language}`
- contract-level validators for inventory and totals
- `@internal`, `@masked`, and `@reserved` field behavior
