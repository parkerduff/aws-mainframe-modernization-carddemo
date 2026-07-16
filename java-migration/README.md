# CardDemo Java Migration

Java migration of the CardDemo transaction-processing subsystem. This module is self-contained and
does **not** modify the existing COBOL, JCL, BMS, or copybook sources under `../app/`.

- **Phase 0** — discovery & baseline: [`docs/PHASE0_BASELINE.md`](docs/PHASE0_BASELINE.md)
  (program inventory, copybook record layouts, batch run order).
- **Phase 1** — data-layer migration (VSAM → relational): Spring Boot + Spring Data JPA project with
  H2 + Flyway, JPA entities mirroring the copybooks, repositories, and a fixed-width data loader.

## Stack

- Java 21, Spring Boot 3.3, Spring Data JPA
- H2 (in-memory) for local dev; Flyway-managed schema (`src/main/resources/db/migration/`)
- Maven

## Build & test

```bash
mvn compile      # compile
mvn test         # run unit + integration tests
mvn package      # build the runnable jar
```

## Layout

```
src/main/java/com/carddemo/
  domain/        JPA entities (Account, CardXref, TranCatBalance[+Id], Transaction, DailyTransaction)
  repository/    Spring Data JPA repositories
  dataload/      Fixed-width loader (ZonedDecimal, FixedWidthRecord, RecordParsers, FixedWidthLoader)
  util/          CobolTimestamp (26-char timestamp <-> LocalDateTime)
src/main/resources/
  application.properties
  db/migration/V1__initial_schema.sql
```

## Loading the sample data

The one-time loader parses the fixed-width ASCII sample files in `../app/data/ASCII/`
(`acctdata.txt`, `cardxref.txt`, `tcatbal.txt`, `dailytran.txt`) using the exact copybook byte
offsets and populates the relational tables. Signed decimal amounts are parsed as
`BigDecimal` with scale 2 (no floating-point rounding drift).

Run it against a persistent database by enabling the loader at startup:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--carddemo.dataload.enabled=true"
```

By default `carddemo.dataload.enabled=false`; the loader logic is also exercised by
`FixedWidthLoaderIntegrationTest`.

## Data-type mapping notes

- `PIC S9(09)V99` / `PIC S9(10)V99` → `java.math.BigDecimal` (scale 2) / `DECIMAL(11,2)` / `DECIMAL(12,2)`
- `PIC 9(n)` → `Long` / `Integer`
- `PIC X(n)` → `String`
- Signed zoned-decimal sign overpunch (`{`, `A`–`I`, `}`, `J`–`R`) is decoded by
  `com.carddemo.dataload.ZonedDecimal`.
