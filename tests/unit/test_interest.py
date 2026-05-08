"""Unit tests for the interest-accrual rules implemented in CBACT04C.cbl."""
from __future__ import annotations

from decimal import Decimal

import pytest

from tests.business_logic import (
    DisclosureGroup,
    TcatBalance,
    monthly_interest,
    total_interest_for_account,
)


def _money(value):
    return Decimal(value).quantize(Decimal("0.01"))


def test_zero_balance_yields_zero_interest():
    assert monthly_interest(_money("0"), Decimal("18.99")) == _money("0")


def test_negative_balance_yields_zero_interest():
    """Credit balances must not earn interest; matches CBACT04C behaviour."""
    assert monthly_interest(_money("-100.00"), Decimal("18.99")) == _money("0")


def test_typical_apr_calculation():
    # $1,000 at 18.99% APR for one month
    # = 1000 * 18.99 / 1200 = 15.825 -> rounded to 15.83
    assert monthly_interest(_money("1000.00"), Decimal("18.99")) == _money("15.83")


def test_max_balance_calculation():
    """Sanity-check at the operational maximum balance."""
    rate = Decimal("24.99")
    result = monthly_interest(_money("9999999.99"), rate)
    expected = (Decimal("9999999.99") * rate / Decimal(1200)).quantize(Decimal("0.01"))
    assert result == expected


def test_total_interest_sums_categories():
    balances = [
        TcatBalance(acct_id=1, tran_type_cd="01", tran_cat_cd=10, balance=_money("500.00")),
        TcatBalance(acct_id=1, tran_type_cd="01", tran_cat_cd=20, balance=_money("250.00")),
        TcatBalance(acct_id=1, tran_type_cd="01", tran_cat_cd=99, balance=_money("0.00")),
    ]
    rate_lookup = {
        ("01", "01", 10): Decimal("18.99"),
        ("01", "01", 20): Decimal("12.50"),
    }
    total = total_interest_for_account(balances, rate_lookup)
    # 500.00 * 18.99 / 1200 = 7.9125 -> 7.91
    # 250.00 * 12.50 / 1200 = 2.6042 -> 2.60
    assert total == _money("7.91") + _money("2.60")
