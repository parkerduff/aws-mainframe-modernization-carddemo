# CardDemo Modernization Roadmap

> **Status:** Assessment & planning document. This is an analysis of modernization
> opportunities for the CardDemo mainframe application. It contains **no code changes** and
> implements **no migrations** — it is intended to inform a phased modernization program.
>
> **Scope analyzed:** the full repository — core COBOL/CICS/VSAM/JCL application (`app/cbl`,
> `app/cpy`, `app/bms`, `app/jcl`, …) plus the three optional modules
> (`app-authorization-ims-db2-mq`, `app-transaction-type-db2`, `app-vsam-mq`).

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Repository Inventory](#2-repository-inventory)
3. [Application Architecture (Current State)](#3-application-architecture-current-state)
4. [Data Layer Analysis](#4-data-layer-analysis)
5. [Dependency & Interaction Map](#5-dependency--interaction-map)
6. [Modernization Opportunities (Prioritized)](#6-modernization-opportunities-prioritized)
7. [Phased Modernization Plan](#7-phased-modernization-plan)
8. [Risk Register & Cross-Cutting Concerns](#8-risk-register--cross-cutting-concerns)
9. [Appendix A: Per-Program Conversion Catalog](#appendix-a-per-program-conversion-catalog)

---

## 1. Executive Summary

CardDemo is a credit-card management system that exercises the major mainframe paradigms:
**COBOL** business logic, **CICS** online transactions, **VSAM KSDS** storage, **JCL/batch**
processing, **BMS** 3270 screens, plus optional **DB2**, **IMS DB**, and **MQ** integrations.

| Metric | Count |
|---|---|
| COBOL programs (`.cbl`/`.CBL`) — total | **44** |
| &nbsp;&nbsp;• Online CICS programs (core) | 17 |
| &nbsp;&nbsp;• Batch programs (core) | 12 |
| &nbsp;&nbsp;• Utilities / subroutines (core) | 2 (`CSUTLDTC`, `COBSWAIT`) |
| &nbsp;&nbsp;• Optional-module programs | 13 (IMS/DB2/MQ, DB2 tran-type, VSAM-MQ) |
| Copybooks (`.cpy`/`.CPY`, excl. BMS symbolic maps) | 30 core + module copybooks |
| BMS map sources (`.bms`) | 21 (17 core + 4 module) |
| JCL jobs (`.jcl`/`.JCL`) | 47 (core) + module JCL |
| Assembler programs (`.asm`) | 2 (`COBDATFT`, `MVSWAIT`) |
| DB2 DDL tables | 3 (`TRANSACTION_TYPE`, `TRAN_CAT_TYPE`, `AUTHFRDS`) |
| IMS DBDs | `DBPAUTP0`, plus GSAM DBs (`PADFLDBD`, `PASFLDBD`) |

**Largest / highest-complexity programs** (lines of code): `COACTUPC` (4,236),
`COTRTLIC` (2,098, DB2 module), `COTRTUPC` (1,702, DB2 module), `COCRDUPC` (1,560),
`COCRDLIC` (1,459), `COPAUS0C` (1,032, IMS module), `COPAUA0C` (1,026, MQ module),
`COACTVWC` (941), `CBSTM03A` (924). These are the long-pole conversion targets.

**Headline recommendation.** Modernize in the order **data → service/API → UI → batch →
decommission**, using the **Strangler Fig** pattern so the legacy and modern systems run in
parallel during transition. Begin with read-only API exposure and a VSAM→PostgreSQL data
migration (lowest risk, unblocks everything else), then convert the transactional COBOL
programs to a JVM/cloud-native service tier, replace BMS screens with a web UI, and finally
re-platform batch onto Spring Batch / AWS Step Functions and retire CICS/VSAM.

---

## 2. Repository Inventory

### 2.1 Top-level layout

| Path | Contents |
|---|---|
| `app/cbl` | 31 core COBOL programs (online + batch + utilities) |
| `app/cpy` | 30 copybooks (record layouts, COMMAREA, constants, utilities) |
| `app/bms` | 17 BMS map sources (3270 screens) |
| `app/cpy-bms` | 17 generated BMS symbolic-map copybooks |
| `app/jcl` | 38 JCL jobs (load/refresh/post/report/define) |
| `app/proc` | 2 procs (`REPROC`, `TRANREPT`) |
| `app/csd` | `CARDDEMO.CSD` — CICS resource definitions |
| `app/ctl` | IDCAMS/utility control cards |
| `app/asm` | `COBDATFT` (date conversion), `MVSWAIT` (timer) |
| `app/maclib` | Assembler macros (`ASMWAIT`, `COCDATFT`) |
| `app/data` | Sample data (`ASCII`, `EBCDIC`) |
| `app/catlg` | `LISTCAT.txt` |
| `app/scheduler` | `CardDemo.ca7`, `CardDemo.controlm` — scheduler definitions |
| `app/app-authorization-ims-db2-mq` | Optional module: pending authorizations (IMS + DB2 + MQ) |
| `app/app-transaction-type-db2` | Optional module: transaction-type management (DB2) |
| `app/app-vsam-mq` | Optional module: account/date inquiry over MQ |
| `samples/` | Compile JCL/PROCs + AWS M2 & UniKix runtime zips |
| `scripts/` | Shell helpers (`run_full_batch.sh`, `local_compile.sh`, `remote_*`, …) |
| `diagrams/` | Architecture / flow diagrams, `CARDDEMO-DataModel.drawio` |

### 2.2 Program classification (core `app/cbl`)

**Online (CICS) — 17 programs.** Each owns one BMS mapset, receives/sends 3270 maps, and
navigates via `EXEC CICS XCTL`/`RETURN` using the shared COMMAREA (`COCOM01Y`).

| Program | LOC | CICS cmds | Transaction | Function |
|---|---:|---:|---|---|
| `COSGN00C` | 260 | 10 | CC00 | Sign-on / authentication |
| `COMEN01C` | 308 | 7 | CM00 | Main (user) menu |
| `COADM01C` | 288 | 7 | CA00 | Admin menu |
| `COACTVWC` | 941 | 15 | CAVW | Account view |
| `COACTUPC` | 4,236 | 17 | CAUP | Account update (most complex program) |
| `COCRDLIC` | 1,459 | 18 | CCLI | Credit-card list (browse/paginate) |
| `COCRDSLC` | 887 | 14 | CCDL | Credit-card view |
| `COCRDUPC` | 1,560 | 12 | CCUP | Credit-card update |
| `COTRN00C` | 699 | 10 | CT00 | Transaction list |
| `COTRN01C` | 330 | 5 | CT01 | Transaction view |
| `COTRN02C` | 783 | 11 | CT02 | Transaction add |
| `CORPT00C` | 649 | 7 | CR00 | Transaction reports (submits batch) |
| `COBIL00C` | 572 | 13 | CB00 | Bill payment |
| `COUSR00C` | 695 | 11 | CU00 | List users |
| `COUSR01C` | 299 | 5 | CU01 | Add user |
| `COUSR02C` | 414 | 6 | CU02 | Update user |
| `COUSR03C` | 359 | 6 | CU03 | Delete user |

**Batch — 12 programs.**

| Program | LOC | Job | Function |
|---|---:|---|---|
| `CBACT01C` | 430 | (print) | Read & print account master |
| `CBACT02C` | 178 | (print) | Read & print card master |
| `CBACT03C` | 178 | (print) | Read & print card xref |
| `CBACT04C` | 652 | INTCALC | Interest calculation & accrual |
| `CBCUS01C` | 178 | (print) | Read & print customer master |
| `CBTRN01C` | 494 | (validate) | Read daily-transaction file, validate against masters |
| `CBTRN02C` | 731 | POSTTRAN | **Core posting engine** — post daily transactions |
| `CBTRN03C` | 649 | TRANREPT | Transaction detail report |
| `CBSTM03A` | 924 | CREASTMT | Statement generation (driver, plain text + HTML) |
| `CBSTM03B` | 230 | CREASTMT | Statement I/O subroutine |
| `CBEXPORT` | 582 | CBEXPORT | Export all master files to a single export file |
| `CBIMPORT` | 487 | CBIMPORT | Import combined export back into master files |

**Utilities / subroutines.** `CSUTLDTC` (157, date validation via `CEEDAYS`),
`COBSWAIT` (41, CICS delay), and Assembler `COBDATFT` (date format) and `MVSWAIT` (batch timer).

### 2.3 Optional modules

| Module | Programs | Tech | Purpose |
|---|---|---|---|
| `app-authorization-ims-db2-mq` | `COPAUA0C` (MQ trigger), `COPAUS0C` (summary), `COPAUS1C` (detail), `COPAUS2C` (DB2 insert), `CBPAUP0C` (purge), `PAUDBLOD`/`PAUDBUNL`/`DBUNLDGS` (IMS load/unload) | IMS DB + DB2 + MQ + CICS | Credit-card pending-authorization processing & fraud log |
| `app-transaction-type-db2` | `COTRTLIC` (list/delete, cursors), `COTRTUPC` (add/update), `COBTUPDT` (batch update) | DB2 + CICS | Transaction-type/category reference-data management |
| `app-vsam-mq` | `COACCT01` (account inquiry), `CODATE01` (system-date inquiry) | MQ + VSAM | Async request/response inquiry services |

---

## 3. Application Architecture (Current State)

### 3.1 Online navigation (CICS XCTL flow)

```
                         ┌──────────┐
            CC00  ─────▶ │ COSGN00C │  (sign-on; reads USRSEC)
                         └────┬─────┘
              admin? ┌────────┴────────┐ user?
                     ▼                 ▼
                ┌──────────┐      ┌──────────┐
                │ COADM01C │      │ COMEN01C │   (menu tables: COADM02Y / COMEN02Y)
                └────┬─────┘      └────┬─────┘
                     │                 │  numbered options drive XCTL to:
        ┌────────────┘     ┌───────────┼─────────────────────────────┐
        ▼                  ▼           ▼            ▼          ▼       ▼
   COUSR00C/01C/02C/03C  COACTVWC  COACTUPC   COCRDLIC/SLC/UPC  COTRN00C/01C/02C
   (user admin)          (acct)    (acct upd)  (cards)          (transactions)
                                                       CORPT00C (reports)  COBIL00C (bill pay)
```

Navigation is **table-driven**: `COMEN01C`/`COADM01C` read a menu-options table
(`COMEN02Y`/`COADM02Y` — option number, label, target program, allowed user type) and `XCTL`
to the selected program. Every program carries the shared **COMMAREA** (`COCOM01Y`,
`CARDDEMO-COMMAREA`) holding user id, user type, from/to-program, and program context. This
is effectively a server-side session object.

### 3.2 Common patterns (shared across nearly all online programs)

- **Screen lifecycle:** `RECEIVE MAP` → `PROCESS-ENTER-KEY` (field validation) → `READ`/
  `REWRITE`/`WRITE` VSAM → `SEND MAP`. Function keys map to navigation (PF3 = back).
- **Error-flag pattern:** `WS-ERR-FLG` (level-88 `ERR-FLG-ON/OFF`) + a message field; each
  validation step sets the flag, positions the cursor (`MOVE -1 TO …L`), and re-sends the map.
- **Constants/literals block:** DD/file names and transaction ids are defined as
  `LIT-*` working-storage constants (e.g. `LIT-ACCTFILENAME VALUE 'ACCTDAT'`).
- **Date utilities:** `CSUTLDTC` (Language-Environment `CEEDAYS`) and Assembler `COBDATFT`.

### 3.3 Batch processing pipeline

The end-to-end nightly cycle (from the README "Running Batch Jobs" sequence):

```
CLOSEFIL (quiesce CICS files)
   └▶ ACCTFILE / CARDFILE / XREFFILE / CUSTFILE  (IDCAMS REPRO: refresh masters from .PS)
        └▶ TRANBKP            (backup/refresh transaction master)
             └▶ TRANCATG / TRANTYPE / DISCGRP / TCATBALF  (load reference VSAM)
                  └▶ DUSRSECJ (load user-security VSAM)
                       └▶ POSTTRAN  (CBTRN02C: post DALYTRAN → TRANSACT, update ACCT + TCATBAL)
                            └▶ INTCALC   (CBACT04C: interest accrual using DISCGRP rates)
                                 └▶ TRANBKP / COMBTRAN (SORT+merge system & daily trans)
                                      └▶ CREASTMT (CBSTM03A/B: statements, text + HTML)
                                           └▶ TRANIDX (define AIX on transaction file)
                                                └▶ OPENFIL (re-enable CICS files)
```

`POSTTRAN` is the heart of the system: `CBTRN02C` reads `DALYTRAN` sequentially, validates
each record (`1500-VALIDATE-TRAN`: account & xref exist, within credit limit), then
`2000-POST-TRANSACTION` writes to the transaction KSDS, updates the account balance/cycle
amounts, and updates the transaction-category balance — rejects go to `DALYREJS`. This is a
classic **multi-file atomic update** that becomes a single DB transaction in the target.

### 3.4 Scheduling

Scheduler definitions exist for **CA-7** (`CardDemo.ca7`) and **Control-M**
(`CardDemo.controlm`). These encode the job dependencies above and are the source of truth
for ordering when re-implementing the pipeline on a modern orchestrator.

---

## 4. Data Layer Analysis

### 4.1 VSAM files → target relational model

All primary stores are **VSAM KSDS** (key-sequenced). Two **alternate indexes (AIX)** exist.
Record layouts come from the `CV*`/`CS*` copybooks (record lengths in parentheses).

| VSAM file (CICS DD) | Copybook | RECLN | Primary key | Alt index | Target table | Notes |
|---|---|---:|---|---|---|---|
| `ACCTDAT` | `CVACT01Y` (`ACCOUNT-RECORD`) | 300 | `ACCT-ID` `9(11)` | — | `account` | balances are `S9(10)V99` → `NUMERIC(12,2)` |
| `CARDDAT` | `CVACT02Y` (`CARD-RECORD`) | 150 | `CARD-NUM` `X(16)` | `CARDAIX` on `CARD-ACCT-ID` | `card` | FK `acct_id` → `account` |
| `CUSTDAT` | `CVCUS01Y` (`CUSTOMER-RECORD`) | 500 | `CUST-ID` `9(09)` | — | `customer` | PII: SSN, DOB, FICO, govt id |
| `CARDXREF` | `CVACT03Y` (`CARD-XREF-RECORD`) | 50 | `XREF-CARD-NUM` `X(16)` | `CXACAIX` on `XREF-ACCT-ID` | `card_xref` (or FKs) | card↔acct↔cust mapping |
| `TRANSACT` | `CVTRA05Y` (`TRAN-RECORD`) | 350 | `TRAN-ID` `X(16)` | (TRANIDX defines AIX) | `transaction` | amount `S9(09)V99` |
| `DALYTRAN` | `CVTRA06Y` (`DALYTRAN-RECORD`) | 350 | (sequential) | — | `daily_transaction` (staging) | input to posting |
| `TCATBALF` | `CVTRA01Y` (`TRAN-CAT-BAL-RECORD`) | 50 | acct+type+cat (composite) | — | `tran_category_balance` | composite PK |
| `DISCGRP` | `CVTRA02Y` (`DIS-GROUP-RECORD`) | 50 | group+type+cat (composite) | — | `disclosure_group` | holds `DIS-INT-RATE` |
| `TRANTYPE` | `CVTRA03Y` (`TRAN-TYPE-RECORD`) | 60 | `TRAN-TYPE` `X(02)` | — | `transaction_type` | also a DB2 table (module) |
| `TRANCATG` | `CVTRA04Y` (`TRAN-CAT-RECORD`) | 60 | type+cat (composite) | — | `transaction_category` | also a DB2 table (module) |
| `USRSEC` | `CSUSR01Y` (`SEC-USER-DATA`) | 80 | `SEC-USR-ID` `X(08)` | — | `app_user` → IAM/Cognito | stores plaintext password |

**Type-mapping guidance** (per the data-layer patterns): `PIC 9(n)`→`NUMERIC(n,0)`;
`PIC S9(n)V99`→`NUMERIC(n+2,2)` (never floating point for money); `PIC X(n)`→
`VARCHAR(n)`/`CHAR(n)`; date `PIC X(10)`→`DATE`; timestamp `PIC X(26)`→`TIMESTAMP`. Drop
`FILLER` padding fields in the new schema (keep them only in a byte-exact compatibility
parser used during migration).

### 4.2 Referential model (to be enforced in the RDBMS)

```
customer (CUST-ID) ─1───∞─ card_xref (XREF-CUST-ID, XREF-ACCT-ID, XREF-CARD-NUM)
                                  │                    │
account (ACCT-ID) ─1──────∞──────┘                    └──∞─ card (CARD-NUM, CARD-ACCT-ID)
   │
   ├─1───∞─ transaction (TRAN-ID; TRAN-CARD-NUM, TRAN-TYPE-CD, TRAN-CAT-CD)
   ├─1───∞─ tran_category_balance (acct + type + cat)
   └──────  disclosure_group (group + type + cat) provides interest rate by account group

transaction_type (TR_TYPE) ─1──∞─ transaction_category (type + cat)
```

The XREF file is the join hub (card ↔ account ↔ customer); in the relational target this
becomes foreign keys (`card.acct_id`, plus a `card_xref`/customer link), eliminating the
manual cross-reference reads found throughout the online programs.

### 4.3 DB2 and IMS (optional modules)

- **DB2** (`app-transaction-type-db2/ddl`): `CARDDEMO.TRANSACTION_TYPE`
  (`TR_TYPE` PK, `TR_DESCRIPTION`) and `CARDDEMO.TRAN_CAT_TYPE`. Accessed via embedded SQL
  with cursors (`COTRTLIC`) — already relational, so this is the **easiest** data component
  to lift into PostgreSQL/Aurora.
- **DB2** (`app-authorization-ims-db2-mq/ddl`): `CARDDEMO.AUTHFRDS` — fraud/authorization log
  (`CARD_NUM`+`AUTH_TS` composite PK, decimal amounts, merchant fields).
- **IMS DB** (`app-authorization-ims-db2-mq/ims`): `DBPAUTP0` pending-authorization hierarchical
  DB, plus GSAM databases (`PADFLDBD`, `PASFLDBD`) for sequential load/unload. IMS segments map
  to parent/child relational tables (or a document model); this is the **highest-effort** data
  migration because of the hierarchical access paths and DL/I call logic in `COPAUS0C`/`CBPAUP0C`.

### 4.4 Data migration mechanics

1. **Extract:** IDCAMS `REPRO` each VSAM KSDS to a sequential file (the repo's
   `ACCTFILE`/`CARDFILE`/… jobs and the `app/data/EBCDIC` samples already model this).
2. **Parse:** byte-offset parsing using the copybook field positions; **unpack COMP-3** packed
   decimals (two digits/byte, trailing sign nibble) and handle EBCDIC→ASCII.
3. **Transform:** trim padding, convert dates, enforce types from the table above.
4. **Load:** bulk `COPY`/`LOAD` into PostgreSQL/Aurora.
5. **Validate:** row counts + checksums + spot-check business-critical records (balances).

---

## 5. Dependency & Interaction Map

### 5.1 Program → copybook usage (shared structures)

| Copybook | Role | Used by (representative) |
|---|---|---|
| `COCOM01Y` | `CARDDEMO-COMMAREA` (session/context) | **all** online programs |
| `COTTL01Y`, `CSMSG01Y`, `CSMSG02Y` | titles / message constants | most online programs |
| `CSDAT01Y`, `CODATECN`, `CSUTLDPY`, `CSUTLDWY` | date structures / utilities | account/card/date programs, `CSUTLDTC` |
| `COMEN02Y` / `COADM02Y` | menu option tables | `COMEN01C` / `COADM01C` |
| `CSUSR01Y` | user-security record | `COSGN00C`, `COUSR0x`, `COADM01C`, `COMEN01C` |
| `CVACT01Y` (account) | account record | `COACTVWC`, `COACTUPC`, `COBIL00C`, `CBACT01C`, `CBACT04C`, `CBTRN01C/02C`, `CBSTM03*` |
| `CVACT02Y` (card) | card record | `COCRDLIC/SLC/UPC`, `COACTVWC/UPC`, `CBACT02C`, `CBTRN01C` |
| `CVACT03Y` (xref) | card xref | `COACTVWC/UPC`, `COBIL00C`, `COTRN02C`, `CBACT03C`, `CBTRN01C/02C/03C`, `CBSTM03*` |
| `CVCUS01Y` (customer) | customer record | `COACTVWC/UPC`, `CBCUS01C`, `CBSTM03*` |
| `CVTRA05Y`/`CVTRA06Y` (tran/daily) | transaction records | `COTRN00C/01C/02C`, `CORPT00C`, `COBIL00C`, `CBTRN01C/02C/03C` |
| `CVTRA01Y`–`CVTRA04Y` (cat-bal, disc-grp, type, cat) | reference/balance | `CBACT04C`, `CBTRN02C/03C`, tran-type module |
| `CSLKPCDY` (1,318 lines) | large lookup/reference-code table | programs needing code lookups |

Each BMS screen also has a generated symbolic-map copybook in `app/cpy-bms`
(`COxxx.CPY`) paired 1:1 with its `app/bms/COxxx.bms` source.

### 5.2 Program → VSAM file access (read/update)

**Online:**

| File | Programs that access it |
|---|---|
| `USRSEC` | `COSGN00C`, `COMEN01C`, `COADM01C`, `COUSR00C/01C/02C/03C` |
| `ACCTDAT` | `COACTVWC`, `COACTUPC`, `COBIL00C`, `COTRN02C` |
| `CARDDAT` / `CARDAIX` | `COCRDLIC`, `COCRDSLC`, `COCRDUPC`, `COACTVWC`, `COACTUPC` |
| `CUSTDAT` | `COACTVWC`, `COACTUPC` |
| `CARDXREF` (`CXACAIX`/`CCXREF`) | `COACTVWC`, `COACTUPC`, `COBIL00C`, `COTRN02C` |
| `TRANSACT` | `COTRN00C`, `COTRN01C`, `COTRN02C`, `CORPT00C`, `COBIL00C` |

**Batch:**

| Program | Reads | Writes/Updates |
|---|---|---|
| `CBACT01C/02C/03C/CBCUS01C` | ACCT / CARD / XREF / CUST | print report |
| `CBTRN01C` | DALYTRAN, CUST, XREF, CARD, ACCT | TRANFILE (validated) |
| `CBTRN02C` (POSTTRAN) | DALYTRAN, XREF, ACCT, TCATBAL | TRANSACT, ACCT, TCATBAL, DALYREJS |
| `CBACT04C` (INTCALC) | TCATBAL, XREF, ACCT, DISCGRP | TRANSACT, ACCT |
| `CBTRN03C` (TRANREPT) | TRANFILE, CARDXREF, TRANTYPE, TRANCATG, DATEPARM | TRANREPT (report) |
| `CBSTM03A/B` (CREASTMT) | TRNX, XREF, CUST, ACCT | STMT (text), HTML |
| `CBEXPORT` / `CBIMPORT` | all masters / export file | export file / all masters |

### 5.3 Batch job execution order (must run in sequence)

`CLOSEFIL` → master refresh (`ACCTFILE`,`CARDFILE`,`XREFFILE`,`CUSTFILE`) → `TRANBKP` →
reference loads (`TRANCATG`,`TRANTYPE`,`DISCGRP`,`TCATBALF`) → `DUSRSECJ` → **`POSTTRAN`** →
**`INTCALC`** → `TRANBKP`/`COMBTRAN` → `CREASTMT` → `TRANIDX` → `OPENFIL`
(+ optional `CBPAUP0J` to purge expired authorizations). This dependency chain is the
specification for the modern workflow DAG (Step Functions / Airflow / Spring Batch jobs).

---

## 6. Modernization Opportunities (Prioritized)

Each opportunity is rated **Effort / Risk / Business value** (low/med/high) and a suggested
**priority P0–P3** (P0 = do first / foundational, P3 = last / optional).

### 6.1 Language / Runtime modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Convert **batch utilities & print programs** (`CBACT01C/02C/03C`, `CBCUS01C`) to Java/Python — simple OPEN→READ→PRINT loops, no CICS | Low | Low | Med | **P1** |
| Convert **core posting/interest** (`CBTRN01C`, `CBTRN02C`, `CBACT04C`) to a service + Spring Batch — financial logic, needs equivalence testing | High | High | High | **P2** |
| Convert **online CRUD programs** (`COACTVWC`, `COCRDLIC/SLC/UPC`, `COTRN00C/01C/02C`, `COBIL00C`, `COUSR0x`) to REST services | Med–High | Med | High | **P2–P3** |
| Convert **`COACTUPC`** (4,236 LOC, deeply nested validation/update) — manual, decompose first | High | High | High | **P3** |
| Replace Assembler `COBDATFT`/`MVSWAIT` and `CSUTLDTC` with library date utils / scheduler waits | Low | Low | Low | **P1** |
| Retire `CBEXPORT`/`CBIMPORT` once data lives in the RDBMS (native dump/restore replaces them) | Low | Low | Low | **P3** |

**Automated vs. manual conversion candidates.**
- *Good for automated/tooling-assisted conversion:* the batch print programs and the smaller
  online programs (`COTRN01C`, `COUSR01C`, `COSGN00C`, `COMEN01C`, `COADM01C`) — linear control
  flow, few CICS verbs, clear copybook I/O.
- *Manual / careful refactor required:* `COACTUPC`, `COCRDUPC`, `COCRDLIC`, `COACTVWC`,
  `CBTRN02C`, `CBACT04C`, and the DB2/IMS/MQ module programs (`COTRTLIC`, `COPAUS0C`,
  `COPAUA0C`) — high LOC, nested `EVALUATE`, multi-file atomic updates, and external resource
  managers. Extract business rules to tests **before** converting.

### 6.2 Data layer modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Migrate **VSAM KSDS → PostgreSQL/Aurora** (schema from §4.1, ETL from §4.4) | High | Med | High | **P2 (foundational)** |
| Lift **DB2 tables** (`TRANSACTION_TYPE`, `TRAN_CAT_TYPE`, `AUTHFRDS`) to PostgreSQL — already relational | Low | Low | Med | **P2** |
| Replace **IMS DB** (`DBPAUTP0`, GSAM) with relational parent/child tables | High | High | Med | **P3** |
| Recreate **AIX** (`CARDAIX`, `CXACAIX`, TRANIDX) as secondary indexes / FKs | Low | Low | Med | **P2** |
| Establish **dual-write / CDC reconciliation** during transition (Strangler Fig) | Med | Med | High | **P2** |

### 6.3 UI / Presentation modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Expose business logic as **REST/GraphQL APIs** (transaction→endpoint mapping, §6 of data patterns) | Med | Low | High | **P1** |
| Replace **21 BMS 3270 maps** with a **React/Angular** web UI (field inventory from `cpy-bms`) | High | Med | High | **P3** |
| Preserve **PF-key navigation & validation** semantics in the web UI / API layer | Med | Med | Med | **P3** |

The symbolic maps in `app/cpy-bms` give an exact field inventory (name, length, attributes)
per screen — a ready-made spec for form components. Menu options (`COMEN02Y`/`COADM02Y`) map
directly to UI routes / API resources.

### 6.4 Integration modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Replace **IBM MQ** (`COPAUA0C`, `COACCT01`, `CODATE01`) with SQS/SNS/EventBridge | Med | Med | Med | **P3** |
| Decompose into **bounded-context microservices** (Accounts, Cards, Customers, Transactions, Users/Auth, Reporting, Authorizations) | High | High | High | **P3** |
| Replace COMMAREA/`XCTL` chaining with **API gateway + session/token context** | Med | Med | Med | **P2–P3** |

Natural service boundaries (from the file-access map §5.2): **Auth/User** (USRSEC),
**Account** (ACCTDAT + XREF), **Card** (CARDDAT/AIX), **Customer** (CUSTDAT),
**Transaction/Posting** (TRANSACT/DALYTRAN/TCATBAL), **Reference data** (TRANTYPE/TRANCATG/
DISCGRP), **Authorizations** (IMS/DB2/MQ module).

### 6.5 Batch modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Re-platform pipeline to **AWS Step Functions / Spring Batch / Airflow** using the §5.3 DAG | High | Med | High | **P3** |
| Convert **CA-7 / Control-M** schedules to the modern orchestrator's DAG definition | Med | Low | Med | **P3** |
| Replace IDCAMS REPRO load/refresh jobs with DB-native bulk load / seed scripts | Low | Low | Med | **P2** |
| Consider **event-driven posting** (replace nightly `POSTTRAN` with stream processing) | High | High | Med | **P3 (stretch)** |

### 6.6 Infrastructure modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Stand up **CI/CD** (build, test, lint, deploy) — none exists in the repo today | Low | Low | High | **P1** |
| **Containerize** the modern service tier (replace CICS region) | Med | Med | High | **P3** |
| **Infrastructure-as-Code** (Terraform/CDK) for DB, queues, compute, networking | Med | Low | Med | **P1–P2** |
| Use **AWS Mainframe Modernization (M2)** runtime as an interim re-host (`samples/m2` zips already present) | Med | Med | Med | **P0–P1 (optional bridge)** |

### 6.7 Security modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Replace **VSAM `USRSEC` (plaintext passwords)** with IAM/Cognito + hashed credentials | Med | Med | High | **P1 (security)** |
| Map **user types (admin/user)** to RBAC roles / JWT claims | Low | Low | High | **P1** |
| Encrypt **PII** (SSN, DOB, FICO, govt id in `CUSTDAT`; PAN in `CARDDAT`) at rest & in transit | Med | High | High | **P1** |
| Remove hard-coded demo credentials (`ADMIN001`/`USER0001`/`PASSWORD`) from any modern path | Low | Med | High | **P1** |

> ⚠️ `CSUSR01Y` stores passwords in clear text and the README documents shared demo
> credentials. This is acceptable for a demo but must be remediated first in any real
> modernization (hashing, secret storage, MFA).

### 6.8 Testing modernization

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Build **equivalence/golden-master tests** (run same inputs through legacy & modern, compare byte/value-for-value) | Med | Low | High | **P0–P1** |
| Extract **business rules** (validation, interest calc, posting, fees) as testable assertions before conversion | Med | Low | High | **P0** |
| Add **unit + integration test suites** to each converted service | Med | Low | High | **P1–P2** |
| Generate **test data** from `app/data/EBCDIC` and `.PS` samples | Low | Low | Med | **P0** |

### 6.9 Observability

| Opportunity | Effort | Risk | Value | Priority |
|---|---|---|---|---|
| Structured **logging** (replace `DISPLAY`/abend dumps) | Low | Low | High | **P1** |
| **Metrics & dashboards** (posting volume, rejects, batch duration) | Med | Low | Med | **P2** |
| **Distributed tracing** across the new service/API tier | Med | Low | Med | **P2** |
| Map **file-status / RESP / SQLCODE / ABEND** handling to alerts & error budgets | Med | Med | Med | **P2** |

---

## 7. Phased Modernization Plan

### Phase 0 — Assessment, discovery & test foundation *(P0)*
**Goal:** know exactly what exists and be able to prove equivalence.
- Complete automated discovery / dependency mapping (this document is the seed).
- Extract business rules to assertions; build golden-master test harness from sample data.
- Stand up the repo's CI skeleton; (optionally) re-host on **AWS M2** as a working baseline.
- **Components:** all programs (catalog), `app/data` samples, `scripts/run_full_batch.sh`.

### Phase 1 — Quick wins: APIs, CI/CD, security, observability *(P1)*
**Goal:** value without touching core financial logic.
- Read-only **REST APIs** in front of the existing data (account view, card list/view,
  transaction list/view) — wrap, don't rewrite.
- **CI/CD** + IaC scaffolding; structured logging.
- **Security:** replace plaintext `USRSEC`/demo creds with hashed creds + RBAC; encrypt PII.
- Convert the **low-risk batch print programs** (`CBACT01C/02C/03C`, `CBCUS01C`) and date/timer
  utilities as proof-of-concept conversions.
- **Components:** `COACTVWC`, `COCRDLIC/SLC`, `COTRN00C/01C` (read paths), `COSGN00C`/`USRSEC`,
  `CBACT01C/02C/03C`, `CBCUS01C`, `CSUTLDTC`, `COBDATFT`.

### Phase 2 — Data modernization: VSAM/DB2 → relational *(P2)*
**Goal:** make a modern database the system of record.
- Migrate VSAM KSDS → PostgreSQL/Aurora (schema §4.1, ETL §4.4); lift DB2 tables; rebuild AIX
  as indexes/FKs; replace IDCAMS load jobs with DB-native seeding.
- Implement **dual-write + CDC reconciliation** (Strangler Fig) so legacy and modern stay
  consistent during cutover.
- Add metrics/tracing to data access.
- **Components:** all `CV*` copybook entities, `CARDAIX`/`CXACAIX`/TRANIDX, DB2 module tables,
  load jobs `ACCTFILE`/`CARDFILE`/`XREFFILE`/`CUSTFILE`/`TRAN*`/`DISCGRP`/`TCATBALF`.

### Phase 3 — Application modernization: COBOL → JVM/cloud-native *(P3)*
**Goal:** retire COBOL business logic program-by-program.
- Convert transactional online programs and core batch to services + Spring Batch, **gated by
  the Phase 0 equivalence tests**. Order: simplest → most complex.
  - First: `COTRN02C`, `COUSR0x`, `COBIL00C`, `COCRDUPC`.
  - Then: `COCRDLIC`, `COACTVWC`, `CORPT00C`/`CBTRN03C`, `CBSTM03A/B`.
  - Core financial: `CBTRN01C`, **`CBTRN02C`**, **`CBACT04C`** (posting & interest).
  - Last / hardest: **`COACTUPC`** (4,236 LOC) — decompose its validation/update paragraphs
    into discrete methods first.
- Replace COMMAREA/`XCTL` chaining with API-gateway routing + token/session context.
- **Components:** all core `CO*`/`CB*` programs not handled earlier.

### Phase 4 — Full cloud-native: microservices, serverless batch, modern UI *(P3+)*
**Goal:** decommission the mainframe.
- Split into bounded-context **microservices** (§6.4); replace **MQ** with SQS/SNS/EventBridge;
  migrate **IMS** auth module to relational/event-driven.
- Re-platform the **batch DAG** (§5.3) onto Step Functions / Spring Batch; convert CA-7/
  Control-M schedules.
- Replace **21 BMS screens** with a React/Angular web UI built from the `cpy-bms` field
  inventory and menu tables.
- Decommission CICS, VSAM, `CBEXPORT`/`CBIMPORT`.
- **Components:** optional modules (`app-authorization-ims-db2-mq`, `app-vsam-mq`,
  `app-transaction-type-db2`), all BMS maps, remaining batch jobs/schedulers.

**Ordering rationale.** Data is migrated before logic (Phase 2 before 3) so converted programs
target a stable modern store; read-only APIs and security/CI come first (Phase 1) for early
value at low risk; UI and microservice decomposition come last (Phase 4) because they depend
on the service/API tier and data model being in place; the hardest, highest-risk financial
programs (`CBTRN02C`, `CBACT04C`, `COACTUPC`) are sequenced only after the equivalence-test
safety net and modern database exist.

---

## 8. Risk Register & Cross-Cutting Concerns

| Risk | Mitigation |
|---|---|
| **Financial-precision drift** (COMP-3 / fixed-point rounding) | Use `BigDecimal`/`NUMERIC`, never float; byte/value-for-value equivalence tests on balances & interest |
| **Multi-file atomicity** in `CBTRN02C` (acct + tran + cat-bal + reject) | Single DB transaction; saga/outbox if split across services |
| **Hidden business rules** in 4k-line `COACTUPC` | Extract rules to tests before conversion; decompose paragraphs to methods |
| **Plaintext credentials / unencrypted PII** | Address in Phase 1 (hash, encrypt, secret store) before any production cutover |
| **Date/century handling** (`X(10)` dates, LE `CEEDAYS`, Assembler `COBDATFT`) | Standardize on ISO dates; test boundary/leap cases |
| **Dual-run consistency** during Strangler Fig | CDC + reconciliation jobs; designate system-of-record per entity per phase |
| **Scheduler fidelity** (CA-7/Control-M dependencies) | Encode exact §5.3 DAG; verify COND/restart semantics |
| **No existing automated tests / CI** | Phase 0/1 builds the safety net first |

---

## Appendix A: Per-Program Conversion Catalog

Legend — **Type:** ON=online CICS, BA=batch, UT=utility, MOD=optional module.
**Auto?** = suitability for automated/tooling-assisted conversion (vs. manual refactor).

| Program | Type | LOC | Key tech | Target | Auto? | Phase |
|---|---|---:|---|---|---|---|
| `COSGN00C` | ON | 260 | CICS, USRSEC | Auth service / login API | Yes | 1 |
| `COMEN01C` | ON | 308 | CICS, menu table | UI routing / menu API | Yes | 1 |
| `COADM01C` | ON | 288 | CICS, menu table | Admin routing API | Yes | 1 |
| `COACTVWC` | ON | 941 | CICS, ACCT/CARD/CUST/XREF | Account read API | Partial | 1–2 |
| `COACTUPC` | ON | 4,236 | CICS, multi-file update | Account update service | Manual | 3 |
| `COCRDLIC` | ON | 1,459 | CICS browse/paginate | Card list API (pagination) | Partial | 2–3 |
| `COCRDSLC` | ON | 887 | CICS, CARD | Card view API | Yes | 1–2 |
| `COCRDUPC` | ON | 1,560 | CICS, CARD update | Card update service | Manual | 3 |
| `COTRN00C` | ON | 699 | CICS, TRANSACT browse | Transaction list API | Partial | 1–2 |
| `COTRN01C` | ON | 330 | CICS, TRANSACT read | Transaction view API | Yes | 1 |
| `COTRN02C` | ON | 783 | CICS, ACCT/XREF/TRAN write | Transaction add service | Partial | 3 |
| `CORPT00C` | ON | 649 | CICS, submits batch report | Report request API | Partial | 3 |
| `COBIL00C` | ON | 572 | CICS, bill-pay update | Bill-payment service | Manual | 3 |
| `COUSR00C` | ON | 695 | CICS, USRSEC browse | User list API | Yes | 1–2 |
| `COUSR01C` | ON | 299 | CICS, USRSEC write | Add-user API | Yes | 1 |
| `COUSR02C` | ON | 414 | CICS, USRSEC update | Update-user API | Yes | 2 |
| `COUSR03C` | ON | 359 | CICS, USRSEC delete | Delete-user API | Yes | 2 |
| `CBACT01C` | BA | 430 | Seq read ACCT | Reporting job | Yes | 1 |
| `CBACT02C` | BA | 178 | Seq read CARD | Reporting job | Yes | 1 |
| `CBACT03C` | BA | 178 | Seq read XREF | Reporting job | Yes | 1 |
| `CBCUS01C` | BA | 178 | Seq read CUST | Reporting job | Yes | 1 |
| `CBACT04C` | BA | 652 | INTCALC, DISCGRP rates | Interest service (Spring Batch) | Manual | 3 |
| `CBTRN01C` | BA | 494 | Validate daily trans | Validation step | Partial | 3 |
| `CBTRN02C` | BA | 731 | **POSTTRAN** multi-file update | Posting engine (transactional) | Manual | 3 |
| `CBTRN03C` | BA | 649 | TRANREPT report | Report service | Partial | 3 |
| `CBSTM03A` | BA | 924 | Statement driver (text+HTML) | Statement service | Manual | 3 |
| `CBSTM03B` | BA | 230 | Statement I/O subroutine | merged into statement service | Partial | 3 |
| `CBEXPORT` | BA | 582 | Export all masters | retire (DB dump) | n/a | 3 |
| `CBIMPORT` | BA | 487 | Import combined file | retire (DB restore) | n/a | 3 |
| `CSUTLDTC` | UT | 157 | LE `CEEDAYS` date check | date library | Yes | 1 |
| `COBSWAIT` | UT | 41 | CICS delay | scheduler/sleep | Yes | 1 |
| `COTRTLIC` | MOD | 2,098 | DB2 cursors, CICS | Tran-type list/delete API | Manual | 4 |
| `COTRTUPC` | MOD | 1,702 | DB2, CICS | Tran-type add/update API | Manual | 4 |
| `COBTUPDT` | MOD | 237 | DB2 batch | Tran-type batch update | Partial | 4 |
| `COPAUA0C` | MOD | 1,026 | MQ trigger, IMS | Authorization consumer (SQS) | Manual | 4 |
| `COPAUS0C` | MOD | 1,032 | IMS read, CICS | Pending-auth summary API | Manual | 4 |
| `COPAUS1C` | MOD | 604 | IMS update, CICS | Pending-auth detail API | Manual | 4 |
| `COPAUS2C` | MOD | 244 | DB2 insert | Auth-log writer | Partial | 4 |
| `CBPAUP0C` | MOD | 386 | IMS purge (batch) | Purge job | Partial | 4 |
| `PAUDBLOD`/`PAUDBUNL`/`DBUNLDGS` | MOD | 369/317/366 | IMS load/unload (GSAM) | retire after IMS migration | n/a | 4 |
| `COACCT01` | MOD | 620 | MQ + VSAM account inquiry | Account inquiry service (SQS) | Manual | 4 |
| `CODATE01` | MOD | 524 | MQ system-date inquiry | Date inquiry service (SQS) | Partial | 4 |

---

*Generated as a planning artifact from static analysis of the repository
(`app/`, `samples/`, `scripts/`, optional modules). No application code was modified.*
