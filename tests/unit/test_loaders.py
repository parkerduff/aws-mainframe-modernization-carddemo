"""Unit tests for the fixed-width record loaders."""
from __future__ import annotations

import pathlib
from decimal import Decimal

import pytest

from db.migrate import (
    ACCOUNTS,
    CARDS,
    CARD_XREF,
    USER_SECURITY,
    parse_records,
)

FIXTURES = pathlib.Path(__file__).resolve().parents[1] / "fixtures"


def _records(entity, fixture_name):
    return list(parse_records(FIXTURES / fixture_name, entity.fields, entity.record_length))


def test_account_records_round_trip():
    rows = _records(ACCOUNTS, "accounts.dat")
    assert len(rows) == 2
    first = rows[0]
    assert first["acct_id"] == 10000000001
    assert first["acct_active_status"] == "Y"
    assert first["acct_curr_bal"] == Decimal("100.00")
    assert first["acct_credit_limit"] == Decimal("500.00")


def test_card_records_round_trip():
    rows = _records(CARDS, "cards.dat")
    assert len(rows) == 2
    first = rows[0]
    assert first["card_num"] == "4111111111111111"
    assert first["card_acct_id"] == 10000000001
    assert first["card_active_status"] == "Y"


def test_card_xref_round_trip():
    rows = _records(CARD_XREF, "card_xref.dat")
    assert len(rows) == 2
    first = rows[0]
    assert first["xref_card_num"] == "4111111111111111"
    assert first["xref_acct_id"] == 10000000001
    assert first["xref_cust_id"] == 100000001


def test_user_security_round_trip():
    rows = _records(USER_SECURITY, "user_security.dat")
    assert len(rows) == 2
    admin, user = rows
    assert admin["user_id"] == "ADMIN001"
    assert admin["user_type"] == "A"
    assert len(admin["password_hash"]) == 64
    assert admin["password_salt"] == "ADMIN001SALT0001"
    assert user["user_id"] == "USER0001"
    assert user["user_type"] == "U"


def test_record_length_is_validated():
    """A bad field declaration must raise ValueError before any I/O."""
    from db.migrate.loaders import Entity, Field
    from db.migrate.parse_fixed_width import TRIM

    bad = Entity(
        name="bad",
        table="bad",
        record_length=10,
        fields=(Field("a", 5, TRIM),),
    )
    with pytest.raises(ValueError):
        list(parse_records(FIXTURES / "user_security.dat", bad.fields, bad.record_length))
