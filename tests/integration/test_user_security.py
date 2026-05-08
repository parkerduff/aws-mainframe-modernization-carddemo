"""Bootstrap the user_security table and verify sign-on parity with COSGN00C."""
from __future__ import annotations

import pytest

from tests.business_logic import UserRecord, hash_password, verify_signon


@pytest.fixture
def seeded_users(conn):
    psycopg = pytest.importorskip("psycopg")
    rows = []
    seeds = [
        ("ADMIN001", "MARGARET", "GOLD", "A", "ADMIN001SALT0001"),
        ("USER0001", "LAWRENCE", "THOMAS", "U", "USER0001SALT0001"),
    ]
    with conn.cursor() as cur:
        for user_id, fname, lname, utype, salt in seeds:
            digest = hash_password("PASSWORD", salt)
            cur.execute(
                """
                INSERT INTO user_security (
                    user_id, first_name, last_name, user_type,
                    password_hash, password_salt
                ) VALUES (%s, %s, %s, %s, %s, %s)
                ON CONFLICT (user_id) DO UPDATE SET
                    password_hash = EXCLUDED.password_hash,
                    password_salt = EXCLUDED.password_salt,
                    user_type     = EXCLUDED.user_type
                """,
                (user_id, fname, lname, utype, digest, salt),
            )
            rows.append((user_id, utype, salt, digest))
    yield rows


def _users_dict(rows):
    return {
        user_id: UserRecord(
            user_id=user_id,
            user_type=utype,
            password_hash=digest,
            password_salt=salt,
        )
        for user_id, utype, salt, digest in rows
    }


def test_admin_signon_routes_correctly(seeded_users):
    ok, target = verify_signon("ADMIN001", "PASSWORD", _users_dict(seeded_users))
    assert ok is True
    assert target == "COADM01C"


def test_regular_user_signon_routes_correctly(seeded_users):
    ok, target = verify_signon("USER0001", "PASSWORD", _users_dict(seeded_users))
    assert ok is True
    assert target == "COMEN01C"


def test_invalid_password_rejected(seeded_users):
    ok, target = verify_signon("USER0001", "WRONG", _users_dict(seeded_users))
    assert ok is False
    assert target == ""


def test_password_field_only_stores_hashes(conn, seeded_users):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT user_id, password_hash, password_salt FROM user_security"
        )
        rows = cur.fetchall()
    assert rows
    for user_id, digest, salt in rows:
        assert len(digest) == 64
        int(digest, 16)              # must be valid lowercase hex
        assert digest.lower() == digest
        assert "PASSWORD" not in digest
        assert salt and not salt.isspace()
