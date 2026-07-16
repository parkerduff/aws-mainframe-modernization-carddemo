# Phase 0 — Discovery & Baseline

Baseline analysis of the CardDemo transaction-processing subsystem prior to the Java migration.
It captures (1) the COBOL programs that touch the transaction (`TRANSACT`) VSAM KSDS, (2) the
record layouts of the copybooks being migrated in Phase 1, and (3) the batch run order that serves
as the golden reference for future orchestration and regression testing.

All file/line references point at the existing COBOL sources under `app/` and are **read-only**;
this document and the `java-migration/` project do not modify any COBOL, JCL, or copybook files.

---

## 1. Program inventory (readers/writers of the `TRANSACT` file)

The `TRANSACT` VSAM KSDS (record layout `CVTRA05Y` / `TRAN-RECORD`, 350 bytes, key `TRAN-ID`) is
accessed by both the online (CICS) programs and the batch programs. Online programs reach the file
through CICS file control (`EXEC CICS STARTBR/READNEXT/READPREV/READ/WRITE/REWRITE/ENDBR`); batch
programs use native COBOL file I/O (`SELECT ... ASSIGN`, `READ`, `WRITE`, `REWRITE`).

| Program | Type | Role | Reads | Writes | `TRANSACT` access |
|---|---|---|---|---|---|
| `COTRN00C` | Online (CICS) | **List transactions** — paginated browse of the transaction file | `TRANSACT` | — | `STARTBR` + `READNEXT`/`READPREV` + `ENDBR` (forward/backward paging) |
| `COTRN01C` | Online (CICS) | **View transaction** — display a single transaction by ID | `TRANSACT` | — | `EXEC CICS READ` on `TRANSACT` by `TRAN-ID` |
| `COTRN02C` | Online (CICS) | **Add transaction** — capture and append a new transaction | `TRANSACT`, `ACCTDAT` (`CVACT01Y`), `CARDXREF` (`CVACT03Y`) | `TRANSACT` | `STARTBR`/`READPREV`/`ENDBR` to derive the next `TRAN-ID`; `EXEC CICS WRITE` to append |
| `COBIL00C` | Online (CICS) | **Bill pay** — pay account balance in full, writing a payment transaction | `ACCTDAT` (`CVACT01Y`), `CARDXREF` AIX, `TRANSACT` | `TRANSACT` (new payment), `ACCTDAT` (balance update) | `STARTBR`/`READPREV`/`ENDBR` for next id; `EXEC CICS WRITE` payment; `EXEC CICS REWRITE` on `ACCTDAT` |
| `CBTRN01C` | Batch | **Daily transaction reader** — validate/join daily transactions against reference files | `DALYTRAN` (`CVTRA06Y`), `CUSTOMER`, `XREF` (`CVACT03Y`), `CARD`, `ACCOUNT` (`CVACT01Y`) | — (read/validation pass) | Reads `TRAN-RECORD` layout (`CVTRA05Y`); prepares records for posting |
| `CBTRN02C` | Batch | **Posting engine** — post daily transactions to the master files | `DALYTRAN` (`CVTRA06Y`), `XREF`, `ACCOUNT`, `TCATBAL` (`CVTRA01Y`) | `TRANSACT` (posted txns), `ACCOUNT` (balances), `TCATBAL` (category balances), `DALYREJS` (rejects) | `WRITE FD-TRANFILE-REC FROM TRAN-RECORD`; `REWRITE` on `ACCOUNT` and `TCATBAL` |
| `CBTRN03C` | Batch | **Transaction detail report** — print formatted transaction report | `TRANSACT`, `XREF` (`CVACT03Y`), `TRANTYPE`, `TRANCATG`, `DATEPARM` | `TRANREPT` (report file) | Sequential `READ TRANSACT-FILE INTO TRAN-RECORD` |
| `CBACT04C` | Batch | **Interest calculator** — compute interest and write *system* transactions | `TCATBAL` (`CVTRA01Y`), `XREF`, `ACCOUNT`, `DISCGRP` | `TRANSACT` (interest/system txns), `ACCOUNT` (balances) | `WRITE FD-TRANFILE-REC FROM TRAN-RECORD` for generated interest transactions |

**Notes**

- The online transaction id used by `COTRN02C`, `COBIL00C` is derived by browsing the file backwards
  (`STARTBR` at high values + `READPREV`) to find the highest existing `TRAN-ID` and incrementing it.
- `CBTRN02C` (posting) and `CBACT04C` (interest) are the two batch writers of `TRANSACT`.
  Interest-generated ("system") transactions are later combined with the daily ones (see `COMBTRAN` in
  the batch run order below).
- `CBTRN01C`/`CBTRN02C` distinguish the *daily* input file (`DALYTRAN`, layout `CVTRA06Y`) from the
  posted *master* file (`TRANSACT`, layout `CVTRA05Y`). The two layouts are byte-identical (350 bytes).

---

## 2. Record layouts (copybooks migrated in Phase 1)

COBOL numeric conventions used below:

- `PIC 9(n)` — unsigned zoned decimal, `n` bytes (one byte per digit). Maps to an integral Java type.
- `PIC X(n)` — alphanumeric, `n` bytes. Maps to `String`.
- `PIC S9(i)V99` — signed zoned decimal with **implied** 2-digit fraction (`V` occupies no byte).
  Width on disk = `i + 2` bytes; the sign is *overpunched* onto the last byte. **Maps to
  `java.math.BigDecimal` with scale 2 — never `float`/`double`.**

### Zoned-decimal sign overpunch (last byte of a signed field)

The ASCII sample files encode the sign on the final digit byte using the standard IBM convention.
This is implemented in `com.carddemo.dataload.ZonedDecimal`:

| Last digit | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|---|
| Positive | `{` | `A` | `B` | `C` | `D` | `E` | `F` | `G` | `H` | `I` |
| Negative | `}` | `J` | `K` | `L` | `M` | `N` | `O` | `P` | `Q` | `R` |

Example: `0000005047G` → digits `00000050477` = `504.77`; `0000009190}` → `-919.00`.

### 2.1 `CVTRA05Y` — `TRAN-RECORD` (TRANSACT master, RECLN 350)

| Offset | Len | COBOL field | PIC | Java type | JPA field (`Transaction`) |
|---:|---:|---|---|---|---|
| 0 | 16 | TRAN-ID | X(16) | String | `tranId` (`@Id`) |
| 16 | 2 | TRAN-TYPE-CD | X(02) | String | `tranTypeCd` |
| 18 | 4 | TRAN-CAT-CD | 9(04) | Integer | `tranCatCd` |
| 22 | 10 | TRAN-SOURCE | X(10) | String | `tranSource` |
| 32 | 100 | TRAN-DESC | X(100) | String | `tranDesc` |
| 132 | 11 | TRAN-AMT | S9(09)V99 | **BigDecimal(scale 2)** | `tranAmt` |
| 143 | 9 | TRAN-MERCHANT-ID | 9(09) | Long | `tranMerchantId` |
| 152 | 50 | TRAN-MERCHANT-NAME | X(50) | String | `tranMerchantName` |
| 202 | 50 | TRAN-MERCHANT-CITY | X(50) | String | `tranMerchantCity` |
| 252 | 10 | TRAN-MERCHANT-ZIP | X(10) | String | `tranMerchantZip` |
| 262 | 16 | TRAN-CARD-NUM | X(16) | String | `tranCardNum` |
| 278 | 26 | TRAN-ORIG-TS | X(26) | String (26-char ts) | `tranOrigTs` |
| 304 | 26 | TRAN-PROC-TS | X(26) | String (26-char ts) | `tranProcTs` |
| 330 | 20 | FILLER | X(20) | — | (not persisted) |

Timestamps use the format `yyyy-MM-dd HH:mm:ss.SSSSSS` (26 chars). The raw string is stored to
guarantee lossless round-tripping; `com.carddemo.util.CobolTimestamp` converts to/from `LocalDateTime`.

### 2.2 `CVTRA06Y` — `DALYTRAN-RECORD` (daily input, RECLN 350)

Byte-identical structure to `CVTRA05Y` with `DALYTRAN-` prefixed names. Migrated to the
`DailyTransaction` staging entity.

| Offset | Len | COBOL field | PIC | Java type | JPA field (`DailyTransaction`) |
|---:|---:|---|---|---|---|
| — | — | *(surrogate key)* | — | Long | `id` (`@GeneratedValue`) |
| 0 | 16 | DALYTRAN-ID | X(16) | String | `dalytranId` |
| 16 | 2 | DALYTRAN-TYPE-CD | X(02) | String | `dalytranTypeCd` |
| 18 | 4 | DALYTRAN-CAT-CD | 9(04) | Integer | `dalytranCatCd` |
| 22 | 10 | DALYTRAN-SOURCE | X(10) | String | `dalytranSource` |
| 32 | 100 | DALYTRAN-DESC | X(100) | String | `dalytranDesc` |
| 132 | 11 | DALYTRAN-AMT | S9(09)V99 | **BigDecimal(scale 2)** | `dalytranAmt` |
| 143 | 9 | DALYTRAN-MERCHANT-ID | 9(09) | Long | `dalytranMerchantId` |
| 152 | 50 | DALYTRAN-MERCHANT-NAME | X(50) | String | `dalytranMerchantName` |
| 202 | 50 | DALYTRAN-MERCHANT-CITY | X(50) | String | `dalytranMerchantCity` |
| 252 | 10 | DALYTRAN-MERCHANT-ZIP | X(10) | String | `dalytranMerchantZip` |
| 262 | 16 | DALYTRAN-CARD-NUM | X(16) | String | `dalytranCardNum` |
| 278 | 26 | DALYTRAN-ORIG-TS | X(26) | String | `dalytranOrigTs` |
| 304 | 26 | DALYTRAN-PROC-TS | X(26) | String | `dalytranProcTs` |
| 330 | 20 | FILLER | X(20) | — | (not persisted) |

A surrogate key is used because a daily file may repeat a `DALYTRAN-ID` across runs.

### 2.3 `CVACT01Y` — `ACCOUNT-RECORD` (RECLN 300)

| Offset | Len | COBOL field | PIC | Java type | JPA field (`Account`) |
|---:|---:|---|---|---|---|
| 0 | 11 | ACCT-ID | 9(11) | Long | `acctId` (`@Id`) |
| 11 | 1 | ACCT-ACTIVE-STATUS | X(01) | String | `acctActiveStatus` |
| 12 | 12 | ACCT-CURR-BAL | S9(10)V99 | **BigDecimal(scale 2)** | `acctCurrBal` |
| 24 | 12 | ACCT-CREDIT-LIMIT | S9(10)V99 | **BigDecimal(scale 2)** | `acctCreditLimit` |
| 36 | 12 | ACCT-CASH-CREDIT-LIMIT | S9(10)V99 | **BigDecimal(scale 2)** | `acctCashCreditLimit` |
| 48 | 10 | ACCT-OPEN-DATE | X(10) | String | `acctOpenDate` |
| 58 | 10 | ACCT-EXPIRAION-DATE | X(10) | String | `acctExpirationDate` |
| 68 | 10 | ACCT-REISSUE-DATE | X(10) | String | `acctReissueDate` |
| 78 | 12 | ACCT-CURR-CYC-CREDIT | S9(10)V99 | **BigDecimal(scale 2)** | `acctCurrCycCredit` |
| 90 | 12 | ACCT-CURR-CYC-DEBIT | S9(10)V99 | **BigDecimal(scale 2)** | `acctCurrCycDebit` |
| 102 | 10 | ACCT-ADDR-ZIP | X(10) | String | `acctAddrZip` |
| 112 | 10 | ACCT-GROUP-ID | X(10) | String | `acctGroupId` |
| 122 | 178 | FILLER | X(178) | — | (not persisted) |

(`ACCT-EXPIRAION-DATE` is the copybook's original spelling; the Java field is `acctExpirationDate`,
column `ACCT_EXPIRATION_DATE`.)

### 2.4 `CVACT03Y` — `CARD-XREF-RECORD` (RECLN 50)

| Offset | Len | COBOL field | PIC | Java type | JPA field (`CardXref`) |
|---:|---:|---|---|---|---|
| 0 | 16 | XREF-CARD-NUM | X(16) | String | `xrefCardNum` (`@Id`) |
| 16 | 9 | XREF-CUST-ID | 9(09) | Long | `xrefCustId` |
| 25 | 11 | XREF-ACCT-ID | 9(11) | Long | `xrefAcctId` |
| 36 | 14 | FILLER | X(14) | — | (not persisted) |

In the ASCII sample the trailing 14-byte FILLER is trimmed (lines are 36 bytes); the loader
right-pads short lines before slicing.

### 2.5 `CVTRA01Y` — `TRAN-CAT-BAL-RECORD` (RECLN 50)

| Offset | Len | COBOL field | PIC | Java type | JPA field (`TranCatBalance`) |
|---:|---:|---|---|---|---|
| 0 | 11 | TRANCAT-ACCT-ID | 9(11) | Long | `id.trancatAcctId` (composite `@EmbeddedId`) |
| 11 | 2 | TRANCAT-TYPE-CD | X(02) | String | `id.trancatTypeCd` (composite) |
| 13 | 4 | TRANCAT-CD | 9(04) | Integer | `id.trancatCd` (composite) |
| 17 | 11 | TRAN-CAT-BAL | S9(09)V99 | **BigDecimal(scale 2)** | `tranCatBal` |
| 28 | 22 | FILLER | X(22) | — | (not persisted) |

The COBOL `TRAN-CAT-KEY` group (`TRANCAT-ACCT-ID` + `TRANCAT-TYPE-CD` + `TRANCAT-CD`) becomes the
JPA composite key `TranCatBalanceId`.

---

## 3. Batch run order (golden reference)

Captured from `scripts/run_posting.sh` and `scripts/run_full_batch.sh`. Each step submits a JCL job
(under `app/jcl/`) via the mainframe FTP/JES bridge. This ordering is the reference for future
orchestration and regression testing.

### 3.1 Posting cycle — `scripts/run_posting.sh`

| Step | JCL | Purpose |
|---:|---|---|
| 1 | `CLOSEFIL.jcl` | Close files in CICS (quiesce online access) |
| 2 | `ACCTFILE.jcl` | Refresh Account Master file |
| 3 | `TCATBALF.jcl` | Refresh Transaction Category Balance file |
| 4 | `TRANBKP.jcl` | Refresh / back up Transaction Master |
| 5 | `POSTTRAN.jcl` | **Core posting job** (`CBTRN02C`) — post daily transactions |
| 6 | `TRANIDX.jcl` | Define alternate index on the transaction file |
| 7 | `OPENFIL.jcl` | Open files in CICS (resume online access) |

### 3.2 Full batch cycle — `scripts/run_full_batch.sh`

| Step | JCL | Purpose |
|---:|---|---|
| 1 | `CLOSEFIL.jcl` | Close files in CICS |
| 2 | `ACCTFILE.jcl` | Refresh Account Data file |
| 3 | `CARDFILE.jcl` | Refresh Card Data |
| 4 | `XREFFILE.jcl` | Refresh Card Cross Reference |
| 5 | `CUSTFILE.jcl` | Refresh Customer Data |
| 6 | `TRANBKP.jcl` | Refresh Transaction Data |
| 7 | `DISCGRP.jcl` | Refresh Disclosure Group |
| 8 | `TCATBALF.jcl` | Refresh Transaction Category Balance |
| 9 | `TRANTYPE.jcl` | Refresh Transaction Type File |
| 11 | `DUSRSECJ.jcl` | Refresh User Security File |
| 12 | `POSTTRAN.jcl` | **Core posting job** (`CBTRN02C`) |
| 13 | `INTCALC.jcl` | **Interest calculation** (`CBACT04C`) — writes system transactions |
| 14 | `TRANBKP.jcl` | Back up Transaction Data |
| 15 | `COMBTRAN.jcl` | Combine system (interest) transactions with the daily ones |
| 15 | `TRANIDX.jcl` | Define alternate index on the transaction file |
| 16 | `OPENFIL.jcl` | Open files in CICS |

(Step numbers 10 and the duplicate "15" reflect the original numbering in `run_full_batch.sh`.)

**Regression-relevant dependencies**

1. Reference/master files are refreshed *before* posting.
2. Posting (`CBTRN02C`, step 12) runs before interest calculation (`CBACT04C`, step 13).
3. Interest-generated transactions are merged with daily transactions (`COMBTRAN`, step 15) *after*
   interest calculation.
4. The alternate index (`TRANIDX`) is (re)built after all writes, before CICS files reopen.
