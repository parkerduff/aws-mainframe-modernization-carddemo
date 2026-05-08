# Tests

CardDemo's modernised test suite is split into three layers:

| Layer        | Path                | What it covers                                                                 |
|--------------|---------------------|--------------------------------------------------------------------------------|
| Lint         | `tests/lint/`       | JCL syntax, SQL schema validity, plaintext-secret scanning                     |
| Unit         | `tests/unit/`       | Pure Python re-implementations of the COBOL business logic (interest, posting, signon, statement) |
| Integration  | `tests/integration/`| End-to-end batch flow, migration-script correctness against a Postgres service |

The Python unit tests deliberately re-encode the business rules from
the original COBOL (e.g., `CBACT04C.cbl` interest calculation,
`CBTRN02C.cbl` posting rejections) so we can run them on any GitHub
runner without provisioning a mainframe.  When the COBOL is later
ported to Java (or another modern stack) by AWS Mainframe
Modernization, these tests act as a regression harness for the new
implementation.

## Running locally

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r tests/requirements.txt

pytest tests/unit -v             # unit tests, no DB required
pytest tests/integration -v       # integration tests, requires Postgres
```

The integration tests expect a Postgres database whose connection
string is in the `DB_DSN` environment variable.  The CI workflow
provisions a Postgres service container automatically; for local
development you can run:

```bash
docker run --rm -d \
    -e POSTGRES_USER=carddemo \
    -e POSTGRES_PASSWORD=carddemo \
    -e POSTGRES_DB=carddemo \
    -p 5432:5432 postgres:15
export DB_DSN=postgresql://carddemo:carddemo@localhost:5432/carddemo
psql "$DB_DSN" -f db/schema.sql
```

## Test fixtures

`tests/fixtures/` contains tiny fixed-width samples that mirror the
real PS exports.  Each loader test parses the fixture using the same
`Entity` definition that drives the production migration job.
