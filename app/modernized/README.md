# CardDemo Batch (Modernized)

Modern Java ports of selected CardDemo batch COBOL business rules. Each service
is a standalone, framework-free class with an in-memory-friendly repository
abstraction and JUnit 5 unit tests.

## Build & test

Requires Java 17 and Maven.

```bash
mvn test          # run unit tests
mvn -DskipTests package
```

## Rules implemented

### R19 — Transaction Report Date Filtering
Port of `app/cbl/CBTRN03C.cbl`.

- `service/TransactionReportService` — filters transactions to an inclusive
  `YYYY-MM-DD` date range (`TRAN-PROC-TS(1:10)` comparison, lines 173-178),
  groups by card number with per-account totals (lines 181-187), enriches each
  line via card-XREF / transaction-type / transaction-category lookups, and
  produces page totals (every configurable page size, default 20) plus a grand
  total. Returns a structured `TransactionReport` instead of a flat print file.
- Models: `TransactionRecord` (`CVTRA05Y`), `TransactionReportLine`,
  `PageTotalSummary`, `AccountTotalSummary`, `GrandTotalSummary`,
  `TransactionReport`.
- `repository/TransactionRepository` — abstracts the four VSAM files
  (transactions, card XREF, transaction type, transaction category).

### R20 — Expired Authorization Purge
Port of `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`.

- `service/AuthorizationPurgeService` — walks each pending-authorization summary
  and its details, deletes details that are at least `expiryDays` old (default 5),
  adjusts the parent summary's approved/declined counters and amounts
  (lines 287-293), deletes a summary once it has no remaining approved or
  declined auths (lines 156-158), and checkpoints once more than
  `checkpointFrequency` summaries have been processed since the last checkpoint
  (default 5; the COBOL `>` comparison on line 160). The 9's-complement Julian
  `PA-AUTH-DATE-9C` encoding is
  resolved to `java.time.LocalDate` at the model boundary so expiry uses
  `ChronoUnit.DAYS`.
- Models: `AuthorizationSummary` (`CIPAUSMY`), `AuthorizationDetail`
  (`CIPAUDTY`), `PurgeResult`.
- `repository/AuthorizationRepository` — abstracts the IMS DL/I access
  (`GN`/`GNP`/`DLET`/`CHKP`).

## Notes on faithful vs. modernized behaviour

- **Date comparison** uses lexicographic `String` comparison on `YYYY-MM-DD`,
  which is equivalent to chronological ordering for that format (matches COBOL).
- **Paging** is driven by detail-line count (one page total per `pageSize`
  detail lines) rather than the original physical print-line counter, since the
  output is a data structure rather than a 133-column report file.
- **Summary deletion** applies the clearly-intended rule
  `approvedAuthCnt <= 0 AND declinedAuthCnt <= 0`; the original COBOL checks the
  approved count twice (line 156).
- **Surviving summaries** whose counters changed are persisted via
  `AuthorizationRepository.updateSummary(...)`.
