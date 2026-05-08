"""End-to-end test of the migration loaders against the real schema."""
from __future__ import annotations

import pathlib

import pytest

from db.migrate import (
    ACCOUNTS,
    CARDS,
    CARD_XREF,
    USER_SECURITY,
    parse_records,
)

FIXTURES = pathlib.Path(__file__).resolve().parents[1] / "fixtures"


def _filter_columns(entity, row):
    return {k: v for k, v in row.items() if not k.startswith("_")}


def test_load_accounts_then_cards_then_xref(conn):
    with conn.cursor() as cur:
        for row in parse_records(FIXTURES / "accounts.dat", ACCOUNTS.fields, ACCOUNTS.record_length):
            payload = _filter_columns(ACCOUNTS, row)
            columns = ", ".join(payload)
            placeholders = ", ".join(["%s"] * len(payload))
            cur.execute(
                f"INSERT INTO accounts ({columns}) VALUES ({placeholders})",
                tuple(payload.values()),
            )
        for row in parse_records(FIXTURES / "cards.dat", CARDS.fields, CARDS.record_length):
            payload = _filter_columns(CARDS, row)
            columns = ", ".join(payload)
            placeholders = ", ".join(["%s"] * len(payload))
            cur.execute(
                f"INSERT INTO cards ({columns}) VALUES ({placeholders})",
                tuple(payload.values()),
            )
        # customers must exist before card_xref
        cur.execute(
            """
            INSERT INTO customers (cust_id, cust_first_name, cust_last_name)
            VALUES (100000001, 'JANE', 'DOE'), (100000002, 'JOHN', 'DOE')
            """
        )
        for row in parse_records(FIXTURES / "card_xref.dat", CARD_XREF.fields, CARD_XREF.record_length):
            payload = _filter_columns(CARD_XREF, row)
            columns = ", ".join(payload)
            placeholders = ", ".join(["%s"] * len(payload))
            cur.execute(
                f"INSERT INTO card_xref ({columns}) VALUES ({placeholders})",
                tuple(payload.values()),
            )

        cur.execute("SELECT COUNT(*) FROM accounts")
        assert cur.fetchone()[0] == 2
        cur.execute("SELECT COUNT(*) FROM cards")
        assert cur.fetchone()[0] == 2
        cur.execute("SELECT COUNT(*) FROM card_xref")
        assert cur.fetchone()[0] == 2


def test_load_user_security_records(conn):
    with conn.cursor() as cur:
        for row in parse_records(FIXTURES / "user_security.dat", USER_SECURITY.fields, USER_SECURITY.record_length):
            payload = _filter_columns(USER_SECURITY, row)
            columns = ", ".join(payload)
            placeholders = ", ".join(["%s"] * len(payload))
            cur.execute(
                f"INSERT INTO user_security ({columns}) VALUES ({placeholders})",
                tuple(payload.values()),
            )
        cur.execute("SELECT user_id, user_type FROM user_security ORDER BY user_id")
        rows = cur.fetchall()
    assert rows == [("ADMIN001", "A"), ("USER0001", "U")]
