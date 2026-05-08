"""Unit tests for statement generation (CBSTM03A.cbl / CBSTM03B.cbl)."""
from __future__ import annotations

import datetime as dt
from decimal import Decimal

from tests.business_logic import Account, Transaction, build_statement


def test_statement_renders_header_and_total():
    account = Account(
        acct_id=10000000001,
        active_status="Y",
        current_balance=Decimal("250.00"),
        credit_limit=Decimal("1000.00"),
    )
    transactions = [
        Transaction(
            tran_id="T0001",
            card_num="4111111111111111",
            amount=Decimal("100.00"),
            description="Coffee",
            orig_ts=dt.datetime(2026, 5, 1, 10, 0, 0),
        ),
        Transaction(
            tran_id="T0002",
            card_num="4111111111111111",
            amount=Decimal("150.00"),
            description="Books",
            orig_ts=dt.datetime(2026, 5, 2, 11, 0, 0),
        ),
    ]
    output = build_statement(account, transactions)
    assert "ACCT ID 10000000001" in output
    assert "Coffee" in output
    assert "Books" in output
    assert "TOTAL ACTIVITY:          250.00" in output
    assert "CURRENT BALANCE:         250.00" in output


def test_statement_handles_empty_transactions():
    account = Account(
        acct_id=10000000002,
        active_status="Y",
        current_balance=Decimal("0.00"),
        credit_limit=Decimal("1000.00"),
    )
    output = build_statement(account, [])
    assert "TOTAL ACTIVITY:            0.00" in output
    assert "CURRENT BALANCE:           0.00" in output
