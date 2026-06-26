# COACTVWC (CAVW) — Account View: Current Behavior

This document captures the **as-is** behavior of the legacy COBOL/CICS program
that the modern `account-view-service` reproduces. It is derived entirely from
the COBOL source and copybooks in this repository.

## Program identity

| Attribute | Value | Source |
|---|---|---|
| Program | `COACTVWC` | `app/cbl/COACTVWC.cbl:22-23` |
| Transaction id | `CAVW` | `app/cbl/COACTVWC.cbl:145-146` |
| Mapset / map | `COACTVW` / `CACTVWA` | `app/cbl/COACTVWC.cbl:147-150` |
| Layer | Business logic (online, CICS) | `app/cbl/COACTVWC.cbl:3` |
| Function | Accept and process an Account View request | `app/cbl/COACTVWC.cbl:4` |

`COACTVWC` is the CardDemo online transaction that takes an **account number**,
validates it, looks up the related records across three VSAM files, and displays
the account and its owning customer's details on the `CACTVWA` 3270 screen.

## Inputs

| Input | Copybook / field | Notes |
|---|---|---|
| Account number (`ACCTSID`) | `CC-ACCT-ID PIC X(11)` (`app/cpy/CVCRD01Y.cpy`) | The only user-entered key. |
| PF key / AID | `CCARD-AID` (`app/cpy/CVCRD01Y.cpy`) | `Enter` = process, `PF03` = exit to caller/menu. Other keys are remapped to `Enter`. |
| Commarea context | `CARDDEMO-COMMAREA` (`app/cpy/COCOM01Y.cpy`) | Carries from/to program & tranid for navigation; re-entry flags. |

The modern service models **only the data path** (`ACCTSID` → records); PF-key
routing, screen send/receive, and commarea navigation are CICS presentation
concerns that are intentionally dropped (see `MIGRATION_NOTES.md`).

## Files read (all read-only)

| Step (paragraph) | Dataset | Key | Record copybook |
|---|---|---|---|
| `9200-GETCARDXREF-BYACCT` | `CXACAIX` (alt index over card xref) | account id | `CARD-XREF-RECORD` (`app/cpy/CVACT03Y.cpy`) |
| `9300-GETACCTDATA-BYACCT` | `ACCTDAT` | account id | `ACCOUNT-RECORD` (`app/cpy/CVACT01Y.cpy`) |
| `9400-GETCUSTDATA-BYCUST` | `CUSTDAT` | customer id (from xref) | `CUSTOMER-RECORD` (`app/cpy/CVCUS01Y.cpy`) |

The customer id used in step 3 comes from `XREF-CUST-ID` returned by step 1
(`app/cbl/COACTVWC.cbl:739`, `708-711`) — **not** from the account record.

## Validation rules — `2210-EDIT-ACCOUNT` (`app/cbl/COACTVWC.cbl:649-684`)

Pre-edit (`2200-EDIT-MAP-INPUTS`, lines 628-633): if `ACCTSID` is `*` or spaces it
is treated as "no value" (`LOW-VALUES`).

1. **Blank** — account id is spaces/low-values →
   `INPUT-ERROR`, message **"Account number not provided"** (`WS-PROMPT-FOR-ACCT`, lines 121-122, 657-659).
2. **Not numeric or zero** — `CC-ACCT-ID IS NOT NUMERIC` *or* `= ZEROES` →
   `INPUT-ERROR`, message **"Account Filter must  be a non-zero 11 digit number"** (lines 666-674; note the double space in the literal).
   Because the field is fixed `PIC X(11)`, any input that is not exactly 11
   digits is space/low-value padded and therefore fails the `IS NUMERIC` test.
3. **Valid** — exactly 11 numeric digits, non-zero → proceed to read.

## Read-and-validate flow — `9000-READ-ACCT` (`app/cbl/COACTVWC.cbl:687-722`)

Reads happen in order and **short-circuit on the first failure**:

1. `9200` read xref by account id:
   - `NOTFND` → error **"Account:`<id>` not found in Cross ref file."** (lines 741-758).
   - `NORMAL` → capture `XREF-CUST-ID`, `XREF-CARD-NUM`.
2. `9300` read account master by account id:
   - `NOTFND` → error **"Account:`<id>` not found in Acct Master file."** (lines 789-807).
   - `NORMAL` → `FOUND-ACCT-IN-MASTER`.
3. `9400` read customer master by the xref's customer id:
   - `NOTFND` → error **"CustId:`<id>` not found in customer master."** (lines 839-857).
   - `NORMAL` → `FOUND-CUST-IN-MASTER`.

> The legacy `NOTFND` messages additionally append a CICS diagnostic suffix
> (`Resp: <n> Reas: <n>`). Those codes are runtime/platform artifacts and are
> intentionally **not** reproduced; the modern service returns the stable,
> human-readable portion of each message.

For any read returning something other than `NORMAL`/`NOTFND`, the legacy program
builds a generic `File Error: ...` message and treats it as `INPUT-ERROR`.

## Outputs — `1200-SETUP-SCREEN-VARS` (`app/cbl/COACTVWC.cbl:460-535`)

On success the program maps onto the screen:

**Account (ACCTDAT):** active status, current balance, credit limit, cash credit
limit, current-cycle credit, current-cycle debit, open/expiration/reissue dates,
group id (lines 471-491).

**Customer (CUSTDAT):** customer id, SSN reformatted as **`XXX-XX-XXXX`**
(`STRING CUST-SSN(1:3) '-' CUST-SSN(4:2) '-' CUST-SSN(6:4)`, lines 496-503),
FICO score, date of birth, first/middle/last name, address lines 1-3 (line 3 →
city), state, zip, country, phones 1-2, govt-issued id, EFT account id, primary
card-holder indicator (lines 505-522).

Money fields are `PIC S9(10)V99` (signed, two implied decimals) — see
`app/cpy/CVACT01Y.cpy`. They must be represented with exact decimal types
(`BigDecimal`, scale 2), never floating point.

The info line always shows the prompt **"Enter or update id of account to
display"** (`WS-PROMPT-FOR-INPUT`, lines 113-114, 528-529): on this map the
program never sets the alternate "Displaying details..." info text.

## Edge cases captured from copybook field definitions

| Edge case | Behavior |
|---|---|
| Account id `*` or all spaces | Treated as blank → "Account number not provided". |
| Fewer/more than 11 digits | Fails `IS NUMERIC` (padding) → non-zero-11-digit message. |
| Embedded non-digit (letter/space/punct) within 11 chars | Fails `IS NUMERIC`. |
| `00000000000` | Numeric but `= ZEROES` → non-zero-11-digit message. |
| Xref present, account master missing | `ACCOUNT_NOT_FOUND` (reads short-circuit). |
| Xref + account present, customer missing | `CUSTOMER_NOT_FOUND`. |
| Customer id resolved from xref, not account | Lookup uses `XREF-CUST-ID`. |
| SSN `123456789` | Displayed as `123-45-6789`. |
| `S9(10)V99` amounts | Exact 2-decimal value preserved. |
