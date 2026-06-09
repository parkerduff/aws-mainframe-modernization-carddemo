# CardDemo — Java 21 / Spring Boot Migration

A modernization of the COBOL/CICS **CardDemo** mainframe application (under `../app/`) to a
Java 21 Spring Boot REST service. The online CICS transactions become REST controllers, the
VSAM files become relational tables (PostgreSQL, seeded from the original ASCII data), and the
batch programs/JCL become Spring Batch jobs.

## Overview

| Mainframe concept | Java / Spring equivalent |
|---|---|
| CICS online programs (`CO*`) | `@RestController` + `@Service` |
| Batch programs (`CB*`) + JCL | Spring Batch `Job` / `Step` |
| VSAM KSDS files + copybooks | JPA `@Entity` + Spring Data repositories |
| COMMAREA state passing | JWT carrying `userId`, `userType`, `fromProgram` |
| BMS maps / 3270 screens | JSON request/response DTOs (records) |
| Signed packed/zoned decimals | `BigDecimal` (scale 2) — never floating point |
| `COBDATFT.asm` date routine | `java.time` |

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 13+ (for running; tests use in-memory H2)

## Build & Test

```bash
cd carddemo-java
mvn clean test        # runs unit + batch + context tests against H2
mvn -DskipTests package
```

## Run

Create a database and point the app at it (defaults shown):

```bash
createdb carddemo
export DB_URL=jdbc:postgresql://localhost:5432/carddemo
export DB_USERNAME=carddemo
export DB_PASSWORD=carddemo
export CARDDEMO_JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run
```

Flyway applies `V1__initial_schema.sql` (tables) and `V2__seed_data.sql` (data parsed from
`../app/data/ASCII/`) on startup. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

### Seeded logins

Users come from `app/jcl/DUSRSECJ.jcl`. The original plaintext password `PASSWORD` is stored
as a BCrypt hash. Admins: `ADMIN001`–`ADMIN005`; users: `USER0001`–`USER0005`.

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userId":"ADMIN001","password":"PASSWORD"}'
# -> { "token": "...", "userType": "A", "nextProgram": "COADM01C" }
```

Pass the token on subsequent calls: `-H "Authorization: Bearer <token>"`.

## API Endpoints

| Method & path | Description | Replaces |
|---|---|---|
| `POST /api/auth/login` | Sign on, returns JWT + menu routing | `COSGN00C` (CC00) |
| `GET /api/menu` | Menu options for the caller's user type | `COMEN01C` / `COADM01C` |
| `GET /api/accounts/{id}` | Account by id | `COACTVWC` |
| `GET /api/accounts?cardNum=` | Account via card → XREF → account | `COACTVWC` |
| `PUT /api/accounts/{id}` | Update account fields | `COACTUPC` |
| `GET /api/transactions?cardNum=&page=&size=` | List transactions (paged) | `COTRN00C` |
| `GET /api/transactions/{id}` | Transaction detail | `COTRN01C` |
| `POST /api/transactions` | Add a transaction | `COTRN02C` |
| `GET /api/cards?acctId=` | Cards for an account | `COCRDLIC` |
| `GET /api/cards/search?...` | Search cards | `COCRDSLC` |
| `PUT /api/cards/{cardNum}` | Update card | `COCRDUPC` |
| `GET/POST/PUT/DELETE /api/users` | User CRUD (**admin only**) | `COUSR00C`–`COUSR03C` |
| `POST /api/billpay` | Bill payment | `COBIL00C` |
| `GET /api/reports/transactions` | Transaction report | `CORPT00C` |
| `GET /api/reports/accounts` | Account report | `CORPT00C` |

## Security

Spring Security 6, stateless JWT (`com.aws.carddemo.security`). `/api/auth/login` and the
Swagger/OpenAPI endpoints are public; `/api/users/**` requires `ROLE_ADMIN`; everything else
requires authentication. The JWT payload mirrors the COMMAREA general info (user id, user
type, originating program).

## Batch Jobs (`com.aws.carddemo.batch`)

Jobs are not run on web startup (`spring.batch.job.enabled=false`); launch them explicitly.

- **`postTransactionJob`** — replaces `CBTRN02C.cbl` + `POSTTRAN.jcl`. Reads the fixed-width
  daily-transaction file (`classpath:batch/dailytran-sample.txt` by default, override with
  `carddemo.batch.daily-tran-input`), validates each record against the card XREF and account
  (invalid card / missing account / overlimit / post-expiration), posts valid transactions and
  updates account + category balances, and writes rejects to `target/batch/dailyrejs.txt`
  (override with `carddemo.batch.reject-output`).
- **`interestCalculationJob`** — replaces `CBACT04C.cbl`. Computes monthly interest
  (`balance * rate / 1200`) per account and updates balances.

## Project Layout

```
src/main/java/com/aws/carddemo/
  controller/   REST controllers (CICS transactions)
  service/      business logic (COBOL online programs)
  batch/        Spring Batch jobs (COBOL batch + JCL)
  entity/       JPA entities (VSAM records / copybooks)
  repository/   Spring Data repositories (file access)
  dto/          request/response records (BMS maps)
  security/     JWT provider + filter
  config/       security + OpenAPI configuration
  domain/       enums (e.g. UserType from 88-levels)
  util/         COBOL conversions, zoned-decimal codec
src/main/resources/
  application.yml
  db/migration/ Flyway V1 schema + V2 seed data
  batch/        sample daily-transaction file
```

## COBOL → Java Mapping

| COBOL program | Java class |
|---|---|
| `COSGN00C` | `service/AuthService` |
| `COMEN01C`, `COADM01C` | `service/MenuService` |
| `COACTVWC`, `COACTUPC` | `service/AccountService` |
| `COTRN00C`, `COTRN01C`, `COTRN02C` | `service/TransactionService` |
| `COCRDLIC`, `COCRDSLC`, `COCRDUPC` | `service/CardService` |
| `COUSR00C`–`COUSR03C` | `service/UserService` |
| `COBIL00C` | `service/BillPayService` |
| `CORPT00C` | `service/ReportService` |
| `CBTRN02C` + `POSTTRAN.jcl` | `batch/PostTransactionJobConfig` |
| `CBACT04C` | `batch/InterestCalculationJobConfig` |

Copybook → entity: `CVACT01Y`→`AccountEntity`, `CVACT02Y`→`CardEntity`,
`CVACT03Y`→`CardXrefEntity`, `CVCUS01Y`→`CustomerEntity`, `CVTRA05Y`→`TransactionEntity`,
`CVTRA01Y`→`TranCatBalEntity`, `CSUSR01Y`→`SecUserEntity`.
