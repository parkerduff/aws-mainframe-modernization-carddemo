"""Verify the modernised schema applies cleanly and has the expected shape."""
from __future__ import annotations

import pytest


EXPECTED_TABLES = {
    "transaction_types",
    "transaction_categories",
    "customers",
    "accounts",
    "cards",
    "card_xref",
    "disclosure_groups",
    "transactions",
    "daily_transactions",
    "tcat_balances",
    "user_security",
    "audit_log",
}


def test_all_expected_tables_present(conn):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT table_name
              FROM information_schema.tables
             WHERE table_schema = 'public'
            """
        )
        actual = {row[0] for row in cur.fetchall()}
    missing = EXPECTED_TABLES - actual
    assert not missing, f"missing tables: {missing}"


def test_user_security_constraints(conn):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT column_name, data_type, character_maximum_length
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'user_security'
             ORDER BY ordinal_position
            """
        )
        cols = {name: (dtype, length) for name, dtype, length in cur.fetchall()}

    assert cols["user_id"][1] == 8
    assert cols["password_hash"][1] == 64
    assert cols["password_salt"][1] == 16
    assert cols["user_type"][1] == 1


def test_card_xref_foreign_keys(conn):
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT confrelid::regclass::text
              FROM pg_constraint
             WHERE conrelid = 'card_xref'::regclass
               AND contype  = 'f'
            """
        )
        targets = {row[0] for row in cur.fetchall()}
    assert {"cards", "customers", "accounts"}.issubset(targets)
