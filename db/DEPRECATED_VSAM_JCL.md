# Deprecated VSAM JCL (post-relational migration)

The relational migration (`db/schema.sql`, `db/migrate/`) replaces the
operational VSAM file inventory.  The JCL members listed below are
**deprecated** once the migration is complete and should be removed
during the cut-over window.

> **Removal must be coordinated with operations.** The members are
> retained on `main` so that environments still running the VSAM data
> layer continue to function during the strangler-fig transition.

## Replaced by `db/schema.sql` (DDL) and `db/migrate/` (data load)

| Member                       | Purpose under VSAM                                    | Modern replacement                              |
|------------------------------|--------------------------------------------------------|-------------------------------------------------|
| `app/jcl/ACCTFILE.jcl`       | DELETE/DEFINE/REPRO of `ACCTDATA.VSAM.KSDS`           | `accounts` table + `db/migrate/loaders.py`      |
| `app/jcl/CARDFILE.jcl`       | DELETE/DEFINE/REPRO of `CARDDATA.VSAM.KSDS` + AIX     | `cards` table (FK to `accounts`)                |
| `app/jcl/XREFFILE.jcl`       | DELETE/DEFINE/REPRO of `CARDXREF.VSAM.KSDS` + AIX     | `card_xref` table with FK indexes               |
| `app/jcl/CUSTFILE.jcl`       | DELETE/DEFINE/REPRO of `CUSTDATA.VSAM.KSDS`           | `customers` table                               |
| `app/jcl/TRANSACT.jcl`       | DELETE/DEFINE/REPRO of `TRANSACT.VSAM.KSDS`           | `transactions` table                            |
| `app/jcl/DALYTRAN.jcl`*      | DELETE/DEFINE of `DALYTRAN.PS`                        | `daily_transactions` table                      |
| `app/jcl/TCATBALF.jcl`*      | DELETE/DEFINE/REPRO of `TCATBALF.VSAM.KSDS`           | `tcat_balances` table                           |
| `app/jcl/DISCGRP.jcl`*       | DELETE/DEFINE/REPRO of `DISCGRP.VSAM.KSDS`            | `disclosure_groups` table                       |
| `app/jcl/TRANTYPE.jcl`*      | DELETE/DEFINE/REPRO of `TRANTYPE.VSAM.KSDS`           | `transaction_types` table                       |
| `app/jcl/TRANCATG.jcl`*      | DELETE/DEFINE/REPRO of `TRANCATG.VSAM.KSDS`           | `transaction_categories` table                  |

`*` Filenames listed for completeness; some are present only as
reference material in the source repository and may not have been
created yet.

## Replaced by online CICS file control changes

| Member                       | Purpose under VSAM                                       | Modern replacement                                                              |
|------------------------------|-----------------------------------------------------------|---------------------------------------------------------------------------------|
| `app/jcl/CLOSEFIL` (where present) | `CEMT SET FIL(...) CLOSED` for batch maintenance | No-op once data lives in the relational database                                |
| `app/jcl/OPENFIL`  (where present) | `CEMT SET FIL(...) OPEN` after batch maintenance | No-op once data lives in the relational database                                |
| In-stream `CEMT SET FIL ... CLO/OPE` blocks inside `CARDFILE.jcl`, `XREFFILE.jcl`, `CUSTFILE.jcl` | CICS file recycling after IDCAMS REPRO | No-op; the relational data store is online for the duration of the maintenance window |

## Replaced by `db/migrate/init_user_security.sh`

| Member                | Purpose under VSAM                                                | Modern replacement                                       |
|-----------------------|-------------------------------------------------------------------|----------------------------------------------------------|
| `app/jcl/ESDSRRDS.jcl`| Bootstraps the user-security ESDS/RRDS files with seed records    | `init_user_security.sh sql ...` populates `user_security` |

## Cut-over checklist

1. Confirm all online and batch programs are on the modernised
   data-access layer (`app/cbl/DBACCESS.cbl`) and that the
   `DAL-USE-RDBMS` feature flag is `ON` in production.
2. Run `db/migrate/run_full_migration.sh` against the latest IDCAMS
   REPRO export and reconcile row counts.
3. Run the integration test suite (`tests/integration/`) against the
   relational database.
4. Stop submitting the JCL members listed above; remove them from any
   scheduler tooling (CA7/Control-M) and finally delete them from this
   repository in a follow-up PR.
