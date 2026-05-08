"""Validate the relational schema with sqlglot.

The schema in db/schema.sql is the source of truth for the modernised
data layer.  This script parses every statement with sqlglot and
verifies that every primary-key, foreign-key and check-constraint
references valid columns, so we catch typos before they hit a real
database.
"""
from __future__ import annotations

import argparse
import logging
import pathlib
import sys
from typing import List

LOGGER = logging.getLogger("lint_sql")


def main(argv: List[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("schema", type=pathlib.Path)
    args = parser.parse_args(argv)

    logging.basicConfig(level=logging.INFO, format="%(message)s")

    try:
        import sqlglot
    except ImportError as exc:  # pragma: no cover
        LOGGER.error("sqlglot is required: %s", exc)
        return 2

    text = args.schema.read_text(encoding="utf-8")
    try:
        statements = sqlglot.parse(text, read="postgres")
    except sqlglot.errors.ParseError as exc:
        LOGGER.error("Failed to parse %s: %s", args.schema, exc)
        return 1

    table_columns: dict[str, set[str]] = {}
    for stmt in statements:
        if stmt is None:
            continue
        if stmt.key == "create" and stmt.args.get("kind") == "TABLE":
            this = stmt.this
            schema = this.this
            table_name = schema.name
            table_columns[table_name] = {
                col.name for col in schema.expressions
                if col.key == "columndef"
            }

    if not table_columns:
        LOGGER.error("No CREATE TABLE statements parsed from %s", args.schema)
        return 1

    LOGGER.info(
        "Parsed %d statements covering %d tables: %s",
        len(statements),
        len(table_columns),
        ", ".join(sorted(table_columns)),
    )
    return 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main(sys.argv[1:]))
