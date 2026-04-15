# Testing Java Microservices (CardDemo Migration)

## Overview
The `java-services/` directory contains 10 Spring Boot microservices migrated from the original COBOL CardDemo mainframe application. Each service is a standalone Gradle project with its own `build.gradle`, application class, and embedded H2 database.

## Prerequisites
- Java 17 (verify with `java -version`)
- No external databases needed — all services use H2 in-memory databases in dev mode

## Service Ports
| Service | Port | Has REST Endpoints |
|---------|------|--------------------|
| auth-service | 8081 | Yes |
| account-service | 8082 | Yes |
| card-service | 8083 | Yes |
| transaction-service | 8084 | Yes |
| payment-service | 8085 | Yes |
| user-service | 8086 | Yes |
| menu-service | 8087 | Yes |
| report-service | 8088 | Yes |
| batch-service | 8089 | Yes (jobs) |
| common-lib | N/A | No (shared library) |

## Building
```bash
# Build a single service
cd java-services/<service-name>
./gradlew build

# Build all services (no root multi-project build exists)
for dir in java-services/*/; do
  (cd "$dir" && ./gradlew build)
done
```

## Running a Service
```bash
cd java-services/<service-name>
./gradlew bootRun
```

## Common Startup Issues

### `Table "X" not found (this database is empty)`
This means `data.sql` is executing before JPA creates the schema. Fix: ensure `application.properties` includes:
```properties
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.jpa.hibernate.ddl-auto=create
```
The `defer-datasource-initialization` property is critical — without it, Spring Boot runs `data.sql` before Hibernate creates tables.

### JWT Secret Missing (auth-service)
If auth-service fails with a missing `JWT_SECRET` env var, either set the env var or hardcode a dev-only default in `application.properties` for `jwt.secret`. The secret must be at least 32 bytes for HMAC-SHA384.

## Seed Data & Test Credentials
Seed data is loaded from `src/main/resources/data.sql` in each service. Check each service's `data.sql` for available test account IDs, user IDs, and card numbers.

### auth-service
- Test users are defined in `data.sql` with plaintext dev-only passwords
- User types: 'U' (regular), 'A' (admin)

### account-service
- 5 seed accounts with IDs starting at 10000000001
- Card cross-references linking card numbers to accounts

### payment-service
- 3 seed accounts with varying balances (positive, positive, zero/inactive)

## Testing Approach
These are REST API services with no UI — test via `curl` or similar HTTP clients. No browser recording needed.

### Key Endpoint Patterns
```bash
# auth-service
curl -X POST http://localhost:8081/api/auth/login -H 'Content-Type: application/json' -d '{"userId":"<user_id>","password":"<password>"}'
curl -X POST http://localhost:8081/api/auth/validate -H 'Content-Type: application/json' -d '{"token":"<jwt-token>"}'

# account-service
curl http://localhost:8082/api/accounts/<account_id>
curl http://localhost:8082/api/accounts
curl http://localhost:8082/api/accounts/card/<card_number>

# payment-service
curl http://localhost:8085/api/payments/balance/<account_id>
curl -X POST http://localhost:8085/api/payments/bill -H 'Content-Type: application/json' -d '{"accountId":<account_id>}'
```

### Bill Payment Flow (Critical Business Logic)
The payment-service mirrors COBOL program COBIL00C.cbl:
1. Check balance > 0 (rejects with error if zero)
2. Create transaction record with type code '02'
3. Zero out the account balance
4. Return transaction ID and confirmation

Test by: get balance -> pay -> verify balance=0 -> pay again (should reject)

## Devin Secrets Needed
None required for local dev testing — all services use H2 in-memory databases with no authentication.
