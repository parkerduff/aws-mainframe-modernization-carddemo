# Secrets Management — CardDemo

This document describes how the CardDemo application sources sensitive
configuration values (FTP credentials, database connection strings,
service-account API keys, signon passwords, etc.) and how to configure
the supporting secret store on AWS.

> **Plaintext credentials must never be committed to source control.**
> All JCL members and configuration files in this repository expect to
> resolve credentials from a secure store at deploy/submit time.

---

## 1. Inventory of secrets used by CardDemo

| Logical name              | Used by                                | Type            | Notes                                                                 |
|---------------------------|----------------------------------------|-----------------|-----------------------------------------------------------------------|
| `carddemo/ftp/host`       | `app/jcl/FTPJCL.JCL` (`&FTPHOST`)       | string          | Hostname or IP of the target FTP server                                |
| `carddemo/ftp/user`       | `app/jcl/FTPJCL.JCL` (`&FTPUSER`)       | string          | Service account user                                                   |
| `carddemo/ftp/password`   | `app/jcl/FTPJCL.JCL` (`&FTPPASS`)       | string (secret) | Service account password                                               |
| `carddemo/db/admin-url`   | `db/migrate/*` and DAL configuration    | URL             | JDBC URL for the modernised relational store                           |
| `carddemo/db/admin-user`  | `db/migrate/*` and DAL configuration    | string          | Database administrator username                                        |
| `carddemo/db/admin-pass`  | `db/migrate/*` and DAL configuration    | string (secret) | Database administrator password                                        |
| `carddemo/db/app-user`    | runtime application                     | string          | Restricted-permission database user (read/write only on app schema)    |
| `carddemo/db/app-pass`    | runtime application                     | string (secret) | Application database user password                                     |
| `carddemo/admin/password` | `db/migrate/init_user_security.sh`      | string (secret) | Initial password assigned to bootstrap admin accounts                  |
| `carddemo/user/password`  | `db/migrate/init_user_security.sh`      | string (secret) | Initial password assigned to bootstrap regular-user accounts           |

---

## 2. Storage backends

### 2.1 AWS Secrets Manager (recommended for AWS deployments)

A CloudFormation template that provisions all of the secret resources is
provided at `infrastructure/cloudformation/secrets.yaml`.  Each secret
is stored under the JSON path matching the logical name above.

Resolve a secret on the command line:

```bash
aws secretsmanager get-secret-value \
    --secret-id carddemo/db/admin-pass \
    --query SecretString --output text
```

### 2.2 AWS Systems Manager Parameter Store

If your security model requires SSM (e.g. shared with non-secret
configuration), provision parameters with the `--type SecureString`
flag.  The migration scripts under `db/migrate/` accept the
`SECRET_BACKEND=ssm` environment variable to use this backend.

### 2.3 HashiCorp Vault

For deployments that use Vault, expose secrets under the `secret/data/carddemo/...`
path with the same suffixes as the logical names above and set
`SECRET_BACKEND=vault VAULT_ADDR=...` before running the migration scripts.

### 2.4 IBM RACF / TopSecret on z/OS

When running natively on z/OS the JCL members expect symbolic
parameters to be populated from the scheduler's secure parameter
library.  See your enterprise scheduler documentation (CA7, OPC, ESP,
Control-M) for how to integrate it with the Enterprise Password Vault.

---

## 3. Resolving credentials at JCL submission time

`FTPJCL.JCL` no longer contains plaintext credentials; instead it
references three symbolic parameters:

```jcl
//SYSIN DD *
 &FTPHOST
 &FTPUSER
 &FTPPASS
```

These are populated either by:

* **The job submission script** — `scripts/submit_jcl.sh` (provided in
  this repository) reads the secrets from the configured backend and
  injects them into the job stream before submitting via JES.  Sample
  invocation:

  ```bash
  SECRET_BACKEND=secretsmanager ./scripts/submit_jcl.sh app/jcl/FTPJCL.JCL
  ```

* **The enterprise scheduler** — CA7/OPC/Control-M materialises
  symbolic parameters from a secure parameter library managed under
  RACF/TopSecret.  Configure the parameter library entries to map
  `&FTPHOST`, `&FTPUSER`, `&FTPPASS` to the values above.

Whichever path you use, the materialised credentials must be removed
from the in-memory job stream after submission and must never be
written to spool, SYSLOG, or any unencrypted dataset.

---

## 4. Password hashing for the user-security file

The user-security record (copybook `app/cpy/CSUSR01Y.cpy`) now stores a
SHA-256 password hash plus a per-user salt.  The legacy plaintext
`SEC-USR-PWD` field is preserved for binary compatibility but is BLANK
in newly-bootstrapped records and is ignored by `COSGN00C.cbl`.

Hashes are computed by the new subprogram `app/cbl/CHASHPW.cbl`, which
on z/OS calls the ICSF One-Way-Hash service (`CSNBOWH`) and on AWS
Mainframe Modernization the equivalent runtime callable service.

The bootstrap script `db/migrate/init_user_security.sh` populates the
file (or the modernised `user_security` table — see
`db/schema.sql`) using passwords sourced from the secret store; it
never writes plaintext to the resulting dataset.

---

## 5. Local development

For a self-contained development environment:

1. Copy `.env.example` to `.env` (do **not** commit `.env`).
2. Populate the values for the secrets listed in Section 1.
3. Run `direnv allow` (or `source .env`) to expose them to the
   migration scripts and CI tooling.
4. Run `db/migrate/init_user_security.sh` to populate the
   user-security table with hashed passwords.

The `.gitignore` file blocks `.env`, `*.pem`, `credentials*.json`,
`secrets*.yml`, and other common credential file patterns from being
committed.
