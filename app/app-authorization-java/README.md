# CardDemo Credit Card Authorizations — Java (Spring Boot)

A Spring Boot re-implementation of the CardDemo **Credit Card Authorizations** extension,
rebuilt from the documented business requirements of the legacy
`app/app-authorization-ims-db2-mq` application (COBOL / CICS / IMS / DB2 / MQ).

This is a **behavioural rewrite**, not a line-by-line port: the business rules,
reason codes, message formats and transactional guarantees are preserved, while the
mainframe runtime (CICS transactions, IMS hierarchical DB, DB2 embedded SQL, MQ triggers,
BMS maps, JCL batch) is replaced with idiomatic Spring components.

See [`REQUIREMENTS.md`](./REQUIREMENTS.md) for the business requirements (BR-01…BR-10),
each of which is traced to code and tests below.

---

## Architecture

```
                 AWS.M2.CARDDEMO.PAUTH.REQUEST (JMS)
                             │
                 ┌───────────▼────────────┐
                 │ AuthorizationRequest    │  messaging/
                 │ Listener  (@JmsListener)│  (replaces COPAUA0C MQ trigger)
                 └───────────┬────────────┘
                             │ parse (AuthorizationMessageCodec)
                 ┌───────────▼────────────┐
                 │ AuthorizationRequest    │  service/
                 │ Service  (@Transactional)│  (orchestration + persistence)
                 └───────┬────────┬────────┘
          xref/account   │        │  decision
      ┌──────────────────▼─┐   ┌──▼───────────────────────────┐
      │ CrossReferenceSvc  │   │ AuthorizationDecisionEngine   │  service/decision/
      │ AccountService     │   │  + FraudScoringEngine (SPI)   │  (replaces 6000-MAKE-DECISION)
      └────────────────────┘   └──────────────────────────────┘
                             │ persist summary + detail
                 ┌───────────▼────────────┐
                 │ JPA repositories / H2 / │  domain/, repository/
                 │ Db2 (PENDING_AUTH_*,    │  (replaces IMS PAUTSUM0/PAUTDTL1 + DB2 AUTHFRDS)
                 │  AUTHFRDS)              │
                 └───────────┬────────────┘
                             │ formatReply
                 AWS.M2.CARDDEMO.PAUTH.REPLY (JMS)

  REST (web/)  ── list / view / fraud-toggle ──►  service/ ──► repositories
  Scheduler (batch/) ── purge expired auths ───►  ExpiredAuthorizationPurgeService
```

### Packages

| Package | Responsibility | Legacy equivalent |
|---|---|---|
| `messaging` | JMS listener + comma-separated request/reply codec | COPAUA0C MQ trigger, `CCPAURQY`/`CCPAURLY` copybooks |
| `domain` | JPA entities + code enums | IMS segments `PAUTSUM0`/`PAUTDTL1` (`CIPAUSMY`/`CIPAUDTY`), DB2 `AUTHFRDS`, `CCPAUERY` reason codes |
| `repository` | Spring Data JPA repositories | VSAM/IMS/DB2 file & SQL access |
| `service` | Request orchestration + persistence | COPAUA0C procedure division |
| `service.decision` | Rule-based decision engine + pluggable scoring SPI | COPAUA0C `6000-MAKE-DECISION`, `5600-READ-PROFILE-DATA` stub |
| `service.fraud` | Fraud mark/unmark toggle + `AUTHFRDS` upsert | COPAUS1C (PF5 toggle) + COPAUS2C (DB2 insert/update) |
| `service.xref` | Card→account cross-reference + account master lookup | VSAM `CCXREF`/`ACCTDAT` (`CVACT03Y`/`CVACT01Y`) |
| `web` | REST controllers replacing BMS screens | COPAUS0C / COPAUS1C BMS maps |
| `batch` | Scheduled expired-authorization purge | CBPAUP0C / CBPAUP0J |
| `config` | Clock, config-properties, demo data loader | ENVIRONMENT DIVISION / JCL parms |

---

## Program & resource mapping

| Legacy component | Java replacement |
|---|---|
| `COPAUA0C` (authorize) | `AuthorizationRequestListener`, `AuthorizationRequestService`, `RuleBasedAuthorizationDecisionEngine` |
| `COPAUS0C` (list screen) | `AuthorizationViewController` (list + summary), `AuthorizationViewService` |
| `COPAUS1C` (detail + PF5 toggle) | `AuthorizationViewController` (detail), `FraudController`, `FraudService` |
| `COPAUS2C` (`AUTHFRDS` insert/update) | `FraudService.toggleFraud` + `AuthFraudRepository` |
| `CBPAUP0C` / `CBPAUP0J` (purge) | `ExpiredAuthorizationPurgeService` + `PurgeScheduler` |
| IMS `PAUTSUM0` / `CIPAUSMY` | `PendingAuthSummary` |
| IMS `PAUTDTL1` / `CIPAUDTY` | `PendingAuthDetail` |
| DB2 `CARDDEMO.AUTHFRDS` | `AuthFraud` (`AuthFraudId` composite key) |
| CICS `SYNCPOINT` / `ROLLBACK` | Spring `@Transactional` |
| MQ `AWS.M2.CARDDEMO.PAUTH.REQUEST` / `.REPLY` | JMS queues (Artemis local, IBM MQ prod) |

### Data model

**`PENDING_AUTH_SUMMARY`** (root, PK `ACCT_ID`) — credit limits/balances and approved/declined
counters and amounts, with a `@OneToMany` to detail rows (cascade + orphan removal, mirroring the
IMS parent/child segment relationship).

**`PENDING_AUTH_DETAIL`** (child, surrogate `ID`, index on `CARD_NUM, AUTH_TS DESC`) — one row per
authorization: card/auth/merchant/transaction fields, `MATCH_STATUS` and `AUTH_FRAUD` flag.

**`AUTHFRDS`** (composite PK `CARD_NUM, AUTH_TS`, unique index `CARD_NUM ASC, AUTH_TS DESC`) —
maps the DB2 fraud table exactly, including `ACCT_ID`, `CUST_ID`, `AUTH_FRAUD`, `FRAUD_RPT_DATE`.

All monetary fields use `java.math.BigDecimal`; timestamps use `LocalDateTime`; the fraud report
date uses `LocalDate`.

### Reason codes (`AuthResponseReason`, from `CCPAUERY`)

| Code | Meaning | | Code | Meaning |
|---|---|---|---|---|
| 0000 | APPROVED | | 4300 | ACCOUNT CLOSED |
| 3100 | INVALID CARD | | 4400 | EXCEEDED DAILY LIMIT |
| 4100 | INSUFFICIENT FUND | | **5100** | **CARD FRAUD** |
| 4200 | CARD NOT ACTIVE | | **5200** | **MERCHANT FRAUD** |
| | | | **5300** | **LOST CARD** |

Approved replies carry `AUTH-RESP-CODE=00`, `AUTH-RESP-REASON=0000`, `APPROVED-AMT=transaction amount`;
declines carry `AUTH-RESP-CODE=05` and `APPROVED-AMT=0`.

---

## MQ message formats (BR-06)

**Request** (`AWS.M2.CARDDEMO.PAUTH.REQUEST`) — 18 comma-separated fields:

```
AUTH-DATE, AUTH-TIME, CARD-NUM, AUTH-TYPE, CARD-EXPIRY-DATE, MESSAGE-TYPE,
MESSAGE-SOURCE, PROCESSING-CODE, TRANSACTION-AMT, MERCHANT-CATAGORY-CODE,
ACQR-COUNTRY-CODE, POS-ENTRY-MODE, MERCHANT-ID, MERCHANT-NAME, MERCHANT-CITY,
MERCHANT-STATE, MERCHANT-ZIP, TRANSACTION-ID
```

**Reply** (`AWS.M2.CARDDEMO.PAUTH.REPLY`) — 6 comma-separated fields:

```
CARD-NUM, TRANSACTION-ID, AUTH-ID-CODE, AUTH-RESP-CODE, AUTH-RESP-REASON, APPROVED-AMT
```

`APPROVED-AMT` is formatted with an explicit sign and two decimals (e.g. `+250.00`).

Example round-trip:

```
in : 240115,120000,4111111111111111,0100,2512,0100,0100,000000,250.00,5411,840,90,MERCH000000001,TEST MERCHANT,ANYTOWN,NY,10001,TX0000000000001
out: 4111111111111111,TX0000000000001,120000,00,0000,+250.00
```

---

## Decisioning rules (BR-01)

1. Default is **APPROVE**.
2. Available amount = `credit limit − credit balance`.
3. Prefer the **authorization summary** figures; fall back to **account master** if no summary.
4. If neither summary nor account is available (or the card/customer is not found in the
   cross-reference) → decline **3100 INVALID CARD**.
5. If `transaction amount > available amount` → decline **4100 INSUFFICIENT FUND**
   (boundary uses `>`, so an amount equal to available is **approved**).
6. Fraud reason codes 5100/5200/5300 and any statistical scoring are produced through the
   pluggable `FraudScoringEngine` SPI.

### Pluggable fraud scoring (preserved legacy limitation)

The COBOL `5600-READ-PROFILE-DATA` paragraph is a `CONTINUE` stub — there is **no statistical
fraud scoring**. That limitation is preserved: `FraudScoringEngine` is an SPI whose default
implementation (`NoOpFraudScoringEngine`) always returns "no opinion", so decisioning stays
rule-based plus manual operator tagging. To add scoring later, publish another
`FraudScoringEngine` bean — `@ConditionalOnMissingBean` means it automatically replaces the
no-op without touching the decision engine.

---

## REST API (replaces BMS screens)

| Method & path | Purpose |
|---|---|
| `GET /api/accounts/{acctId}/summary` | Authorization summary for an account |
| `GET /api/accounts/{acctId}/authorizations?page=&size=` | List authorization details (default page size **5**, mirroring the BMS display) |
| `GET /api/authorizations/{detailId}` | View a single authorization |
| `POST /api/authorizations/{detailId}/fraud-toggle` | Mark/unmark fraud (PF5 equivalent) |
| `POST /api/reference/xref` / `POST /api/reference/accounts` | Seed cross-reference/account data (see note below) |

### Fraud toggle & transactional integrity (BR-03…BR-05, BR-07)

`FraudService.toggleFraud` mirrors the COBOL PF5 toggle: if the authorization is already
fraud-confirmed (`F`) it is set to removed (`R`), otherwise it is marked fraud (`F`). On each
toggle the `AUTHFRDS` row is **upserted** — inserted when absent, otherwise the `AUTH_FRAUD`
and `FRAUD_RPT_DATE` columns are updated (the equivalent of DB2 `SQLCODE -803` handling). The
detail update and the `AUTHFRDS` write run in a single `@Transactional` unit, so both commit or
both roll back — preserving the guarantee the COBOL achieved with `SYNCPOINT` / `ROLLBACK`.

---

## Batch purge (BR-09, replaces CBPAUP0C)

`ExpiredAuthorizationPurgeService.purgeExpired(expiryDays)` deletes authorization details whose
age (today − `AUTH_TS` date) is `>= expiryDays` (default **5**, matching the COBOL default),
adjusts the summary approved/declined counts and amounts, releases held credit for expired
approved authorizations, and deletes summaries with no remaining authorizations. `PurgeScheduler`
runs it on a cron schedule (default `0 0 2 * * *`); disable with `carddemo.purge.enabled=false`.

---

## Configuration

Key settings (`src/main/resources/application.yml`):

| Property | Default | Purpose |
|---|---|---|
| `carddemo.mq.request-queue` | `AWS.M2.CARDDEMO.PAUTH.REQUEST` | inbound queue |
| `carddemo.mq.reply-queue` | `AWS.M2.CARDDEMO.PAUTH.REPLY` | outbound queue |
| `carddemo.purge.expiry-days` | `5` | purge age threshold |
| `carddemo.purge.cron` | `0 0 2 * * *` | purge schedule |
| `carddemo.purge.enabled` | `true` | enable scheduled purge |

### Messaging profiles

- **Default (local/dev):** an embedded **ActiveMQ Artemis** broker; the IBM MQ auto-config is
  excluded so nothing tries to reach a real queue manager.
- **`ibmmq` profile (`application-ibmmq.yml`):** connects to **IBM MQ** via
  `mq-jms-spring-boot-starter` and excludes Artemis. Configure via env vars
  `IBM_MQ_QMGR`, `IBM_MQ_CHANNEL`, `IBM_MQ_CONN_NAME`, `IBM_MQ_USER`, `IBM_MQ_PASSWORD`.

### Database

- **Default:** in-memory **H2** (`jdbc:h2:mem:carddemo`), schema auto-created; H2 console at
  `/h2-console`.
- **Production (`ibmmq` profile shown as example):** point `CARDDEMO_DB_URL` / `CARDDEMO_DB_USER` /
  `CARDDEMO_DB_PASSWORD` at Db2 (or any JPA-supported RDBMS) and use `ddl-auto: validate` against a
  schema created from the DB2 DDL.

### Reference data

The VSAM cross-reference (`CCXREF`) and account master (`ACCTDAT`) files are outside this
extension's scope, so card→account/credit data is served by an in-memory
`InMemoryReferenceDataStore` (seed it via `POST /api/reference/*`, or automatically with the
`demo` profile). In production, replace it with an adapter over the real system of record by
providing `CrossReferenceService` / `AccountService` beans.

---

## Build & run

```bash
# from app/app-authorization-java
mvn test                       # run all unit + integration tests
mvn -q -DskipTests package     # build the executable jar

# run locally (embedded Artemis + H2, with demo reference data seeded)
mvn spring-boot:run -Dspring-boot.run.profiles=demo
# or
java -jar target/app-authorization-java-1.0.0.jar --spring.profiles.active=demo
```

Requires **JDK 17+** and Maven 3.6+.

### Quick manual check

```bash
# after starting with the demo profile (card 4111111111111111 -> account 100000001):
curl localhost:8080/api/accounts/100000001/summary
curl localhost:8080/api/accounts/100000001/authorizations
curl -X POST localhost:8080/api/authorizations/1/fraud-toggle
```

---

## Tests & requirement traceability

| Test | Requirements |
|---|---|
| `RuleBasedAuthorizationDecisionEngineTest` | BR-01 (rules, limit boundary, missing data), BR-02, BR-01.8 |
| `AuthResponseReasonTest` | BR-01.7, BR-02 (5100/5200/5300) |
| `AuthorizationMessageCodecTest` | BR-06 (request/reply formats, decimals) |
| `AuthorizationRequestServiceTest` | BR-07 (orchestration, persistence, no-persist on unknown card) |
| `FraudServiceTest` | BR-03 (mark/unmark), BR-04 (upsert idempotency / -803 equivalent) |
| `FraudServiceRollbackIT` | BR-05 (transactional rollback on fraud-write failure) |
| `ExpiredAuthorizationPurgeServiceIT` | BR-09 (expiry, counter/credit adjustment, summary deletion) |
| `AuthorizationJmsIT` | BR-06 + BR-07 (end-to-end request→reply over Artemis) |
| `AuthorizationViewControllerIT` | BR-08 (list/pagination/detail), BR-03 (toggle endpoint) |
| `AuthorizationApplicationTests` | full context (JPA + JMS + scheduling) starts |
