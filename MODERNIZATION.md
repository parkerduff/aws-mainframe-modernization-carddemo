# CardDemo Modernization Reference

This document is the design companion to the modernization PR.  It
explains how the security hardening, VSAM-to-relational migration and
the CI/CD pipeline fit together, and serves as the single canonical
crosswalk between the legacy and modernised code paths.

> Source documents:
> * `config/secrets-config.md`
> * `db/schema.sql`
> * `db/migrate/README.md`
> * `db/DEPRECATED_VSAM_JCL.md`
> * `infrastructure/README.md`
> * `tests/README.md`

## 1. Security hardening

| Concern                                | Before                                                                 | After                                                                                                         |
|----------------------------------------|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| FTP credentials in `app/jcl/FTPJCL.JCL`| Hostname, user and password embedded as in-stream literals             | Symbolic parameters `&FTPHOST`, `&FTPUSER`, `&FTPPASS` materialised at submission time from a secret store    |
| Plaintext seed passwords in `ESDSRRDS.jcl` | `PASSWORDA` / `PASSWORDU` written as in-stream data, alongside the user record | 160-byte records with SHA-256 password hash + per-user salt; bootstrap password sourced from the secret store |
| Plaintext compare in `COSGN00C.cbl`    | `IF SEC-USR-PWD = WS-USER-PWD`                                         | New subprogram `CHASHPW` produces SHA-256(password + salt); compare against `SEC-USR-PWD-HASH`                |
| Record layout `CSUSR01Y.cpy`           | 80 bytes, password 8 bytes                                             | 160 bytes, adds `SEC-USR-PWD-HASH PIC X(64)` and `SEC-USR-PWD-SALT PIC X(16)`                                 |
| Secrets management                     | Implicit / undocumented                                                | `config/secrets-config.md` documents AWS Secrets Manager / SSM / Vault / RACF integrations                    |
| Repository hygiene                     | `.gitignore` ignored sample dirs only                                  | Adds `*.pem`, `*.key`, `.env*`, `credentials*.json`, `secrets*.yml`, `*.password` etc.                        |

## 2. VSAM → Relational migration

### 2a. Schema

`db/schema.sql` defines the modernised relational schema.  Every VSAM
KSDS becomes a table with a primary key matching the original
`RECORD KEY`; secondary AIX paths become indexes.  The schema also
introduces:

* an `audit_log` table for cross-cutting auditing.
* a `daily_transactions` table backing the `DALYTRAN.PS` flow with a
  `posted` flag so the batch posting job can be re-run safely.
* a normalised `tcat_balances` table whose composite PK matches the
  VSAM key (acct_id, tran_type_cd, tran_cat_cd).

### 2b. Data migration

`db/migrate/loaders.py` declares one `Entity` per VSAM file with a
field list that mirrors the corresponding copybook.  The script can
emit either CSV staging files (default) or SQL `INSERT` scripts.  The
end-to-end orchestrator is `db/migrate/run_full_migration.sh` which
drives the loader for every entity in dependency order.

### 2c. Data Access Layer (DAL)

`app/cbl/DBACCESS.cbl` is a new shared subprogram.  Callers populate
copybook `DBACCESY.cpy` and request a `READ` / `WRITE` / `UPDATE` /
`DELETE` / `STARTBR` operation against a named entity.  The DAL:

1. Translates the request to embedded SQL (EXEC SQL) when the feature
   flag `WS-USE-RDBMS` is ON.
2. Falls back to the original VSAM file-control programs when the flag
   is OFF — supporting the strangler-fig migration without a hard
   cut-over.
3. Maps DB SQLCODE values back to CICS-style RESP/RESP2 codes
   (`DAL-RESP-NORMAL`, `DAL-RESP-NOTFND`, `DAL-RESP-DUPREC`, ...) so
   existing callers can continue to use the familiar `EVALUATE` style.

### 2d. Migrating online CICS programs

Each `EXEC CICS READ/WRITE/REWRITE FILE(...)` becomes a `CALL 'DBACCESS'
USING DAL-COMMAREA`.  The crosswalk below documents the complete
substitution for the eight programs called out in the modernisation
plan; only `COSGN00C` is fully refactored in this PR (security
hardening).  The remaining programs are listed for follow-up PRs so
the migration stays reviewable.

| Program        | VSAM File              | DAL Entity   | Notes                                            |
|----------------|------------------------|--------------|--------------------------------------------------|
| `COSGN00C.cbl` | `USRSEC` (ESDS)        | `USRSEC`     | Done in this PR; uses `CHASHPW` for password verify |
| `COACTVWC.cbl` | `ACCTDAT` (KSDS)       | `ACCOUNT`    | Replace `EXEC CICS READ` with `CALL 'DBACCESS'`     |
| `COACTUPC.cbl` | `ACCTDAT` (KSDS)       | `ACCOUNT`    | Read for update -> `SELECT ... FOR UPDATE`          |
| `COTRN00C.cbl` | `TRANSACT` (KSDS)      | `TRANSACT`   | Browse -> cursor with `STARTBR/READNEXT` mapped     |
| `COTRN01C.cbl` | `TRANSACT` (KSDS)      | `TRANSACT`   | Single read-by-key                                  |
| `COTRN02C.cbl` | `TRANSACT` + `DALYTRAN`| `TRANSACT`/`DALYTRAN` | Atomic insert + tcat_balances update      |
| `COCRDSLC.cbl` | `CARDDAT`              | `CARD`       | Single read-by-key                                  |
| `COCRDLIC.cbl` | `CARDDAT` + AIX        | `CARD`       | Browse over `card_acct_id` index                    |

A small skeleton change pattern, shown as a diff, looks like:

```cobol
*> ----- before -----
*EXEC CICS READ FILE('CARDDAT')
*    INTO  (CARD-RECORD)
*    RIDFLD(WS-CARD-NUM)
*    RESP  (WS-RESP-CD)
*END-EXEC.

*> ----- after  -----
SET DAL-OP-READ TO TRUE
SET DAL-EN-CARD TO TRUE
MOVE WS-CARD-NUM TO DAL-KEY
MOVE 16          TO DAL-KEY-LEN
CALL 'DBACCESS' USING DAL-COMMAREA
MOVE DAL-RESP-CD TO WS-RESP-CD
MOVE DAL-DATA-AREA(1:LENGTH OF CARD-RECORD) TO CARD-RECORD
```

### 2e. Deprecated JCL

See `db/DEPRECATED_VSAM_JCL.md` for the full list of JCL members that
can be retired once the relational migration is complete.

## 3. CI/CD

* `.github/workflows/ci.yml` validates COBOL syntax (GnuCOBOL),
  validates the SQL schema by parsing it with `sqlglot`, runs the
  Python unit and integration test suites, and exposes a
  `deploy-aws-mainframe-modernization` job that the release manager
  can trigger manually.
* Unit tests live under `tests/unit/`; integration tests under
  `tests/integration/`.  They run on the GitHub-hosted Linux runner
  and complete in <1 minute.
* `infrastructure/cloudformation/` contains AWS CloudFormation
  templates for the modernised stack: networking, the relational
  database, secrets, and the AWS Mainframe Modernization environment.
* `CONTRIBUTING.md` documents the COBOL coding standards used in this
  repo (paragraph naming, error handling, copybook imports, etc.) so
  follow-up modernisation PRs stay consistent.
