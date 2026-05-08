"""Pure-Python re-implementations of the CardDemo business rules.

These helpers are deliberately *not* a perfect line-by-line port of the
COBOL programs - they are a behavioural specification that the
modernised codebase (whether the existing COBOL or a future Java/SQL
port) must continue to satisfy.

The unit tests import this module instead of the COBOL programs so we
can run them in seconds on any GitHub-hosted Linux runner without a
mainframe.  When the COBOL programs are re-deployed onto AWS Mainframe
Modernization the integration suite runs the actual COBOL through the
M2 service to validate parity.

References:
    app/cbl/CBACT04C.cbl  - interest accrual
    app/cbl/CBTRN02C.cbl  - daily-transaction posting
    app/cbl/CBSTM03A.cbl  - statement generation
    app/cbl/COSGN00C.cbl  - sign-on
"""
from __future__ import annotations

import dataclasses
import datetime as _dt
import hashlib
from decimal import ROUND_HALF_UP, Decimal
from typing import Iterable, Mapping, Optional, Tuple


# ---------------------------------------------------------------------------
# Sign-on (mirrors COSGN00C.cbl)
# ---------------------------------------------------------------------------

@dataclasses.dataclass(frozen=True)
class UserRecord:
    user_id: str
    user_type: str           # "A" admin / "U" regular
    password_hash: str       # 64-hex SHA-256 digest
    password_salt: str       # up to 16 chars


def hash_password(password: str, salt: str) -> str:
    """Return SHA-256(password || salt) as lowercase hex.

    Matches the algorithm implemented by ``app/cbl/CHASHPW.cbl`` and
    ``db/migrate/init_user_security.sh``.
    """
    return hashlib.sha256((password + salt).encode("utf-8")).hexdigest()


def verify_signon(
    user_id: str,
    password: str,
    users: Mapping[str, UserRecord],
) -> Tuple[bool, str]:
    """Return ``(success, target_program)``.

    Mirrors the EVALUATE block in COSGN00C.cbl: admins are routed to
    ``COADM01C`` and regular users to ``COMEN01C``.  Authentication
    fails if the user does not exist or if the supplied password does
    not hash to the stored digest.
    """
    record = users.get(user_id)
    if record is None:
        return False, ""
    expected = record.password_hash.lower()
    candidate = hash_password(password, record.password_salt).lower()
    if candidate != expected:
        return False, ""
    return True, "COADM01C" if record.user_type == "A" else "COMEN01C"


# ---------------------------------------------------------------------------
# Interest accrual (mirrors CBACT04C.cbl)
# ---------------------------------------------------------------------------

@dataclasses.dataclass(frozen=True)
class TcatBalance:
    acct_id: int
    tran_type_cd: str
    tran_cat_cd: int
    balance: Decimal


@dataclasses.dataclass(frozen=True)
class DisclosureGroup:
    group_id: str
    tran_type_cd: str
    tran_cat_cd: int
    annual_rate: Decimal     # percent, e.g. 18.99


def monthly_interest(
    balance: Decimal,
    annual_rate_percent: Decimal,
) -> Decimal:
    """Replicate CBACT04C's monthly interest formula.

    The original COBOL uses ``balance * rate / 1200`` (rate is annual
    %, /12 = monthly, /100 = ratio).  Negative balances accrue zero
    interest, which matches the legacy behaviour of skipping accrual
    for credit balances.
    """
    if balance <= 0:
        return Decimal("0.00")
    return (balance * annual_rate_percent / Decimal(1200)).quantize(
        Decimal("0.01"), rounding=ROUND_HALF_UP,
    )


def total_interest_for_account(
    balances: Iterable[TcatBalance],
    rate_lookup: Mapping[Tuple[str, str, int], Decimal],
) -> Decimal:
    """Sum monthly interest across every transaction-category balance."""
    total = Decimal("0.00")
    for tcat in balances:
        rate = rate_lookup.get((tcat.tran_type_cd[:2], tcat.tran_type_cd, tcat.tran_cat_cd))
        if rate is None:
            # Fall back to the disclosure group key shape (group, type, cat).
            rate = rate_lookup.get(("DEFAULT  ", tcat.tran_type_cd, tcat.tran_cat_cd))
        if rate is None:
            continue
        total += monthly_interest(tcat.balance, rate)
    return total


# ---------------------------------------------------------------------------
# Transaction posting (mirrors CBTRN02C.cbl)
# ---------------------------------------------------------------------------

@dataclasses.dataclass
class Account:
    acct_id: int
    active_status: str
    current_balance: Decimal
    credit_limit: Decimal


@dataclasses.dataclass(frozen=True)
class DailyTransaction:
    tran_id: str
    card_num: str
    amount: Decimal
    type_cd: str
    cat_cd: int


REJECT_INACTIVE = "ACCOUNT INACTIVE"
REJECT_OVER_LIMIT = "OVER CREDIT LIMIT"
REJECT_UNKNOWN_CARD = "UNKNOWN CARD"


def post_transaction(
    tran: DailyTransaction,
    account: Account,
    *,
    card_to_account: Mapping[str, int],
) -> Tuple[bool, Optional[str]]:
    """Apply a single transaction to *account*.

    Returns ``(posted, reject_reason)``.  When ``posted`` is False the
    account is left untouched and ``reject_reason`` describes why -
    matching the strings written by CBTRN02C to ``DALYREJS-FILE``.
    """
    if tran.card_num not in card_to_account:
        return False, REJECT_UNKNOWN_CARD
    if account.active_status != "Y":
        return False, REJECT_INACTIVE
    new_balance = account.current_balance + tran.amount
    if new_balance > account.credit_limit:
        return False, REJECT_OVER_LIMIT
    account.current_balance = new_balance
    return True, None


# ---------------------------------------------------------------------------
# Statement generation (mirrors CBSTM03A.cbl / CBSTM03B.cbl)
# ---------------------------------------------------------------------------

@dataclasses.dataclass(frozen=True)
class Transaction:
    tran_id: str
    card_num: str
    amount: Decimal
    description: str
    orig_ts: _dt.datetime


def build_statement(
    account: Account,
    transactions: Iterable[Transaction],
) -> str:
    """Render a fixed-width account statement.

    The line widths and column alignments mirror the report layouts
    produced by the original CBSTM03A program so existing downstream
    consumers (PDF render pipeline, audit archive) remain compatible.
    """
    lines = [
        f"ACCOUNT STATEMENT  -  ACCT ID {account.acct_id:011d}",
        "TRAN ID          CARD NUMBER       AMOUNT       DESCRIPTION",
        "-" * 80,
    ]
    total = Decimal("0.00")
    for tx in transactions:
        lines.append(
            f"{tx.tran_id:<16} {tx.card_num:<16} {tx.amount:>12.2f}  {tx.description}"
        )
        total += tx.amount
    lines.append("-" * 80)
    lines.append(f"TOTAL ACTIVITY: {total:>15.2f}")
    lines.append(f"CURRENT BALANCE: {account.current_balance:>14.2f}")
    return "\n".join(lines)
