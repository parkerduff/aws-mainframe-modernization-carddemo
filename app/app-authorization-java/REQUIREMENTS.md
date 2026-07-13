# Credit Card Authorizations — Business Requirements (Java Rewrite)

This document captures the business requirements of the CardDemo **Credit Card
Authorizations** extension as testable acceptance criteria. The Java (Spring Boot)
application under `app/app-authorization-java` is built from these requirements
rather than as a line-by-line port of the legacy COBOL/CICS/IMS/DB2/MQ programs.

The source of truth for the rules is the legacy implementation in
`app/app-authorization-ims-db2-mq`:

| Legacy program | Role |
|---|---|
| `COPAUA0C.cbl` | MQ-triggered authorization request processor + decisioning (`6000-MAKE-DECISION`) |
| `COPAUS0C.cbl` | BMS summary screen — list authorizations for an account (pagination) |
| `COPAUS1C.cbl` | BMS detail screen — view one authorization + fraud mark toggle (`MARK-AUTH-FRAUD`) |
| `COPAUS2C.cbl` | Fraud persistence into DB2 `AUTHFRDS` (insert + `-803` upsert) |
| `CBPAUP0C.cbl` | Batch purge of expired authorizations (`CBPAUP0J` JCL) |
| copybooks `CIPAUSMY`, `CIPAUDTY`, `CCPAURQY`, `CCPAURLY`, `CCPAUERY` | data layouts |
| `ddl/AUTHFRDS.ddl`, `ddl/XAUTHFRD.ddl`, `dcl/AUTHFRDS.dcl` | DB2 table + index |

Each requirement below is given an ID (`BR-xx`) and lists the code and tests that
satisfy it. IDs are referenced from Javadoc/test names to keep traceability.

---

## BR-01 — Authorization decisioning rules

Derived from `COPAUA0C` paragraph `6000-MAKE-DECISION`.

- **BR-01.1** The default decision is **APPROVE** (response code `00`), approved
  amount = requested transaction amount, reason code `0000`.
- **BR-01.2** *Available amount* = `credit limit − credit balance`. When a pending
  authorization **summary** exists for the account, the summary's credit limit and
  credit balance are used.
- **BR-01.3** When no summary exists but the **account master** is found, available
  amount = `account credit limit − account current balance`.
- **BR-01.4** When the transaction amount is **greater than** the available amount,
  the request is **DECLINED** (response code `05`) with reason **`4100`
  INSUFFICIENT FUND**. A transaction amount *equal to* the available amount is
  approved (boundary: `>` not `>=`).
- **BR-01.5** When neither a summary nor an account master is found, the request is
  **DECLINED**. Reason **`3100` INVALID CARD** (also used when the card is not in
  the cross-reference, or the customer master is not found).
- **BR-01.6** On decline the approved amount is `0`.
- **BR-01.7** Reason-code catalogue (from `COPAUS1C` `WS-DECLINE-REASON-TABLE`):

  | Code | Description |
  |---|---|
  | `0000` | APPROVED |
  | `3100` | INVALID CARD (card/account/customer not found) |
  | `4100` | INSUFFICIENT FUND |
  | `4200` | CARD NOT ACTIVE |
  | `4300` | ACCOUNT CLOSED |
  | `4400` | EXCEEDED DAILY LIMIT |
  | `5100` | CARD FRAUD |
  | `5200` | MERCHANT FRAUD |
  | `5300` | LOST CARD |
  | `9000` | UNKNOWN |

- **BR-01.8 (legacy limitation, preserved + extended)** The legacy program only
  ever *sets* the INSUFFICIENT-FUND and not-found decline reasons; `5100`/`5200`/
  `5300`/`4200`/`4300` exist in the reason catalogue but are never triggered because
  the statistical scoring paragraph `5600-READ-PROFILE-DATA` is a `CONTINUE` stub.
  The Java decisioning service keeps rule-based + manual-tag behaviour but exposes a
  **pluggable `FraudScoringEngine`** so a scoring implementation can later return
  `CARD_FRAUD`/`MERCHANT_FRAUD`/`LOST_CARD` and drive a decline. The default engine
  is a no-op (mirrors the stub).

*Code:* `service/decision/RuleBasedAuthorizationDecisionEngine`,
`domain/AuthResponseReason`, `service/decision/FraudScoringEngine` (+ `NoOpFraudScoringEngine`).
*Tests:* `RuleBasedAuthorizationDecisionEngineTest`.

---

## BR-02 — Fraud reason codes

The reason catalogue includes the fraud codes **`5100` CARD FRAUD**, **`5200`
MERCHANT FRAUD**, **`5300` LOST CARD**. These map to `FraudScoringEngine` outcomes
and to `AuthResponseReason` enum constants with their exact code + description text.

*Code:* `domain/AuthResponseReason`, `service/decision/FraudAssessment`.
*Tests:* `AuthResponseReasonTest`, `RuleBasedAuthorizationDecisionEngineTest`.

---

## BR-03 — Operator fraud mark/unmark toggle

Derived from `COPAUS1C` `MARK-AUTH-FRAUD` (PF5 on the detail screen).

- **BR-03.1** If the authorization detail is currently **fraud-confirmed** (`AUTH-FRAUD = 'F'`),
  toggling **removes** the flag (`'R'`, action REMOVE).
- **BR-03.2** Otherwise toggling **marks** it fraud (`'F'`, action REPORT).
- **BR-03.3** Toggling stamps the current **fraud report date** on the detail.

*Code:* `service/fraud/FraudService#toggleFraud`, `domain/FraudFlag`.
*Tests:* `FraudServiceTest` (mark, unmark, re-mark).

---

## BR-04 — Fraud persistence and idempotency

Derived from `COPAUS2C` (DB2 `AUTHFRDS` insert / `-803` update).

- **BR-04.1** On toggle, the full authorization detail is copied into the
  `AUTHFRDS` record (all columns of the DDL) with `AUTH_FRAUD` = the action flag and
  `FRAUD_RPT_DATE` = current date.
- **BR-04.2** The row key is `(CARD_NUM, AUTH_TS)`. If a row already exists (the DB2
  `SQLCODE -803` duplicate-key case), the operation performs an **UPDATE** of
  `AUTH_FRAUD` and `FRAUD_RPT_DATE` instead of an insert (upsert semantics).
- **BR-04.3** Re-marking the same authorization repeatedly is idempotent: it never
  creates duplicate rows and always leaves exactly one row reflecting the latest flag.

*Code:* `service/fraud/FraudService#upsertFraudRecord`, `repository/AuthFraudRepository`,
`domain/AuthFraud`.
*Tests:* `FraudServiceTest` (idempotent re-mark), `AuthFraudRepositoryIT`.

---

## BR-05 — Transactional integrity (single unit of work)

Derived from `COPAUS1C` + `COPAUS2C` `SYNCPOINT`/`ROLLBACK` (IMS + DB2 two-phase commit).

- **BR-05.1** The authorization-detail fraud-flag update and the `AUTHFRDS`
  insert/update are performed in a **single transactional unit** (`@Transactional`).
- **BR-05.2** If the fraud-table write fails, the authorization-detail update is
  **rolled back** — neither change is persisted (equivalent of `SYNCPOINT ROLLBACK`).

*Code:* `service/fraud/FraudService#toggleFraud` (`@Transactional`).
*Tests:* `FraudServiceTest#toggleFraud_rollsBackDetailWhenFraudWriteFails`.

---

## BR-06 — MQ request/reply message formats

Derived from the README "MQ Message Formats", `CCPAURQY`, `CCPAURLY`, and
`COPAUA0C` `2100-EXTRACT-REQUEST-MSG` / `6000` reply `STRING`.

- **BR-06.1** Requests arrive on queue `AWS.M2.CARDDEMO.PAUTH.REQUEST` as
  comma-separated values in this field order: `AUTH-DATE, AUTH-TIME, CARD-NUM,
  AUTH-TYPE, CARD-EXPIRY-DATE, MESSAGE-TYPE, MESSAGE-SOURCE, PROCESSING-CODE,
  TRANSACTION-AMT, MERCHANT-CATAGORY-CODE, ACQR-COUNTRY-CODE, POS-ENTRY-MODE,
  MERCHANT-ID, MERCHANT-NAME, MERCHANT-CITY, MERCHANT-STATE, MERCHANT-ZIP,
  TRANSACTION-ID`.
- **BR-06.2** Replies are published on queue `AWS.M2.CARDDEMO.PAUTH.REPLY` as
  comma-separated values in this order: `CARD-NUM, TRANSACTION-ID, AUTH-ID-CODE,
  AUTH-RESP-CODE, AUTH-RESP-REASON, APPROVED-AMT`.
- **BR-06.3** `TRANSACTION-AMT` is parsed as a signed decimal amount; `APPROVED-AMT`
  is formatted as a signed decimal (COBOL `+9(10).99`).

*Code:* `messaging/AuthorizationMessageCodec`, `messaging/AuthorizationRequestListener`.
*Tests:* `AuthorizationMessageCodecTest`, `AuthorizationJmsIT`.

---

## BR-07 — Authorization request processing (orchestration)

Derived from `COPAUA0C` `5000-PROCESS-AUTH` / `8000-WRITE-AUTH-TO-DB`.

- **BR-07.1** The card number is resolved to account/customer via a
  **cross-reference** lookup (ports the VSAM `CCXREF` → account → customer chain).
- **BR-07.2** The decision is made per BR-01, a reply is produced per BR-06.
- **BR-07.3** When the card is found in the cross-reference, the resulting
  authorization **summary** (created or updated) and **detail** records are
  persisted; approved authorizations increment approved counters and add the
  approved amount to the held credit balance, declined authorizations increment
  declined counters (mirrors `8400-UPDATE-SUMMARY`). New details start with match
  status `P` (pending) when approved, `D` when declined, and no fraud flag.
- **BR-07.4** When the card is not found in the cross-reference, no records are
  persisted but a decline reply (`3100`) is still returned.

*Code:* `service/AuthorizationRequestService`, `service/xref/CrossReferenceService`,
`service/xref/AccountService`.
*Tests:* `AuthorizationRequestServiceTest`, `AuthorizationJmsIT`.

---

## BR-08 — Authorization viewing (summary + detail + pagination)

Derived from `COPAUS0C` (summary list, PF7/PF8 paging, 5 rows/page) and `COPAUS1C`
(detail view).

- **BR-08.1** List authorization summaries/details for an account, most recent
  first, with pagination (default page size 5, matching the BMS 5-row screen).
- **BR-08.2** View the full detail of a selected authorization.
- **BR-08.3** Each list row exposes: transaction id, original date/time, auth type,
  approved/declined status (derived from response code `00`), match status, approved
  amount.

*Code:* `web/AuthorizationViewController`, `service/AuthorizationViewService`.
*Tests:* `AuthorizationViewControllerIT`.

---

## BR-09 — Batch purge of expired authorizations

Derived from `CBPAUP0C` (`CBPAUP0J`).

- **BR-09.1** A scheduled job purges authorization details whose age in days
  (`today − authorization date`) is **≥ the configured expiry days** (default `5`).
- **BR-09.2** When an expired **approved** detail is deleted, the summary approved
  count is decremented, the approved amount is subtracted from the approved total,
  and the held credit is released (credit balance reduced by the approved amount) —
  this is the "adjust available credit when unmatched authorizations are deleted"
  requirement. When an expired **declined** detail is deleted, the declined count and
  declined amount are decremented.
- **BR-09.3** After processing an account, if it has no remaining approved or
  declined authorizations, the summary is deleted; otherwise the adjusted summary is
  saved.

*Code:* `batch/ExpiredAuthorizationPurgeService`, `batch/PurgeScheduler`.
*Tests:* `ExpiredAuthorizationPurgeServiceIT`.

---

## BR-10 — Configuration & environment

- **BR-10.1** Persistence uses Spring Data JPA — **H2** for tests/local, configurable
  to a production RDBMS (e.g. Db2/PostgreSQL) via `spring.datasource.*`.
- **BR-10.2** Messaging uses Spring JMS — an **embedded ActiveMQ Artemis** broker for
  local/test, configurable to **IBM MQ** via the `ibmmq` profile
  (`ibm.mq.*` properties) for production.
- **BR-10.3** The `AUTHFRDS` table and its unique index `(CARD_NUM ASC, AUTH_TS DESC)`
  are modelled exactly as in the DDL.

*Code:* `resources/application.yml`, `config/*`, `domain/AuthFraud`.
*Tests:* full test suite runs on H2 + embedded Artemis.

</invoke>
