# CardDemo Fraud Detection — Java Migration

Java (Spring Boot) migration of the CardDemo **fraud detection** flow from the COBOL
authorization module. It reproduces the behavior of the online "mark authorization fraud"
transaction (BMS PF5) and the DB2 fraud-tracking upsert, without any mainframe runtime
(no CICS/IMS/DB2/MQ) — persistence is JPA against a relational database (H2 in-memory by
default), keeping the design provider-agnostic.

- **Base package:** `com.aws.carddemo.authorization.fraud`
- **Reference COBOL:** `../cbl/COPAUS1C.cbl`, `../cbl/COPAUS2C.cbl`, `../cpy/CIPAUDTY.cpy`,
  `../cbl/COPAUA0C.cbl`, and the AUTHFRDS DDL in [`../README.md`](../README.md).

## COBOL → Java mapping

| COBOL artifact | Java artifact | Notes |
|---|---|---|
| `COPAUS1C` `MARK-AUTH-FRAUD` (PF5 handler) | `service.FraudServiceImpl.markAuthFraud` | Loads the pending auth detail, toggles the fraud flag, sets the report date, upserts AUTHFRDS, updates the detail. |
| `COPAUS1C` BMS PF5 screen | `web.FraudController` `POST /api/authorizations/{cardNum}/{authTs}/fraud-toggle` | REST replacement for the 3270 key. |
| `COPAUS1C` `TAKE-SYNCPOINT` / `ROLL-BACK` (CICS SYNCPOINT) | `@Transactional` on `markAuthFraud` | A single transaction boundary replaces the CICS two-phase commit; any `RuntimeException` rolls back both the AUTHFRDS write and the detail update. |
| `COPAUS1C` `UPDATE-AUTH-DETAILS` messages | `FraudActionResult` `"AUTH MARKED FRAUD..."` / `"AUTH FRAUD REMOVED..."` | Success flag + message DTO. |
| `COPAUS2C` `INSERT INTO CARDDEMO.AUTHFRDS` | `AuthFraudRepository.save` (insert path) | `AuthFraud.fromPendingAuthDetail(...)` maps every AUTHFRDS column. |
| `COPAUS2C` `SQLCODE -803` → `FRAUD-UPDATE` (`UPDATE`) | `AuthFraudRepository.findById(...)` present → update `AUTH_FRAUD` + `FRAUD_RPT_DATE` | Duplicate-key branch = JPA upsert-by-lookup. |
| `CIPAUDTY` `PA-AUTH-FRAUD` 88-levels (`'F'`/`'R'`) + toggle | `domain.FraudStatus` `CONFIRMED("F")` / `REMOVED("R")` + `toggle()` | `toggle()` mirrors confirmed ↔ removed. |
| `CIPAUDTY` PAUTDTL1 segment | `entity.PendingAuthDetail` | Relational stand-in for the IMS pending-auth-details record. |
| AUTHFRDS table (README DDL, PK `CARD_NUM, AUTH_TS`) | `entity.AuthFraud` + `entity.AuthFraudId` (`@EmbeddedId`) | `DECIMAL` → `BigDecimal`, `DATE` → `LocalDate`, `TIMESTAMP` → `LocalDateTime`. |
| `COPAUA0C` reason codes 5100 / 5200 | `domain.FraudReasonCode` `CARD_FRAUD(5100)` / `MERCHANT_FRAUD(5200)` | Card / merchant fraud decline reasons. |

## Build & test

```bash
cd app/app-authorization-ims-db2-mq/java
mvn clean package        # compile + run tests + build the jar
mvn test                 # run the JUnit / Spring Boot (H2) tests only
```

Integration tests cover: first-time fraud marking (insert), re-marking an already-fraud
record (toggle to removed), the existing-row upsert (update instead of insert), and
transactional rollback on a simulated AUTHFRDS failure.

## Run

```bash
cd app/app-authorization-ims-db2-mq/java
mvn spring-boot:run
```

The app starts on `http://localhost:8080` with an in-memory H2 database
(`spring.datasource.url=jdbc:h2:mem:carddemo`; H2 console at `/h2-console`). To point at a
real database (e.g. DB2), override the `spring.datasource.*` and `spring.jpa.*` properties.

### Toggle fraud on an authorization

```bash
curl -X POST "http://localhost:8080/api/authorizations/4111111111111111/2024-01-31T12:34:56/fraud-toggle"
# 200 OK  -> {"success":true,"message":"AUTH MARKED FRAUD..."}   (first toggle)
# 200 OK  -> {"success":true,"message":"AUTH FRAUD REMOVED..."}  (second toggle)
# 404     -> {"success":false,"message":"PENDING AUTH DETAIL NOT FOUND ..."}
```

`authTs` is an ISO-8601 date-time and, with `cardNum`, forms the AUTHFRDS primary key.

## Scope

This module intentionally does **not** wire up real IMS/DB2/MQ. It focuses on the fraud
detection business logic (COPAUS1C/COPAUS2C). The COBOL sources are left unchanged.
