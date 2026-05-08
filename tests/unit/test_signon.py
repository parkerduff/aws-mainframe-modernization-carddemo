"""Unit tests for the sign-on rules implemented in COSGN00C.cbl."""
from __future__ import annotations

import pytest

from tests.business_logic import UserRecord, hash_password, verify_signon


@pytest.fixture
def users():
    salt_a = "ADMIN001SALT0001"
    salt_u = "USER0001SALT0001"
    return {
        "ADMIN001": UserRecord(
            user_id="ADMIN001",
            user_type="A",
            password_hash=hash_password("PASSWORD", salt_a),
            password_salt=salt_a,
        ),
        "USER0001": UserRecord(
            user_id="USER0001",
            user_type="U",
            password_hash=hash_password("PASSWORD", salt_u),
            password_salt=salt_u,
        ),
    }


def test_signon_admin_routes_to_admin_menu(users):
    ok, target = verify_signon("ADMIN001", "PASSWORD", users)
    assert ok is True
    assert target == "COADM01C"


def test_signon_user_routes_to_main_menu(users):
    ok, target = verify_signon("USER0001", "PASSWORD", users)
    assert ok is True
    assert target == "COMEN01C"


def test_signon_wrong_password_rejected(users):
    ok, target = verify_signon("USER0001", "WRONG", users)
    assert ok is False
    assert target == ""


def test_signon_unknown_user_rejected(users):
    ok, target = verify_signon("NOBODY  ", "PASSWORD", users)
    assert ok is False
    assert target == ""


def test_hash_is_salted(users):
    """A password reused across two users must produce two distinct hashes."""
    a = users["ADMIN001"]
    u = users["USER0001"]
    assert a.password_hash != u.password_hash


def test_hash_is_deterministic():
    digest_1 = hash_password("PASSWORD", "ADMIN001SALT0001")
    digest_2 = hash_password("PASSWORD", "ADMIN001SALT0001")
    assert digest_1 == digest_2
    assert len(digest_1) == 64
    int(digest_1, 16)  # must be valid hex
