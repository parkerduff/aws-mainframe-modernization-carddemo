"""Generic fixed-width record parser for CardDemo migration scripts.

Each entity defines a list of (column_name, length, type) tuples that
mirror the COBOL copybook layout.  The parser reads the source PS file
record-by-record, applies trimming + type conversion, and yields dicts
suitable for SQL bulk-load utilities (psycopg, sqlite3, etc.).
"""
from __future__ import annotations

import csv
import datetime as _dt
import io
import logging
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Callable, Dict, Iterable, Iterator, List, Optional, Tuple

LOGGER = logging.getLogger(__name__)


@dataclass(frozen=True)
class Field:
    """Description of a single fixed-width column."""

    name: str
    length: int
    converter: Callable[[str], object]


def _strip(raw: str) -> str:
    return raw.strip()


def _optional(raw: str) -> Optional[str]:
    value = raw.strip()
    return value or None


def _int(raw: str) -> Optional[int]:
    value = raw.strip()
    if not value:
        return None
    try:
        return int(value)
    except ValueError:
        LOGGER.warning("Invalid integer value: %r", raw)
        return None


def _decimal(scale: int) -> Callable[[str], Optional[Decimal]]:
    """Return a converter for COBOL `S9(n)V99`-style numerics.

    The mainframe export format used by this repo represents the value
    as a left-justified human-readable signed decimal with leading
    spaces, so a direct ``Decimal`` cast is sufficient.  When the
    upstream extract switches to packed-decimal (COMP-3) the migration
    job should use ``ebcdic.parse_packed_decimal`` instead.
    """

    def convert(raw: str) -> Optional[Decimal]:
        value = raw.strip()
        if not value:
            return None
        try:
            decimal_value = Decimal(value)
        except InvalidOperation:
            LOGGER.warning("Invalid decimal(scale=%d) value: %r", scale, raw)
            return None
        # Normalise to the requested scale.
        return decimal_value.quantize(Decimal(10) ** -scale)

    return convert


def _date(format_string: str = "%Y-%m-%d") -> Callable[[str], Optional[_dt.date]]:
    def convert(raw: str) -> Optional[_dt.date]:
        value = raw.strip()
        if not value:
            return None
        try:
            return _dt.datetime.strptime(value, format_string).date()
        except ValueError:
            LOGGER.warning("Invalid date %r (expected %s)", raw, format_string)
            return None

    return convert


def _timestamp(raw: str) -> Optional[_dt.datetime]:
    value = raw.strip()
    if not value:
        return None
    # The CardDemo COBOL programs emit DB2 timestamps in
    # "YYYY-MM-DD-HH.MM.SS.mmmmmm" format.
    for fmt in (
        "%Y-%m-%d-%H.%M.%S.%f",
        "%Y-%m-%d %H:%M:%S.%f",
        "%Y-%m-%d %H:%M:%S",
    ):
        try:
            return _dt.datetime.strptime(value[: len(fmt)], fmt)
        except ValueError:
            continue
    LOGGER.warning("Invalid timestamp value: %r", raw)
    return None


# Convenience shorthands used by the per-entity loaders ------------------------
TRIM = _strip
OPT = _optional
INT = _int
NUM2 = _decimal(2)
DATE = _date()
TS = _timestamp


def parse_records(
    source: Path | str,
    fields: Iterable[Field],
    record_length: int,
    encoding: str = "latin-1",
) -> Iterator[Dict[str, object]]:
    """Iterate over fixed-width records in *source*.

    *record_length* must equal the sum of the field lengths and is
    checked on every line so corrupted exports are caught early rather
    than producing silently wrong inserts.
    """
    fields = list(fields)
    expected_length = sum(field.length for field in fields)
    if expected_length != record_length:
        raise ValueError(
            f"Field length mismatch: declared {record_length}, computed {expected_length}"
        )

    path = Path(source)
    with path.open("r", encoding=encoding, newline="") as handle:
        for line_no, raw in enumerate(handle, start=1):
            line = raw.rstrip("\r\n")
            if not line:
                continue
            if len(line) < record_length:
                # The mainframe export is fixed-width with trailing
                # blanks stripped; pad to compensate so the offsets
                # below still address the correct fields.
                line = line.ljust(record_length)
            elif len(line) > record_length:
                LOGGER.warning(
                    "Line %d in %s longer than expected (%d > %d) - truncating",
                    line_no, path, len(line), record_length,
                )
                line = line[:record_length]

            offset = 0
            row: Dict[str, object] = {}
            for field in fields:
                segment = line[offset : offset + field.length]
                offset += field.length
                row[field.name] = field.converter(segment)
            yield row


def parse_records_to_csv(
    source: Path | str,
    target: Path | str,
    fields: Iterable[Field],
    record_length: int,
    encoding: str = "latin-1",
) -> int:
    """Convert a fixed-width export to a CSV staging file.

    Returns the number of records written.  Useful for offline review
    and for COPY-style bulk load utilities (PostgreSQL, Snowflake, etc).
    """
    fields = list(fields)
    target_path = Path(target)
    target_path.parent.mkdir(parents=True, exist_ok=True)

    with target_path.open("w", encoding="utf-8", newline="") as out:
        writer = csv.DictWriter(out, fieldnames=[field.name for field in fields])
        writer.writeheader()
        count = 0
        for row in parse_records(source, fields, record_length, encoding=encoding):
            # Render decimals/dates/timestamps using their canonical str().
            writer.writerow({k: ("" if v is None else str(v)) for k, v in row.items()})
            count += 1
    return count


def lines_for_review(rows: Iterable[Dict[str, object]], limit: int = 5) -> str:
    """Return a small human-readable preview suitable for log output."""
    buffer = io.StringIO()
    for index, row in enumerate(rows):
        if index >= limit:
            break
        buffer.write(f"  row {index}: {row}\n")
    return buffer.getvalue()
