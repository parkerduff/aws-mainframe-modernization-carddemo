"""Integration-test fixtures.

Each integration test runs inside its own database transaction and
rolls it back at teardown so the DB stays clean for parallel runs.
The connection string is sourced from the ``DB_DSN`` environment
variable; if it is missing the entire integration suite is skipped so
unit-only CI jobs continue to pass.
"""
from __future__ import annotations

import os
import pathlib

import pytest

REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]


@pytest.fixture(scope="session")
def db_dsn() -> str:
    dsn = os.environ.get("DB_DSN")
    if not dsn:
        pytest.skip("DB_DSN environment variable is not set")
    return dsn


@pytest.fixture(scope="session")
def schema_sql() -> str:
    return (REPO_ROOT / "db" / "schema.sql").read_text(encoding="utf-8")


@pytest.fixture
def conn(db_dsn):
    psycopg = pytest.importorskip("psycopg")
    connection = psycopg.connect(db_dsn, autocommit=False)
    try:
        yield connection
    finally:
        connection.rollback()
        connection.close()
