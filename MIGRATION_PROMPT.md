# CardDemo Migration Prompt: COBOL/CICS Mainframe → Java 21 + Spring Boot

> **This is an executable specification.** Read it end to end, then perform the migration described below. It is self-contained: you do not need prior knowledge of the CardDemo application. Everything you need to understand the legacy system, the target architecture, and the exact translation rules is here. Where this document references source files, read them directly from the repository before translating.

---

## 1. Introduction / Context

**CardDemo** is a credit card management application originally written in **COBOL** and run on an IBM mainframe under **CICS** (online transaction processing) and **JCL/batch** (nightly processing). It manages customers, accounts, credit cards, and financial transactions. The online side is a set of pseudo-conversational CICS programs driving 3270 terminal screens (defined as BMS maps); the batch side posts transactions, calculates interest, and generates statements against VSAM files.

Your task is to **migrate the entire application to Java 21 with Spring Boot 3.2+**, replacing:
- CICS online programs with REST controllers + services,
- BMS terminal screens with a JSON REST API,
- VSAM files with a PostgreSQL relational database,
- JCL batch jobs with Spring Batch jobs,
- COMMAREA session-passing with stateless JWT authentication.

- **Repository:** `ankehao-demo/aws-mainframe-modernization-carddemo`
- **Source root:** all legacy assets live under `app/`.
- **Output:** a new, self-contained Maven project under `carddemo-java/` (see §4).

The migration must **preserve business behavior**: validation rules, financial calculations (exact decimal arithmetic), routing/authorization logic, and record layouts. Do not introduce floating-point arithmetic for money. Do not silently drop fields.

---

## 2. Source Application Inventory

All paths are relative to the repository root.

### 2.1 Online CICS COBOL programs — `app/cbl/CO*.cbl`
These are the interactive (screen-driven) programs. Migrate each to a controller + service:

| Program | Purpose |
|---|---|
| `COSGN00C` | Sign-on / authentication |
| `COMEN01C` | Main menu (regular user) |
| `COADM01C` | Admin menu |
| `COACTVWC` | Account view |
| `COACTUPC` | Account update |
| `COTRN00C` | Transaction list |
| `COTRN01C` | Transaction detail / view |
| `COTRN02C` | Transaction add |
| `COBIL00C` | Bill payment |
| `COCRDLIC` | Card list |
| `COCRDSLC` | Card detail / search |
| `COCRDUPC` | Card update |
| `COUSR00C` | User list |
| `COUSR01C` | User add |
| `COUSR02C` | User update |
| `COUSR03C` | User delete |
| `CORPT00C` | Reporting |
| `COMEN02C` | Secondary/extended menu |

> **Note on the repository:** the actual `app/cbl/` directory also contains `COBSWAIT.cbl` (a CICS wait/utility helper). If `COMEN02C.cbl` is not present, treat the menu copybook `app/cpy/COMEN02Y.cpy` as the source of the extended menu options and fold its behavior into `MenuService`. Read the directory before assuming; migrate what exists and note any deviation.

### 2.2 Batch COBOL programs — `app/cbl/CB*.cbl`
| Program | Purpose |
|---|---|
| `CBTRN01C` | Transaction validation (daily) |
| `CBTRN02C` | Transaction posting |
| `CBTRN03C` | Statement / transaction report |
| `CBACT01C` | Account master read/print |
| `CBACT02C` | Card master read/print |
| `CBACT03C` | Cross-reference read/print |
| `CBACT04C` | Interest calculation |
| `CBCUS01C` | Customer master read/print |

> The repository also contains `CBEXPORT.cbl` / `CBIMPORT.cbl` (branch data export/import). These are out of scope for the core migration unless explicitly requested; if migrated, model them as data import/export endpoints or batch jobs.

### 2.3 Utility program
- `app/cbl/CSUTLDTC.cbl` — date validation/conversion utility (wraps the CEEDAYS/date logic). Migrate to a `DateUtil` / `DateValidationService` backed by `java.time`.

### 2.4 Copybooks (shared record/data layouts)
- `app/cpy/` — record layouts and constants (e.g. `CVACT01Y`, `CVTRA05Y`, `CSUSR01Y`, `CVCRD01Y`, `COCOM01Y`). These define your entities/DTOs (see §3).
- `app/cpy-bms/` — copybooks generated from the BMS maps (screen field definitions). Use these to understand which fields each screen reads/writes when designing request/response DTOs.

### 2.5 BMS maps — `app/bms/`
3270 screen layouts (e.g. `COSGN00.bms`, `COMEN01.bms`, `COACTVW.bms`). Each map corresponds to one screen of a program. Use them to determine input vs. output fields, field lengths, and validation hints for the REST request/response DTOs.

### 2.6 JCL — `app/jcl/`
Job control scripts that orchestrate batch programs. Key ones: `POSTTRAN.jcl` (transaction posting), `INTCALC.jcl` (interest calculation). Read these to understand step sequencing, input/output datasets, and job parameters for the Spring Batch jobs.

### 2.7 Assembler utility — `app/asm/COBDATFT.asm`
A date-formatting assembler routine. Replace with `java.time.format.DateTimeFormatter`. (The repo also has `MVSWAIT.asm`, a wait routine with no business meaning — ignore it.)

### 2.8 Sample data — `app/data/ASCII/`
ASCII (already converted from EBCDIC) flat files used to seed the system and to drive tests:
`acctdata.txt`, `carddata.txt`, `custdata.txt`, `cardxref.txt`, `dailytran.txt`, `tcatbal.txt`, `trancatg.txt`, `trantype.txt`, `discgrp.txt`. Use these as Flyway seed data and as fixtures for integration tests (see §10). Field positions are defined by the corresponding copybooks.

### 2.9 Extension modules (advanced / optional)
- `app/app-authorization-ims-db2-mq/` — IMS DB + DB2 + IBM MQ real-time authorization subsystem.
- `app/app-transaction-type-db2/` — DB2-based transaction type lookup.
- `app/app-vsam-mq/` — VSAM + MQ service-enablement.

Migrate the core online + batch application first. Treat these modules as a **second phase**: the MQ asynchronous flows map to message-driven components (e.g. Spring's `@JmsListener`/Kafka or an internal async service), and the DB2/IMS data maps to additional PostgreSQL tables. Do not block the core migration on these.

---

## 3. Key Data Structures (Copybook Layouts)

The following copybook layouts define the canonical data model. Translate each to a Java record (DTO) and/or a JPA `@Entity` per §6. **Field names, sizes, and signs below are authoritative** — reproduce them faithfully.

### 3.1 `app/cpy/COCOM01Y.cpy` — COMMAREA (inter-program communication / session state)
```cobol
01 CARDDEMO-COMMAREA.
   05 CDEMO-GENERAL-INFO.
      10 CDEMO-FROM-TRANID    PIC X(04).
      10 CDEMO-FROM-PROGRAM   PIC X(08).
      10 CDEMO-TO-TRANID      PIC X(04).
      10 CDEMO-TO-PROGRAM     PIC X(08).
      10 CDEMO-USER-ID        PIC X(08).
      10 CDEMO-USER-TYPE      PIC X(01).
         88 CDEMO-USRTYP-ADMIN VALUE 'A'.
         88 CDEMO-USRTYP-USER  VALUE 'U'.
      10 CDEMO-PGM-CONTEXT    PIC 9(01).
         88 CDEMO-PGM-ENTER    VALUE 0.
         88 CDEMO-PGM-REENTER  VALUE 1.
   05 CDEMO-CUSTOMER-INFO.
      10 CDEMO-CUST-ID        PIC 9(09).
      10 CDEMO-CUST-FNAME     PIC X(25).
      10 CDEMO-CUST-MNAME     PIC X(25).
      10 CDEMO-CUST-LNAME     PIC X(25).
   05 CDEMO-ACCOUNT-INFO.
      10 CDEMO-ACCT-ID        PIC 9(11).
      10 CDEMO-ACCT-STATUS    PIC X(01).
   05 CDEMO-CARD-INFO.
      10 CDEMO-CARD-NUM       PIC 9(16).
   05 CDEMO-MORE-INFO.
      10 CDEMO-LAST-MAP       PIC X(7).
      10 CDEMO-LAST-MAPSET    PIC X(7).
```
**Migration:** the COMMAREA carries cross-program state on the mainframe. In the Java target, the *session* portion (`CDEMO-USER-ID`, `CDEMO-USER-TYPE` with its ADMIN/USER 88-levels) becomes **JWT claims** (§8). The navigation fields (`*-TRANID`, `*-PROGRAM`, `LAST-MAP`, `PGM-CONTEXT`) are CICS routing artifacts and are **eliminated** — REST is stateless and routing is done client-side. The customer/account/card "selection" fields become explicit request parameters/path variables.

### 3.2 `app/cpy/CVACT01Y.cpy` — Account record (RECLN 300)
```cobol
01 ACCOUNT-RECORD.
   05 ACCT-ID                PIC 9(11).
   05 ACCT-ACTIVE-STATUS     PIC X(01).
   05 ACCT-CURR-BAL          PIC S9(10)V99.
   05 ACCT-CREDIT-LIMIT      PIC S9(10)V99.
   05 ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99.
   05 ACCT-OPEN-DATE         PIC X(10).
   05 ACCT-EXPIRAION-DATE    PIC X(10).
   05 ACCT-REISSUE-DATE      PIC X(10).
   05 ACCT-CURR-CYC-CREDIT   PIC S9(10)V99.
   05 ACCT-CURR-CYC-DEBIT    PIC S9(10)V99.
   05 ACCT-ADDR-ZIP          PIC X(10).
   05 ACCT-GROUP-ID          PIC X(10).
   05 FILLER                 PIC X(178).
```
(Preserve the misspelled `ACCT-EXPIRAION-DATE` field semantics; name the Java field `expirationDate` but document the source name.)

### 3.3 `app/cpy/CVTRA05Y.cpy` — Transaction record (RECLN 350)
```cobol
01 TRAN-RECORD.
   05 TRAN-ID            PIC X(16).
   05 TRAN-TYPE-CD       PIC X(02).
   05 TRAN-CAT-CD        PIC 9(04).
   05 TRAN-SOURCE        PIC X(10).
   05 TRAN-DESC          PIC X(100).
   05 TRAN-AMT           PIC S9(09)V99.
   05 TRAN-MERCHANT-ID   PIC 9(09).
   05 TRAN-MERCHANT-NAME PIC X(50).
   05 TRAN-MERCHANT-CITY PIC X(50).
   05 TRAN-MERCHANT-ZIP  PIC X(10).
   05 TRAN-CARD-NUM      PIC X(16).
   05 TRAN-ORIG-TS       PIC X(26).
   05 TRAN-PROC-TS       PIC X(26).
   05 FILLER             PIC X(20).
```
(`TRAN-ORIG-TS` / `TRAN-PROC-TS` are 26-char timestamps `YYYY-MM-DD-HH.MM.SS.ffffff` → map to `LocalDateTime`/`Instant`.)

### 3.4 `app/cpy/CSUSR01Y.cpy` — Security user record
```cobol
01 SEC-USER-DATA.
   05 SEC-USR-ID     PIC X(08).
   05 SEC-USR-FNAME  PIC X(20).
   05 SEC-USR-LNAME  PIC X(20).
   05 SEC-USR-PWD    PIC X(08).
   05 SEC-USR-TYPE   PIC X(01).
   05 SEC-USR-FILLER PIC X(23).
```
**Migration:** `SEC-USR-PWD` is **plaintext** in the legacy system. In the Java target, store a **BCrypt hash** instead (§8). `SEC-USR-TYPE` maps to a role: `'A'` → `ROLE_ADMIN`, `'U'` → `ROLE_USER`.

### 3.5 `app/cpy/CVCRD01Y.cpy` — Card record (note the `REDEFINES` pattern)
The card work area uses COBOL `REDEFINES` to view the same bytes as both alphanumeric (`PIC X`) and numeric (`PIC 9`):
```cobol
10 CC-ACCT-ID   PIC X(11) VALUE SPACES.
10 CC-ACCT-ID-N REDEFINES CC-ACCT-ID PIC 9(11).
10 CC-CARD-NUM   PIC X(16) VALUE SPACES.
10 CC-CARD-NUM-N REDEFINES CC-CARD-NUM PIC 9(16).
10 CC-CUST-ID   PIC X(09) VALUE SPACES.
10 CC-CUST-ID-N REDEFINES CC-CUST-ID PIC 9(9).
```
The copybook also defines `CCARD-AID` (88-level AID/PF-key conditions: ENTER, CLEAR, PA1/PA2, PFK01–PFK12) and message/navigation work fields. The AID/PF-key logic is terminal-interaction state and is **not** carried into REST.

**Migration of `REDEFINES`:** these are alternate views of one field (a string that *may* hold digits). Model each as a single canonical field (the `X` form, a trimmed `String`), and provide **conversion/factory methods** on the record for the numeric view, e.g.:
```java
public record CardKey(String acctId, String cardNum, String custId) {
    public long acctIdNumeric() { return Long.parseLong(acctId.trim()); }
    public long cardNumNumeric() { return Long.parseLong(cardNum.trim()); }
    public long custIdNumeric()  { return Long.parseLong(custId.trim()); }
}
```
Validate that the string is all digits before exposing the numeric view (mirrors the COBOL assumption that the field holds a number).

> Other copybooks you will need while migrating: `CVCUS01Y` (customer), `CVACT02Y`/`CVACT03Y` (card master, card xref), `CVTRA01Y`–`CVTRA07Y` (transaction type/category/balance), `CSUTLDPY`/`CSUTLDWY` (date utility), `CSMSG01Y`/`CSMSG02Y` (messages). Read them in `app/cpy/` and translate using the same rules.

---

## 4. Target Architecture Specification

Produce a new Maven project at **`carddemo-java/`** with:

- **Java 21** (use language features in §11).
- **Spring Boot 3.2+** (Spring Web, Spring Data JPA, Spring Validation, Spring Security 6, Spring Batch).
- **Maven** build via `pom.xml` (single module; multi-module is acceptable if it improves clarity).
- **PostgreSQL** as the database; schema and seed data managed by **Flyway** migrations under `src/main/resources/db/migration`.
- **Spring Security 6** with **JWT** bearer-token authentication.
- **Spring Batch** for the migrated batch jobs.
- **Virtual threads enabled**: `spring.threads.virtual.enabled=true`.
- An **OpenAPI 3.x** spec for the REST API (keep it version-controlled; keep code annotations in sync).

### Package structure — root `com.aws.carddemo`
```
com.aws.carddemo
├── entity      // JPA @Entity classes (Account, Card, Customer, Transaction, User, ...)
├── repository  // Spring Data JpaRepository interfaces
├── service     // business logic (ported COBOL paragraphs)
├── controller  // REST controllers (ported CICS online programs)
├── batch       // Spring Batch jobs/steps/readers/processors/writers
├── config      // Spring configuration (datasource, batch, OpenAPI, virtual threads)
├── security    // JWT filter, SecurityFilterChain, UserDetailsService, BCrypt
└── dto         // request/response records
```

---

## 5. Detailed Mapping Tables

### 5.1 Online program → controller/service

| COBOL Program | CICS Tran | Java Controller | Java Service |
|---|---|---|---|
| COSGN00C | CC00 | `AuthController` | `AuthService` |
| COMEN01C | CM00 | `MenuController` | `MenuService` |
| COADM01C | CA00 | `AdminMenuController` | `AdminMenuService` |
| COACTVWC | CAVW | `AccountController` (GET) | `AccountService` |
| COACTUPC | CAUP | `AccountController` (PUT) | `AccountService` |
| COTRN00C | CT00 | `TransactionController` (list) | `TransactionService` |
| COTRN01C | CT01 | `TransactionController` (detail) | `TransactionService` |
| COTRN02C | CT02 | `TransactionController` (create) | `TransactionService` |
| COBIL00C | CB00 | `BillPayController` | `BillPayService` |
| COCRDLIC | — | `CardController` (list) | `CardService` |
| COCRDSLC | — | `CardController` (search) | `CardService` |
| COCRDUPC | — | `CardController` (update) | `CardService` |
| COUSR00C–03C | CU00–03 | `UserController` (CRUD) | `UserService` |
| CORPT00C | — | `ReportController` | `ReportService` |

### 5.2 Batch program → Spring Batch job

| COBOL Program | JCL | Spring Batch Job |
|---|---|---|
| CBTRN02C | `POSTTRAN.jcl` | `PostTransactionJob` |
| CBACT04C | `INTCALC.jcl` | `InterestCalculationJob` |
| CBTRN01C | — | `TransactionValidationJob` |
| CBTRN03C | — | `StatementGenerationJob` |
| CBACT01C–03C | — | `AccountProcessingJobs` (account/card/xref read jobs) |
| CBCUS01C | — | `CustomerFileProcessJob` |

Each batch job should use the chunk-oriented model: `ItemReader` (FlatFileItemReader for the `app/data/ASCII/` inputs, or `JpaPagingItemReader` for DB reads) → `ItemProcessor` (the per-record business logic from the COBOL `PERFORM` loop) → `ItemWriter` (JPA save or flat-file output).

---

## 6. COBOL → Java Translation Rules

Apply these mechanically; preserve paragraph names as method names (camelCase) for traceability.

| COBOL construct | Java translation |
|---|---|
| `88-level` condition | `enum` (or boolean predicate on an enum); e.g. `CDEMO-USRTYP-ADMIN/USER` → `enum UserType { ADMIN, USER }` |
| `REDEFINES` | one canonical field + conversion/factory method(s) on the record (see §3.5) |
| `PIC S9(10)V99` (and any `V99` money) | `BigDecimal` with `setScale(2)`; never `double`/`float` |
| `PIC X(n)` | `String` (trim trailing spaces on read; pad/validate length on write) |
| `PIC 9(n)` | `long` (n large or an identifier) or `int` (small counters) |
| `EXEC CICS READ FILE` | `JpaRepository.findById(...)` |
| `EXEC CICS WRITE FILE` | `JpaRepository.save(...)` |
| `EXEC CICS REWRITE FILE` | `JpaRepository.save(...)` on a managed entity |
| `EXEC CICS DELETE FILE` | `JpaRepository.deleteById(...)` |
| `EXEC CICS STARTBR / READNEXT` (browse) | paged/sorted query (`Pageable`, `findAllBy...`) |
| `EXEC CICS SEND MAP` | build and return a REST response (JSON DTO) |
| `EXEC CICS RECEIVE MAP` | bind the REST request body / params to a request DTO |
| `EXEC CICS RETURN TRANSID(...)` | nothing server-side — REST is stateless; routing is client-side |
| `EXEC CICS XCTL / LINK PROGRAM` | a service method call (inject and invoke the target service) |
| `EVALUATE TRUE ... WHEN` | `switch` expression with pattern matching / guarded patterns |
| `PERFORM para THRU para-EXIT` | extract to a method and call it |
| `PERFORM ... UNTIL` (record loop) | Spring Batch chunk step, or a Java loop/stream |
| `COPY copybook` | `import` of the shared record/entity/DTO |
| `MOVE ... TO ...` | assignment / DTO mapping |
| `COMPUTE` on money | `BigDecimal` arithmetic with explicit `RoundingMode` |
| `CSUTLDTC` / `COBDATFT.asm` date logic | `java.time` (`LocalDate`, `DateTimeFormatter`); validation throws on bad dates |
| `ABEND` / error path | throw a domain exception mapped to an HTTP status via `@ControllerAdvice` |

**Decimal rule (critical):** every monetary field is `BigDecimal` scale 2. All arithmetic uses `BigDecimal` with an explicit `RoundingMode` (default `HALF_UP` unless the COBOL `ROUNDED`/truncation behavior dictates otherwise). Reconcile results against the legacy outputs.

---

## 7. API Endpoint Specification

Base path `/api`. All endpoints require a valid JWT **except** `POST /api/auth/login`.

| Method | Path | Source program(s) | Notes |
|---|---|---|---|
| POST | `/api/auth/login` | COSGN00C | returns JWT; public |
| GET | `/api/menu` | COMEN01C / COMEN02C | menu options for the authenticated user's type |
| GET | `/api/accounts/{id}` | COACTVWC | account view |
| PUT | `/api/accounts/{id}` | COACTUPC | account update |
| GET | `/api/transactions?cardNum=&page=&size=` | COTRN00C | paged list |
| GET | `/api/transactions/{id}` | COTRN01C | transaction detail |
| POST | `/api/transactions` | COTRN02C | create transaction |
| GET | `/api/cards?acctId=` | COCRDLIC | list cards for account |
| GET | `/api/cards/search?...` | COCRDSLC | search/detail |
| PUT | `/api/cards/{cardNum}` | COCRDUPC | update card |
| GET | `/api/users` | COUSR00C | list (admin only) |
| POST | `/api/users` | COUSR01C | create (admin only) |
| GET | `/api/users/{id}` | COUSR00C/02C | read (admin only) |
| PUT | `/api/users/{id}` | COUSR02C | update (admin only) |
| DELETE | `/api/users/{id}` | COUSR03C | delete (admin only) |
| POST | `/api/billpay` | COBIL00C | bill payment |
| GET | `/api/reports/transactions` | CORPT00C / CBTRN03C | transaction report |
| GET | `/api/reports/accounts` | CORPT00C / CBACT01C | account report |

Return appropriate HTTP statuses (`200`, `201`, `400` validation, `401` unauthenticated, `403` forbidden, `404` not found, `409` conflict). Use `@Valid` request DTOs reproducing the BMS/COBOL field validations.

---

## 8. Security Requirements

- **Password hashing:** replace plaintext `SEC-USR-PWD PIC X(08)` with **BCrypt**. When seeding users from `app/data/ASCII/` (the user records), hash the seed passwords during migration; never store plaintext.
- **JWT:** on successful `POST /api/auth/login`, issue a signed JWT whose claims carry `userId` and `userType` (`ADMIN`/`USER`). This **replaces COMMAREA session state** (`CDEMO-USER-ID`, `CDEMO-USER-TYPE`). Tokens are bearer tokens in the `Authorization` header; the API is stateless (`SessionCreationPolicy.STATELESS`).
- **Roles:** map `SEC-USR-TYPE` → authority. `'A'` → `ROLE_ADMIN`, `'U'` → `ROLE_USER`.
  - All `/api/users/**` endpoints: **`ROLE_ADMIN` only**.
  - Every other authenticated endpoint: `ROLE_USER` (admins also allowed).
- **Public endpoint:** only `POST /api/auth/login`. Everything else requires authentication.
- Implement a `OncePerRequestFilter` JWT filter, a `UserDetailsService` backed by the user repository, and a `SecurityFilterChain` bean. Do not log secrets or tokens.

---

## 9. Database Schema Requirements

Create Flyway migrations that map each copybook layout to a table. Money fields are `NUMERIC(12,2)` (room for `S9(10)V99`). Suggested baseline (`V1__init.sql`); adjust names/types to match the copybooks exactly and add indexes matching the VSAM keys and alternate indexes:

```sql
-- Customers (CVCUS01Y)
CREATE TABLE customer (
    cust_id        BIGINT PRIMARY KEY,
    first_name     VARCHAR(25),
    middle_name    VARCHAR(25),
    last_name      VARCHAR(25)
    -- add remaining CVCUS01Y fields (address, phone, ssn, fico, ...)
);

-- Accounts (CVACT01Y)
CREATE TABLE account (
    acct_id              BIGINT PRIMARY KEY,           -- ACCT-ID 9(11)
    active_status        CHAR(1)        NOT NULL,      -- ACCT-ACTIVE-STATUS
    curr_bal             NUMERIC(12,2)  NOT NULL,      -- ACCT-CURR-BAL S9(10)V99
    credit_limit         NUMERIC(12,2)  NOT NULL,
    cash_credit_limit    NUMERIC(12,2)  NOT NULL,
    open_date            DATE,
    expiration_date      DATE,                         -- ACCT-EXPIRAION-DATE
    reissue_date         DATE,
    curr_cyc_credit      NUMERIC(12,2)  NOT NULL,
    curr_cyc_debit       NUMERIC(12,2)  NOT NULL,
    addr_zip             VARCHAR(10),
    group_id             VARCHAR(10)
);

-- Cards (CVACT02Y / CVCRD01Y)
CREATE TABLE card (
    card_num   VARCHAR(16) PRIMARY KEY,    -- CC-CARD-NUM / CARD-NUM
    acct_id    BIGINT NOT NULL REFERENCES account(acct_id),
    cust_id    BIGINT REFERENCES customer(cust_id),
    -- add card status, name on card, expiry, cvv per copybook
    CONSTRAINT card_acct_fk FOREIGN KEY (acct_id) REFERENCES account(acct_id)
);
CREATE INDEX idx_card_acct ON card(acct_id);   -- VSAM AIX: card by account

-- Card cross-reference (CVACT03Y) — card <-> account <-> customer
CREATE TABLE card_xref (
    card_num  VARCHAR(16) PRIMARY KEY REFERENCES card(card_num),
    acct_id   BIGINT NOT NULL REFERENCES account(acct_id),
    cust_id   BIGINT NOT NULL REFERENCES customer(cust_id)
);

-- Transactions (CVTRA05Y)
CREATE TABLE transaction (
    tran_id        VARCHAR(16) PRIMARY KEY,
    type_cd        CHAR(2)        NOT NULL,
    cat_cd         INTEGER        NOT NULL,
    source         VARCHAR(10),
    description    VARCHAR(100),
    amt            NUMERIC(11,2)  NOT NULL,   -- TRAN-AMT S9(09)V99
    merchant_id    BIGINT,
    merchant_name  VARCHAR(50),
    merchant_city  VARCHAR(50),
    merchant_zip   VARCHAR(10),
    card_num       VARCHAR(16)    NOT NULL REFERENCES card(card_num),
    orig_ts        TIMESTAMP,
    proc_ts        TIMESTAMP
);
CREATE INDEX idx_tran_card ON transaction(card_num);

-- Security users (CSUSR01Y) — password stored as BCrypt hash
CREATE TABLE sec_user (
    usr_id      VARCHAR(8) PRIMARY KEY,
    first_name  VARCHAR(20),
    last_name   VARCHAR(20),
    pwd_hash    VARCHAR(72) NOT NULL,    -- BCrypt, NOT plaintext
    usr_type    CHAR(1)     NOT NULL     -- 'A' admin, 'U' user
);

-- Reference tables: transaction type (CVTRA03Y), category (CVTRA04Y),
-- category balance (CVTRA01Y), disclosure group (CVTRA02Y) ...
```
Add a separate Flyway migration (`V2__seed.sql` or a Spring Batch/CommandLineRunner loader) that imports `app/data/ASCII/*` into these tables. Parse each line by the fixed positions defined in the copybooks.

---

## 10. Testing Requirements

- **Unit tests (JUnit 5):** one test class per service. Cover every business rule extracted from the COBOL (validation, financial calculation, routing/authorization). Assert exact `BigDecimal` values for money.
- **Integration tests (Testcontainers + PostgreSQL):** spin up a real Postgres, run Flyway, exercise controllers end-to-end (`@SpringBootTest` + `MockMvc`/`WebTestClient`). Cover auth (login → JWT → protected call), CRUD, and admin-only authorization (403 for non-admin).
- **Equivalence tests:** seed from `app/data/ASCII/` and verify migrated batch jobs (`PostTransactionJob`, `InterestCalculationJob`) produce the same account balances / interest figures as the COBOL logic. Compare values, not byte layout.
- **Batch tests:** use `JobLauncherTestUtils` to launch jobs and assert step completion + output state.

---

## 11. Java 21 Features to Use

- **Records** for all DTOs (request/response) and immutable value objects (e.g. `CardKey` in §3.5).
- **Sealed interfaces** for closed type hierarchies (e.g. menu option types, transaction outcome results, report result variants).
- **Pattern matching in `switch`** to translate `EVALUATE TRUE` blocks (guarded patterns instead of nested `IF`).
- **Virtual threads:** `spring.threads.virtual.enabled=true` for high-concurrency request handling.
- **Structured concurrency** (`StructuredTaskScope`) for parallel independent lookups (e.g. fetching account + card + customer for a combined view).
- **Scoped values** (`ScopedValue`) for propagating request context (authenticated user id / type) instead of thread-locals.

---

## Execution Checklist

1. Scaffold `carddemo-java/` (Maven, Spring Boot 3.2+, Java 21, dependencies in §4).
2. Define the OpenAPI spec and the package structure (§4).
3. Create entities + repositories + Flyway schema (§3, §9), then the seed loader from `app/data/ASCII/` (§2.8).
4. Implement security (JWT, BCrypt, roles) (§8).
5. Port online programs to controllers + services using the mapping (§5.1) and translation rules (§6); expose the endpoints in §7.
6. Port batch programs to Spring Batch jobs (§5.2), reading JCL (`POSTTRAN.jcl`, `INTCALC.jcl`) for sequencing.
7. Write unit + integration + equivalence tests (§10).
8. Run `mvn -q -DskipTests package` to verify the build, then `mvn test`. Fix until green.
9. (Phase 2) Address the IMS/DB2/MQ extension modules (§2.9).

**Read the source before you translate each unit.** This document tells you *what* to produce and *how* to map constructs; the authoritative business logic lives in the COBOL under `app/`. When in doubt, the copybook layouts and the program source win.
