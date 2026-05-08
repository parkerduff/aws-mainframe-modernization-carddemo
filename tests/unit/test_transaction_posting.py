"""Unit tests for the daily transaction posting rules in CBTRN02C.cbl."""
from __future__ import annotations

from decimal import Decimal

import pytest

from tests.business_logic import (
    Account,
    DailyTransaction,
    REJECT_INACTIVE,
    REJECT_OVER_LIMIT,
    REJECT_UNKNOWN_CARD,
    post_transaction,
)


@pytest.fixture
def account():
    return Account(
        acct_id=10000000001,
        active_status="Y",
        current_balance=Decimal("100.00"),
        credit_limit=Decimal("500.00"),
    )


@pytest.fixture
def card_lookup(account):
    return {"4111111111111111": account.acct_id}


def test_happy_path_updates_balance(account, card_lookup):
    tx = DailyTransaction(
        tran_id="TX1", card_num="4111111111111111",
        amount=Decimal("50.00"), type_cd="05", cat_cd=10,
    )
    posted, reason = post_transaction(tx, account, card_to_account=card_lookup)
    assert posted is True
    assert reason is None
    assert account.current_balance == Decimal("150.00")


def test_inactive_account_rejects(account, card_lookup):
    account.active_status = "N"
    tx = DailyTransaction("TX1", "4111111111111111", Decimal("10"), "05", 10)
    posted, reason = post_transaction(tx, account, card_to_account=card_lookup)
    assert posted is False
    assert reason == REJECT_INACTIVE
    assert account.current_balance == Decimal("100.00")


def test_over_limit_rejects(account, card_lookup):
    tx = DailyTransaction("TX1", "4111111111111111", Decimal("9999.00"), "05", 10)
    posted, reason = post_transaction(tx, account, card_to_account=card_lookup)
    assert posted is False
    assert reason == REJECT_OVER_LIMIT
    assert account.current_balance == Decimal("100.00")


def test_unknown_card_rejects(account):
    tx = DailyTransaction("TX1", "9999999999999999", Decimal("10"), "05", 10)
    posted, reason = post_transaction(tx, account, card_to_account={})
    assert posted is False
    assert reason == REJECT_UNKNOWN_CARD


def test_credit_balance_allowed(account, card_lookup):
    """Refunds (negative amounts) should always post if the card is known."""
    tx = DailyTransaction("TX1", "4111111111111111", Decimal("-25.00"), "06", 10)
    posted, reason = post_transaction(tx, account, card_to_account=card_lookup)
    assert posted is True
    assert reason is None
    assert account.current_balance == Decimal("75.00")
