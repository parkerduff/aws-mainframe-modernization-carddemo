#!/usr/bin/env bash
#
# run_full_migration.sh - End-to-end migration of CardDemo VSAM exports
# into the relational schema defined in db/schema.sql.  Intended to be
# run from a non-mainframe workstation (or CI) after IDCAMS REPRO has
# produced the sequential PS exports.

set -euo pipefail

usage() {
    cat <<'USAGE'
Usage: run_full_migration.sh <export-dir> <output-dir>

  export-dir   directory containing the IDCAMS REPRO output files,
               named e.g. AWS.M2.CARDDEMO.ACCTDATA.PS
  output-dir   where to write CSV staging files

Environment:
  DB_DSN       libpq DSN for psql (optional; if set, the script will
               also pipe each CSV through `\copy`).
USAGE
    exit 1
}

[[ $# -ne 2 ]] && usage

EXPORT_DIR="$1"
OUTPUT_DIR="$2"
mkdir -p "$OUTPUT_DIR"

declare -A SOURCES=(
    [transaction_types]="AWS.M2.CARDDEMO.TRANTYPE.PS"
    [transaction_categories]="AWS.M2.CARDDEMO.TRANCATG.PS"
    [customers]="AWS.M2.CARDDEMO.CUSTDATA.PS"
    [accounts]="AWS.M2.CARDDEMO.ACCTDATA.PS"
    [cards]="AWS.M2.CARDDEMO.CARDDATA.PS"
    [card_xref]="AWS.M2.CARDDEMO.CARDXREF.PS"
    [disclosure_groups]="AWS.M2.CARDDEMO.DISCGRP.PS"
    [transactions]="AWS.M2.CARDDEMO.TRANSACT.PS"
    [daily_transactions]="AWS.M2.CARDDEMO.DALYTRAN.PS"
    [tcat_balances]="AWS.M2.CARDDEMO.TCATBALF.PS"
    [user_security]="AWS.M2.CARDDEMO.USRSEC.PS"
)

ORDER=(
    transaction_types
    transaction_categories
    customers
    accounts
    cards
    card_xref
    disclosure_groups
    transactions
    daily_transactions
    tcat_balances
    user_security
)

for entity in "${ORDER[@]}"; do
    src="$EXPORT_DIR/${SOURCES[$entity]}"
    dst="$OUTPUT_DIR/$entity.csv"

    if [[ ! -f "$src" ]]; then
        echo "WARN: $src not found - skipping $entity"
        continue
    fi

    echo "[$(date -u +%FT%TZ)] Staging $entity from $src"
    python -m db.migrate.loaders "$entity" "$src" "$dst" --format csv

    if [[ -n "${DB_DSN:-}" ]]; then
        echo "[$(date -u +%FT%TZ)] Loading $entity into $DB_DSN"
        psql "$DB_DSN" -c "\\copy $entity FROM '$dst' WITH (FORMAT csv, HEADER true)"
    fi
done

echo "Migration complete.  Staged CSVs in $OUTPUT_DIR"
