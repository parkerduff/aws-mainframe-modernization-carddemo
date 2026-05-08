"""Per-entity fixed-width record definitions and SQL load helpers.

Each entity here corresponds to one of the operational VSAM files
exported as a sequential PS file by the mainframe.  The field
definitions mirror the COBOL copybooks under app/cpy/ exactly so the
parser can be regression-tested against the bundled sample data.

The module is deliberately framework-free: callers can either consume
``ENTITIES`` directly to produce CSV staging files, or use
``build_insert_statements`` to emit a SQL script that targets the
schema in db/schema.sql.
"""
from __future__ import annotations

import argparse
import csv
import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence

from .parse_fixed_width import (
    DATE,
    Field,
    INT,
    NUM2,
    OPT,
    TRIM,
    TS,
    parse_records,
)

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class Entity:
    name: str
    table: str
    record_length: int
    fields: Sequence[Field]


# CVACT01Y - account record (300 bytes)
ACCOUNTS = Entity(
    name="accounts",
    table="accounts",
    record_length=300,
    fields=(
        Field("acct_id", 11, INT),
        Field("acct_active_status", 1, TRIM),
        Field("acct_curr_bal", 12, NUM2),
        Field("acct_credit_limit", 12, NUM2),
        Field("acct_cash_credit_limit", 12, NUM2),
        Field("acct_open_date", 10, DATE),
        Field("acct_expiration_date", 10, DATE),
        Field("acct_reissue_date", 10, DATE),
        Field("acct_curr_cyc_credit", 12, NUM2),
        Field("acct_curr_cyc_debit", 12, NUM2),
        Field("acct_addr_zip", 10, OPT),
        Field("acct_group_id", 10, TRIM),
        Field("_filler", 178, OPT),
    ),
)

# CVACT02Y - card record (150 bytes)
CARDS = Entity(
    name="cards",
    table="cards",
    record_length=150,
    fields=(
        Field("card_num", 16, TRIM),
        Field("card_acct_id", 11, INT),
        Field("card_cvv_cd", 3, INT),
        Field("card_embossed_name", 50, OPT),
        Field("card_expiration_date", 10, DATE),
        Field("card_active_status", 1, TRIM),
        Field("_filler", 59, OPT),
    ),
)

# CVCUS01Y - customer record (500 bytes)
CUSTOMERS = Entity(
    name="customers",
    table="customers",
    record_length=500,
    fields=(
        Field("cust_id", 9, INT),
        Field("cust_first_name", 25, OPT),
        Field("cust_middle_name", 25, OPT),
        Field("cust_last_name", 25, OPT),
        Field("cust_addr_line_1", 50, OPT),
        Field("cust_addr_line_2", 50, OPT),
        Field("cust_addr_line_3", 50, OPT),
        Field("cust_addr_state_cd", 2, OPT),
        Field("cust_addr_country_cd", 3, OPT),
        Field("cust_addr_zip", 10, OPT),
        Field("cust_phone_num_1", 15, OPT),
        Field("cust_phone_num_2", 15, OPT),
        Field("cust_ssn", 9, INT),
        Field("cust_govt_issued_id", 20, OPT),
        Field("cust_dob", 10, DATE),
        Field("cust_eft_account_id", 10, OPT),
        Field("cust_pri_card_holder_ind", 1, TRIM),
        Field("cust_fico_credit_score", 3, INT),
        Field("_filler", 168, OPT),
    ),
)

# CVACT03Y - card cross-reference record (50 bytes)
CARD_XREF = Entity(
    name="card_xref",
    table="card_xref",
    record_length=50,
    fields=(
        Field("xref_card_num", 16, TRIM),
        Field("xref_cust_id", 9, INT),
        Field("xref_acct_id", 11, INT),
        Field("_filler", 14, OPT),
    ),
)

# CVTRA05Y - online transaction record (350 bytes)
TRANSACTIONS = Entity(
    name="transactions",
    table="transactions",
    record_length=350,
    fields=(
        Field("tran_id", 16, TRIM),
        Field("tran_type_cd", 2, TRIM),
        Field("tran_cat_cd", 4, INT),
        Field("tran_source", 10, OPT),
        Field("tran_desc", 100, OPT),
        Field("tran_amt", 11, NUM2),
        Field("tran_merchant_id", 9, INT),
        Field("tran_merchant_name", 50, OPT),
        Field("tran_merchant_city", 50, OPT),
        Field("tran_merchant_zip", 10, OPT),
        Field("tran_card_num", 16, TRIM),
        Field("tran_orig_ts", 26, TS),
        Field("tran_proc_ts", 26, TS),
        Field("_filler", 20, OPT),
    ),
)

# CVTRA06Y - daily transaction record (350 bytes)
DAILY_TRANSACTIONS = Entity(
    name="daily_transactions",
    table="daily_transactions",
    record_length=350,
    fields=(
        Field("dalytran_id", 16, TRIM),
        Field("dalytran_type_cd", 2, TRIM),
        Field("dalytran_cat_cd", 4, INT),
        Field("dalytran_source", 10, OPT),
        Field("dalytran_desc", 100, OPT),
        Field("dalytran_amt", 11, NUM2),
        Field("dalytran_merchant_id", 9, INT),
        Field("dalytran_merchant_name", 50, OPT),
        Field("dalytran_merchant_city", 50, OPT),
        Field("dalytran_merchant_zip", 10, OPT),
        Field("dalytran_card_num", 16, TRIM),
        Field("dalytran_orig_ts", 26, TS),
        Field("dalytran_proc_ts", 26, TS),
        Field("_filler", 20, OPT),
    ),
)

# CVTRA01Y - transaction category balance (50 bytes)
TCAT_BALANCES = Entity(
    name="tcat_balances",
    table="tcat_balances",
    record_length=50,
    fields=(
        Field("trancat_acct_id", 11, INT),
        Field("trancat_type_cd", 2, TRIM),
        Field("trancat_cd", 4, INT),
        Field("tran_cat_bal", 11, NUM2),
        Field("_filler", 22, OPT),
    ),
)

# CVTRA02Y - disclosure group (50 bytes)
DISCLOSURE_GROUPS = Entity(
    name="disclosure_groups",
    table="disclosure_groups",
    record_length=50,
    fields=(
        Field("acct_group_id", 10, TRIM),
        Field("tran_type_cd", 2, TRIM),
        Field("tran_cat_cd", 4, INT),
        Field("int_rate", 6, NUM2),
        Field("_filler", 28, OPT),
    ),
)

# CVTRA03Y - transaction type (60 bytes)
TRANSACTION_TYPES = Entity(
    name="transaction_types",
    table="transaction_types",
    record_length=60,
    fields=(
        Field("tran_type_cd", 2, TRIM),
        Field("tran_type_desc", 50, OPT),
        Field("_filler", 8, OPT),
    ),
)

# CVTRA04Y - transaction category (60 bytes)
TRANSACTION_CATEGORIES = Entity(
    name="transaction_categories",
    table="transaction_categories",
    record_length=60,
    fields=(
        Field("tran_type_cd", 2, TRIM),
        Field("tran_cat_cd", 4, INT),
        Field("tran_cat_type_desc", 50, OPT),
        Field("_filler", 4, OPT),
    ),
)

# CSUSR01Y - user security record, modernised 160-byte format
USER_SECURITY = Entity(
    name="user_security",
    table="user_security",
    record_length=160,
    fields=(
        Field("user_id", 8, TRIM),
        Field("first_name", 20, OPT),
        Field("last_name", 20, OPT),
        Field("_legacy_pwd", 8, OPT),
        Field("user_type", 1, TRIM),
        Field("_filler", 23, OPT),
        Field("password_hash", 64, TRIM),
        Field("password_salt", 16, TRIM),
    ),
)


ENTITIES: Dict[str, Entity] = {
    e.name: e
    for e in (
        TRANSACTION_TYPES,        # parents first, FK ordering
        TRANSACTION_CATEGORIES,
        CUSTOMERS,
        ACCOUNTS,
        CARDS,
        CARD_XREF,
        DISCLOSURE_GROUPS,
        TRANSACTIONS,
        DAILY_TRANSACTIONS,
        TCAT_BALANCES,
        USER_SECURITY,
    )
}


def quote(value: object) -> str:
    """Render *value* as a SQL literal."""
    if value is None or value == "":
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return str(value)
    text = str(value).replace("'", "''")
    return f"'{text}'"


def build_insert_statements(entity: Entity, source: Path) -> Iterable[str]:
    """Yield ``INSERT`` statements for *entity* read from *source*."""
    columns = [field.name for field in entity.fields if not field.name.startswith("_")]
    column_list = ", ".join(columns)
    for row in parse_records(source, entity.fields, entity.record_length):
        values = ", ".join(quote(row[col]) for col in columns)
        yield f"INSERT INTO {entity.table} ({column_list}) VALUES ({values});"


def write_sql(entity: Entity, source: Path, target: Path) -> int:
    target.parent.mkdir(parents=True, exist_ok=True)
    count = 0
    with target.open("w", encoding="utf-8") as out:
        out.write(f"-- Auto-generated load script for {entity.table}\n")
        out.write("BEGIN;\n")
        for stmt in build_insert_statements(entity, source):
            out.write(stmt + "\n")
            count += 1
        out.write("COMMIT;\n")
    return count


def write_csv(entity: Entity, source: Path, target: Path) -> int:
    target.parent.mkdir(parents=True, exist_ok=True)
    columns = [f.name for f in entity.fields if not f.name.startswith("_")]
    count = 0
    with target.open("w", encoding="utf-8", newline="") as out:
        writer = csv.DictWriter(out, fieldnames=columns)
        writer.writeheader()
        for row in parse_records(source, entity.fields, entity.record_length):
            writer.writerow({c: ("" if row[c] is None else row[c]) for c in columns})
            count += 1
    return count


def _cli() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("entity", choices=sorted(ENTITIES))
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    parser.add_argument("--format", choices=("sql", "csv"), default="sql")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO)
    entity = ENTITIES[args.entity]
    if args.format == "sql":
        rows = write_sql(entity, args.source, args.target)
    else:
        rows = write_csv(entity, args.source, args.target)
    LOGGER.info("Wrote %d %s rows to %s", rows, entity.table, args.target)


if __name__ == "__main__":  # pragma: no cover
    _cli()
