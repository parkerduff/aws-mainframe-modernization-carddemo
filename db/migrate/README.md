# CardDemo VSAM → Relational Migration

This directory contains the tooling and reference material for migrating
the CardDemo operational VSAM files to the relational schema defined
in `db/schema.sql`.

## Source-of-truth mapping

| VSAM dataset                                  | Copybook   | Length | Target table             |
|-----------------------------------------------|------------|-------:|--------------------------|
| `AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS`          | `CVACT01Y` |    300 | `accounts`               |
| `AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS`          | `CVACT02Y` |    150 | `cards`                  |
| `AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS`          | `CVCUS01Y` |    500 | `customers`              |
| `AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS`          | `CVACT03Y` |     50 | `card_xref`              |
| `AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS`          | `CVTRA05Y` |    350 | `transactions`           |
| `AWS.M2.CARDDEMO.DALYTRAN.PS`                 | `CVTRA06Y` |    350 | `daily_transactions`     |
| `AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS`          | `CVTRA01Y` |     50 | `tcat_balances`          |
| `AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS`           | `CVTRA02Y` |     50 | `disclosure_groups`      |
| `AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS`          | `CVTRA03Y` |     60 | `transaction_types`      |
| `AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS`          | `CVTRA04Y` |     60 | `transaction_categories` |
| `AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS` (160-byte) | `CSUSR01Y` |    160 | `user_security`          |

## Step-by-step migration

1. **Extract from VSAM** using IDCAMS REPRO to produce sequential
   ASCII-encoded fixed-width PS files.  Move them to the migration
   workstation (or S3 bucket) as needed.
2. **Stage to CSV** with the loader CLI:
   ```bash
   python -m db.migrate.loaders accounts \
       /path/to/AWS.M2.CARDDEMO.ACCTDATA.PS \
       artifacts/accounts.csv --format csv
   ```
3. **Bulk load** the CSV into the target database:
   * PostgreSQL: `\copy accounts FROM 'artifacts/accounts.csv' CSV HEADER`
   * Snowflake:  `COPY INTO accounts FROM @stage/accounts.csv FILE_FORMAT=csv_header`
   * MySQL:      `LOAD DATA LOCAL INFILE 'artifacts/accounts.csv' INTO TABLE accounts FIELDS TERMINATED BY ',' ...`
4. **Validate** post-load row counts against the IDCAMS REPRO totals
   and run the integration test suite under `tests/integration/`.

For environments without a Python toolchain, the loader can also emit
SQL `INSERT` statements with `--format sql` which can be piped directly
into `psql`/`mysql`/`sqlite3`.

## Bootstrapping the user-security table

The user-security table requires SHA-256 hashed passwords.  The
`init_user_security.sh` script reads the bootstrap admin and user
passwords from a configured secret store (see `config/secrets-config.md`)
and emits either:

* a 160-byte VSAM-compatible PS file (mode `ps`) for staging via
  `ESDSRRDS.jcl`, or
* a SQL bootstrap script (mode `sql`) for direct loading into
  `user_security`.

The script never writes plaintext passwords to disk.

## Re-running migrations

The loader is idempotent only insofar as the schema's `INSERT`
statements are.  To repeat a migration use the wrapper script
`run_full_migration.sh` (also in this directory) which truncates target
tables in dependency order before reloading.
