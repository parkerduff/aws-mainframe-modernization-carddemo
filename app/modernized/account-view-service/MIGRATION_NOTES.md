# Migration Note — COACTVWC (CAVW) → `account-view-service`

## Scope

This change migrates **one** bounded transaction: the CardDemo **Account View**
(`COACTVWC`, tran `CAVW`) read-and-validate flow. Nothing else in CardDemo is
touched. The new module lives at `app/modernized/account-view-service/` and is a
self-contained Spring Boot project.

## What was migrated

| Legacy element | Modern element |
|---|---|
| `2210-EDIT-ACCOUNT` (input edits) | `AccountViewService.viewAccount(...)` validation block |
| `9200/9300/9400` (xref/account/customer reads) | `CardXrefRepository` / `AccountRepository` / `CustomerRepository` |
| `9000-READ-ACCT` orchestration | sequential, short-circuit read flow in `viewAccount` |
| `1200-SETUP-SCREEN-VARS` field mapping (incl. SSN format) | `AccountDetails` + `mapToDetails` / `formatSsn` |
| Record copybooks `CVACT01Y/03Y`, `CVCUS01Y` | `Account`, `CardXref`, `Customer` records |
| Outcome branches | `AccountViewStatus` enum + `AccountViewResult` |

## Key translation decisions

- **Decimal fidelity.** `PIC S9(10)V99` amounts use `BigDecimal` (scale 2); no
  floating point, preserving exact mainframe arithmetic semantics.
- **`IS NUMERIC` on `PIC X(11)`.** Modeled as "exactly 11 ASCII digits". Shorter
  input on a fixed-width 3270 field is padded and fails the numeric test, so the
  service rejects anything that is not 11 digits — matching the legacy edit.
- **Customer key source.** The customer read uses `XREF-CUST-ID` from the xref
  record (not the account id), exactly as the COBOL does.
- **Error messages.** The stable human-readable text of each message is preserved
  verbatim (including the legacy double-space in "must  be"). The CICS
  `Resp/Reas` diagnostic suffix on NOTFND messages is intentionally dropped as a
  runtime/platform artifact.

## Deliberately out of scope (presentation / platform plumbing)

- BMS screen send/receive (`SEND/RECEIVE MAP`), cursor/color/attribute handling.
- PF-key (AID) routing and `XCTL` navigation between programs/menus.
- Commarea chaining and CICS `RETURN TRANSID`.
- `HANDLE ABEND` / abend routine.
- The physical VSAM/CICS file access (replaced by repository interfaces; an
  in-memory implementation is provided for the runnable demo and tests).

## Data layer

The service depends on three narrow repository interfaces. The included
in-memory implementations stand in for the VSAM reads so the module builds, runs,
and is testable without a mainframe. A production migration would back these with
a relational schema (account / customer tables + a card-xref table with an index
on account id) per the VSAM-to-RDBMS mapping; the service logic is unchanged.

## Verification

- 24 tests pass (`mvn test`): 21 service-level equivalence tests covering the
  validation branches, the three not-found branches, the happy-path field
  mapping, decimal precision, SSN formatting, and xref-driven customer lookup;
  plus 3 web tests for the HTTP status mapping.
- Build: `mvn -q -DskipTests package`.

## How to run

```bash
cd app/modernized/account-view-service
mvn test                       # equivalence + web tests
mvn spring-boot:run            # starts on :8080
curl 'http://localhost:8080/api/accounts/view?accountId=00000000001'
```
