# CBACT04C → Java Spring Boot: Modernization Design Note

Modernization of the CardDemo COBOL batch program **CBACT04C** (interest calculator)
to an idiomatic Java 17 / Spring Boot 3.x module, preserving full behavioral fidelity.

- **COBOL source:** `app/cbl/CBACT04C.cbl`
- **Java module:** `modernization/interest-calculator/`
- **Java package:** `com.carddemo.interest`

---

## 1. What the program does

CBACT04C is a batch step that computes monthly interest per account and posts it to the
account master. It reads the transaction-category-balance file (TCATBAL) **sequentially in
account-key order**, and for each category balance:

1. When the **account id changes**, it flushes the previously accumulated interest to that
   account (rewrite) and loads the new account + card cross-reference.
2. Looks up the **interest rate** for `(account group id, transaction type, transaction category)`
   from the disclosure-group file, falling back to a `DEFAULT` group if there is no match.
3. If the rate is non-zero, computes monthly interest, accumulates it, and **writes one
   interest transaction record**.
4. At end-of-file, flushes the last account.

### Files (COBOL `SELECT` → role)

| DDName | Copybook | Org / Access | Role |
|---|---|---|---|
| `TCATBALF` | `CVTRA01Y` | KSDS, sequential | **Input** — category balances (driver) |
| `XREFFILE` | `CVACT03Y` | KSDS, random (alt key = acct id) | Input — card number for the txn |
| `DISCGRP`  | `CVTRA02Y` | KSDS, random | Input — interest rate by group/type/cat |
| `ACCTFILE` | `CVACT01Y` | KSDS, random (I-O) | **In/out** — account master (rewritten) |
| `TRANSACT` | `CVTRA05Y` | sequential | **Output** — generated interest transactions |

---

## 2. Record layouts (copybooks) and byte offsets

Offsets below were **verified against the ASCII seed files** in `app/data/ASCII/`
(dates parse as `YYYY-MM-DD`, numeric fields contain digits, first records decode sensibly).

Monetary fields are **zoned decimal** `PIC S9(n)V99`: `n+2` digit bytes, implied decimal
point, sign carried as an "overpunch" on the trailing byte (`{`=+0 … `I`=+9, `}`=-0 … `R`=-9).

### CVTRA01Y — TRAN-CAT-BAL-RECORD (50 bytes)
| Field | PIC | Offset |
|---|---|---|
| TRANCAT-ACCT-ID | 9(11) | 0–11 |
| TRANCAT-TYPE-CD | X(02) | 11–13 |
| TRANCAT-CD | 9(04) | 13–17 |
| TRAN-CAT-BAL | S9(09)V99 | 17–28 (11 bytes) |
| FILLER | X(22) | 28–50 |

### CVTRA02Y — DIS-GROUP-RECORD (50 bytes)
| Field | PIC | Offset |
|---|---|---|
| DIS-ACCT-GROUP-ID | X(10) | 0–10 |
| DIS-TRAN-TYPE-CD | X(02) | 10–12 |
| DIS-TRAN-CAT-CD | 9(04) | 12–16 |
| DIS-INT-RATE | S9(04)V99 | 16–22 (6 bytes) |
| FILLER | X(28) | 22–50 |

### CVACT03Y — CARD-XREF-RECORD (50 bytes; ASCII seed omits trailing FILLER → 36 bytes)
| Field | PIC | Offset |
|---|---|---|
| XREF-CARD-NUM | X(16) | 0–16 |
| XREF-CUST-ID | 9(09) | 16–25 |
| XREF-ACCT-ID | 9(11) | 25–36 |
| FILLER | X(14) | 36–50 |

### CVACT01Y — ACCOUNT-RECORD (300 bytes)
| Field | PIC | Offset |
|---|---|---|
| ACCT-ID | 9(11) | 0–11 |
| ACCT-ACTIVE-STATUS | X(01) | 11–12 |
| ACCT-CURR-BAL | S9(10)V99 | 12–24 |
| ACCT-CREDIT-LIMIT | S9(10)V99 | 24–36 |
| ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | 36–48 |
| ACCT-OPEN-DATE | X(10) | 48–58 |
| ACCT-EXPIRAION-DATE | X(10) | 58–68 |
| ACCT-REISSUE-DATE | X(10) | 68–78 |
| ACCT-CURR-CYC-CREDIT | S9(10)V99 | 78–90 |
| ACCT-CURR-CYC-DEBIT | S9(10)V99 | 90–102 |
| ACCT-ADDR-ZIP | X(10) | 102–112 |
| ACCT-GROUP-ID | X(10) | 112–122 |
| FILLER | X(178) | 122–300 |

### CVTRA05Y — TRAN-RECORD (350 bytes, output)
| Field | PIC | Offset |
|---|---|---|
| TRAN-ID | X(16) | 0–16 |
| TRAN-TYPE-CD | X(02) | 16–18 |
| TRAN-CAT-CD | 9(04) | 18–22 |
| TRAN-SOURCE | X(10) | 22–32 |
| TRAN-DESC | X(100) | 32–132 |
| TRAN-AMT | S9(09)V99 | 132–143 |
| TRAN-MERCHANT-ID | 9(09) | 143–152 |
| TRAN-MERCHANT-NAME | X(50) | 152–202 |
| TRAN-MERCHANT-CITY | X(50) | 202–252 |
| TRAN-MERCHANT-ZIP | X(10) | 252–262 |
| TRAN-CARD-NUM | X(16) | 262–278 |
| TRAN-ORIG-TS | X(26) | 278–304 |
| TRAN-PROC-TS | X(26) | 304–330 |
| FILLER | X(20) | 330–350 |

---

## 3. Business rules recovered (PROCEDURE DIVISION)

| COBOL paragraph | Rule | Java method |
|---|---|---|
| main loop | Read TCATBAL in key order; on account-key change flush previous account; at EOF flush last | `InterestCalculationBatch.process` |
| 1050-UPDATE-ACCOUNT | `ACCT-CURR-BAL += WS-TOTAL-INT`; zero `ACCT-CURR-CYC-CREDIT`/`-DEBIT`; REWRITE | `updateAccount1050` |
| 1100-GET-ACCT-DATA | Keyed READ of account master | `getAcctData1100` |
| 1110-GET-XREF-DATA | Keyed READ of xref by account id | `getXrefData1110` |
| 1200 / 1200-A | Read disclosure group by (group,type,cat); on not-found retry `DEFAULT`; missing DEFAULT = abend | `getInterestRate1200` |
| 1300-COMPUTE-INTEREST | `WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200`; accumulate; write txn | `computeInterest1300` |
| 1300-B-WRITE-TX | Build & write interest transaction (see below) | `writeTransaction1300B` |
| 1400-COMPUTE-FEES | "To be implemented" (empty in COBOL) | `computeFees1400` (no-op) |
| Z-GET-DB2-FORMAT-TIMESTAMP | `FUNCTION CURRENT-DATE` → DB2 timestamp string | `TimestampProvider` |

### The interest formula and decimal behavior (the critical rule)

```
COMPUTE WS-MONTHLY-INT = ( TRAN-CAT-BAL * DIS-INT-RATE ) / 1200
```

`WS-MONTHLY-INT` is `PIC S9(09)V99` and the `COMPUTE` is **not `ROUNDED`**, so the result is
**truncated** to two decimals. In Java this is:

```java
balance.multiply(rate).divide(BigDecimal.valueOf(1200), 2, RoundingMode.DOWN)
```

`RoundingMode.DOWN` (truncate toward zero) — **not** `HALF_UP`. Interest amounts accumulate
into `WS-TOTAL-INT` and are posted to the balance only when the account key changes.

### Generated interest transaction (1300-B-WRITE-TX)

- `TRAN-ID` = `PARM-DATE` (10) + 6-digit zero-padded global suffix (`WS-TRANID-SUFFIX`, never reset)
- `TRAN-TYPE-CD` = `01`, `TRAN-CAT-CD` = `05` (stored as numeric `0005`), `TRAN-SOURCE` = `System`
- `TRAN-DESC` = `"Int. for a/c "` + 11-digit account id
- `TRAN-AMT` = `WS-MONTHLY-INT`; merchant id `0`, merchant name/city/zip spaces
- `TRAN-CARD-NUM` = `XREF-CARD-NUM`; both timestamps = DB2-format current timestamp

---

## 4. COBOL → Java mapping decisions

- **Money → `java.math.BigDecimal`** everywhere (never `double`/`float`); truncating division to
  mirror non-`ROUNDED` `COMPUTE`.
- **PIC 9(n) → `long`**; **PIC X(n) → `String`** (space-padded on write).
- **Zoned decimal** encode/decode centralized in `CobolCodec` (overpunch sign handling),
  keeping the module byte-faithful to the VSAM/flat-file representation.
- **Paragraphs → methods** with the paragraph number/name preserved for traceability.
- **VSAM files → gateway interfaces** (`AccountRepository`, `CardXrefRepository`,
  `DisclosureGroupRepository`, `TransactionWriter`) with in-memory implementations seeded from
  the packaged ASCII files. A batch is inherently file/stream oriented, so this preserves the
  program's semantics (sequential driver + keyed lookups + rewrite + sequential output) more
  faithfully than a CRUD/JPA layer would; a JDBC/JPA adapter can be dropped in behind the same
  interfaces later.
- **Non-determinism isolated**: `FUNCTION CURRENT-DATE` is behind `TimestampProvider` so
  equivalence tests are deterministic.

---

## 5. Equivalence validation

JUnit 5 tests (`src/test/java`) assert value-for-value equivalence with the COBOL logic:

- **Truncation vs rounding**: `100.00 * 23.00 / 1200 = 1.9166…` → `1.91` (not `1.92`).
- Accumulation across categories and posting to the balance; cycle buckets zeroed.
- Rate `0` → no transaction, no interest.
- Negative balance interest (`-500.00 * 18.00 / 1200 = -7.50`).
- `DEFAULT` group fallback; abend when neither group nor DEFAULT exists.
- 1300-B-WRITE-TX field-by-field, including the 350-byte fixed-width rendering and the
  zoned-encoded amount.
- Global transaction-id suffix increments across accounts; each account posted exactly once.
- Real seed files parse at the documented offsets.

Build/test commands:

```
cd modernization/interest-calculator
mvn clean test          # run the equivalence suite
mvn -q -DskipTests package
mvn spring-boot:run -Dspring-boot.run.profiles=batch   # run against packaged seed data
```
