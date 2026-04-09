#!/usr/bin/env python3
"""
CardDemo Batch Modernization — Post EPICs & User Stories to JIRA

Usage:
  export JIRA_URL=https://yourcompany.atlassian.net
  export JIRA_PROJECT_KEY=CARD
  export JIRA_EMAIL=your@email.com
  export JIRA_API_TOKEN=your-api-token
  pip install -r requirements.txt
  python post_to_jira.py          # actual run
  python post_to_jira.py --dry-run  # preview only
"""

import argparse
import logging
import os
import sys
import time

from jira import JIRA
from jira.exceptions import JIRAError

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger(__name__)

LABELS = ["carddemo-modernization", "batch-rewrite"]

# ---------------------------------------------------------------------------
# Data definitions — every EPIC and story is self-contained here.
# ---------------------------------------------------------------------------

EPICS = [
    {
        "key": "EPIC-1",
        "summary": "EPIC 1: Database Schema & Data Migration",
        "description": (
            "Migrate VSAM KSDS files to Aurora PostgreSQL while preserving "
            "all record layouts, key structures, and data integrity."
        ),
        "stories": [
            {
                "id": "US-1.1",
                "summary": "US-1.1: Create PostgreSQL schema for all VSAM datasets",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* All VSAM KSDS files (CUSTDATA, ACCTDATA, CARDDATA, CARDXREF, "
                    "TRANSACT, TRANTYPE, TRANCATG, DISCGRP, TCATBALF, USRSEC) have "
                    "corresponding PostgreSQL tables\n"
                    "* Primary keys match original KSDS key structures\n"
                    "* Alternate indexes are represented as secondary indexes or unique "
                    "constraints\n"
                    "* All copybook field layouts (PIC clauses) are mapped to appropriate "
                    "PostgreSQL data types\n"
                    "* Foreign key relationships between tables are defined where applicable"
                ),
            },
            {
                "id": "US-1.2",
                "summary": "US-1.2: Seed database from ASCII data files",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* All data from app/data/ files is loaded into corresponding "
                    "PostgreSQL tables\n"
                    "* Record counts match between source files and target tables\n"
                    "* Data integrity constraints pass after loading\n"
                    "* Character encoding is correctly handled (EBCDIC to UTF-8 where "
                    "applicable)"
                ),
            },
            {
                "id": "US-1.3",
                "summary": "US-1.3: Create S3 bucket structure for file-based I/O",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* S3 bucket {{carddemo-data}} is created with appropriate prefixes "
                    "for each file type\n"
                    "* Daily transaction input files can be placed in "
                    "{{s3://carddemo-data/daily-transactions/}}\n"
                    "* Reject files are written to "
                    "{{s3://carddemo-data/rejects/\\{date\\}/}}\n"
                    "* Statement output files are written to "
                    "{{s3://carddemo-data/statements/}}\n"
                    "* IAM policies restrict access to authorized services only"
                ),
            },
        ],
    },
    {
        "key": "EPIC-2",
        "summary": "EPIC 2: Transaction Posting Service (CBTRN02C)",
        "description": (
            "Rewrite the daily transaction posting program preserving all "
            "validation rules, reject handling, category balance upsert, "
            "and account balance update logic."
        ),
        "stories": [
            {
                "id": "US-2.1",
                "summary": "US-2.1: Implement card cross-reference validation",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Look up {{card_xref}} by {{dalytran_card_num}}\n"
                    "* If not found: {{validation_fail_reason = 100}}, "
                    "desc = 'INVALID CARD NUMBER FOUND'\n"
                    "* If found: retrieve {{xref_acct_id}} for subsequent account lookup\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN02C.cbl}} lines 380-392"
                ),
            },
            {
                "id": "US-2.2",
                "summary": "US-2.2: Implement account validation with credit limit and expiry checks",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Look up {{accounts}} by {{xref_acct_id}}\n"
                    "* If not found: {{validation_fail_reason = 101}}, "
                    "desc = 'ACCOUNT RECORD NOT FOUND'\n"
                    "* Compute {{temp_bal = acct_curr_cyc_credit - acct_curr_cyc_debit "
                    "+ dalytran_amt}}\n"
                    "* If {{acct_credit_limit < temp_bal}}: "
                    "{{validation_fail_reason = 102}}, desc = 'OVERLIMIT TRANSACTION'\n"
                    "* If {{acct_expiration_date < dalytran_orig_ts[1:10]}}: "
                    "{{validation_fail_reason = 103}}, "
                    "desc = 'TRANSACTION RECEIVED AFTER ACCT EXPIRATION'\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN02C.cbl}} lines 393-422"
                ),
            },
            {
                "id": "US-2.3",
                "summary": "US-2.3: Implement transaction posting with category balance upsert",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Map all daily transaction fields to transaction record\n"
                    "* {{proc_ts}} generated as DB2-format timestamp\n"
                    "* Category balance upsert: look up {{tran_cat_balances}} by "
                    "({{xref_acct_id}}, {{dalytran_type_cd}}, {{dalytran_cat_cd}}); "
                    "INSERT if not found, UPDATE ({{tran_cat_bal += dalytran_amt}}) "
                    "if found\n"
                    "* Account balance update: {{acct_curr_bal += dalytran_amt}}; "
                    "if amt >= 0: {{acct_curr_cyc_credit += amt}}; "
                    "else: {{acct_curr_cyc_debit += amt}}\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN02C.cbl}} lines 424-559"
                ),
            },
            {
                "id": "US-2.4",
                "summary": "US-2.4: Implement reject record writing",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Reject record = original daily transaction data + validation "
                    "trailer (reason code + reason description)\n"
                    "* Write to {{s3://carddemo-data/rejects/\\{date\\}/}}\n"
                    "* If {{reject_count > 0}}, service exits with non-zero status "
                    "(equivalent to {{RETURN-CODE = 4}})\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN02C.cbl}} lines 446-465"
                ),
            },
            {
                "id": "US-2.5",
                "summary": "US-2.5: Implement sequential processing order preservation",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Input file read sequentially from S3\n"
                    "* Transactions processed one-at-a-time in file order\n"
                    "* Account balance updates are cumulative within the batch run"
                ),
            },
        ],
    },
    {
        "key": "EPIC-3",
        "summary": "EPIC 3: Interest Calculation Service (CBACT04C)",
        "description": (
            "Rewrite the monthly interest calculation program preserving "
            "control-break logic, disclosure group rate lookups with default "
            "fallback, interest formula, and account update logic."
        ),
        "stories": [
            {
                "id": "US-3.1",
                "summary": "US-3.1: Implement control-break processing by account",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Read {{tran_cat_balances}} ordered by {{trancat_acct_id}}\n"
                    "* On account break: update previous account "
                    "({{acct_curr_bal += total_interest}}, reset "
                    "{{cyc_credit=0}}, {{cyc_debit=0}})\n"
                    "* Skip update on first record (WS-FIRST-TIME logic)\n"
                    "* Perform final account update on end of data\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT04C.cbl}} lines 188-213, 350-370"
                ),
            },
            {
                "id": "US-3.2",
                "summary": "US-3.2: Implement disclosure group rate lookup with default fallback",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Look up {{disclosure_groups}} by ({{acct_group_id}}, "
                    "{{trancat_cd}}, {{trancat_type_cd}})\n"
                    "* If not found: retry with {{acct_group_id = 'DEFAULT'}}\n"
                    "* If default also not found: ABEND\n"
                    "* If {{dis_int_rate = 0}}: skip interest and fee computation\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT04C.cbl}} lines 415-460"
                ),
            },
            {
                "id": "US-3.3",
                "summary": "US-3.3: Implement interest computation and transaction generation",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Formula: {{monthly_interest = (tran_cat_bal * dis_int_rate) "
                    "/ 1200}} using exact decimal arithmetic\n"
                    "* Generate interest transaction record with "
                    "{{tran_type_cd='01'}}, {{tran_cat_cd='05'}}, "
                    "{{tran_source='System'}}, "
                    "{{tran_desc='Int. for a/c ' + acct_id}}\n"
                    "* Accept date parameter (equivalent to JCL "
                    "{{PARM='2022071800'}})\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT04C.cbl}} lines 462-515"
                ),
            },
            {
                "id": "US-3.4",
                "summary": "US-3.4: Implement fee computation placeholder",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Preserve empty method/interface called after interest "
                    "computation (1400-COMPUTE-FEES is currently a stub)\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT04C.cbl}} lines 518-520"
                ),
            },
        ],
    },
    {
        "key": "EPIC-4",
        "summary": "EPIC 4: Transaction Report Service (CBTRN03C)",
        "description": (
            "Rewrite the transaction report program preserving date-range "
            "filtering, reference lookups, and multi-level totaling."
        ),
        "stories": [
            {
                "id": "US-4.1",
                "summary": "US-4.1: Implement date-range filtering from parameter input",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Read start date and end date from parameter source\n"
                    "* Filter: include only if {{tran_proc_ts[1:10] >= start_date}} "
                    "AND {{<= end_date}}\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN03C.cbl}} lines 170-178, 220-243"
                ),
            },
            {
                "id": "US-4.2",
                "summary": "US-4.2: Implement reference data lookups for report enrichment",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* On card number change: look up {{card_xref}}\n"
                    "* Look up {{transaction_types}} by {{tran_type_cd}}\n"
                    "* Look up {{transaction_categories}} by "
                    "({{tran_type_cd}}, {{tran_cat_cd}})\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN03C.cbl}} lines 484-512"
                ),
            },
            {
                "id": "US-4.3",
                "summary": "US-4.3: Implement multi-level report totaling",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Page totals: accumulate {{tran_amt}} per page, write page "
                    "total line\n"
                    "* Account totals: accumulate per account (card number), write "
                    "on card number change\n"
                    "* Grand totals: accumulate from page totals, write at end\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN03C.cbl}} lines 274-374"
                ),
            },
        ],
    },
    {
        "key": "EPIC-5",
        "summary": "EPIC 5: Data Export/Import Services (CBEXPORT/CBIMPORT)",
        "description": (
            "Rewrite the branch data export and import programs preserving "
            "the multi-record-type format, field mappings, and error handling."
        ),
        "stories": [
            {
                "id": "US-5.1",
                "summary": "US-5.1: Implement data export service",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Export record format: record type (C/A/X/T/D) + 26-char "
                    "timestamp + sequence number + branch ID ('0001') + region "
                    "code ('NORTH') + record data\n"
                    "* Export order: customers, accounts, xrefs, transactions, cards\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBEXPORT.cbl}} lines 146-158, 269-373"
                ),
            },
            {
                "id": "US-5.2",
                "summary": "US-5.2: Implement data import service",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Dispatch by record type: C->customer, A->account, X->xref, "
                    "T->transaction, D->card, OTHER->error\n"
                    "* Unknown record types: write error record\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBIMPORT.cbl}} lines 270-285, 424-478"
                ),
            },
        ],
    },
    {
        "key": "EPIC-6",
        "summary": "EPIC 6: Transaction Type Maintenance Service (COBTUPDT)",
        "description": "Rewrite the DB2 transaction type CRUD program.",
        "stories": [
            {
                "id": "US-6.1",
                "summary": "US-6.1: Implement transaction type CRUD operations",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* {{rec_type='A'}}: INSERT; {{'U'}}: UPDATE; {{'D'}}: DELETE; "
                    "{{'*'}}: skip; OTHER: ABEND\n"
                    "* On SQL error or no rows found: ABEND with return code 4\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/app-transaction-type-db2/cbl/COBTUPDT.cbl}} lines 109-237"
                ),
            },
        ],
    },
    {
        "key": "EPIC-7",
        "summary": "EPIC 7: Authorization Purge Service (CBPAUP0C)",
        "description": (
            "Rewrite the IMS-based expired authorization purge program."
        ),
        "stories": [
            {
                "id": "US-7.1",
                "summary": "US-7.1: Implement expired authorization purge with checkpoint logic",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Expiry check: {{auth_date = 99999 - auth_date_9c}}; "
                    "{{day_diff = current_yyddd - auth_date}}; "
                    "if {{day_diff >= expiry_days}} -> delete\n"
                    "* On delete approved: decrement {{approved_auth_cnt}}, "
                    "subtract {{approved_amt}}\n"
                    "* On delete declined: decrement {{declined_auth_cnt}}, "
                    "subtract {{transaction_amt}}\n"
                    "* If both counts <= 0: delete summary record\n"
                    "* Checkpoint every {{chkp_freq}} summaries\n"
                    "* Default {{expiry_days=5}}, {{chkp_freq=5}}\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl}} "
                    "lines 132-386"
                ),
            },
        ],
    },
    {
        "key": "EPIC-8",
        "summary": "EPIC 8: Transaction Validation Service (CBTRN01C)",
        "description": (
            "Rewrite the daily transaction validation/display program."
        ),
        "stories": [
            {
                "id": "US-8.1",
                "summary": "US-8.1: Implement transaction validation with xref and account lookup",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Look up {{card_xref}}; if not found log "
                    "'CARD NUMBER \\{num\\} COULD NOT BE VERIFIED'\n"
                    "* If found: look up {{accounts}}; if not found log "
                    "'ACCOUNT \\{id\\} NOT FOUND'\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBTRN01C.cbl}} lines 154-250"
                ),
            },
        ],
    },
    {
        "key": "EPIC-9",
        "summary": "EPIC 9: Account Data Processing Service (CBACT01C)",
        "description": (
            "Rewrite the account read/write program preserving multi-format "
            "output."
        ),
        "stories": [
            {
                "id": "US-9.1",
                "summary": "US-9.1: Implement account data processing with multiple output formats",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Fixed-length output with date formatting "
                    "(replaces {{CALL 'COBDATFT'}})\n"
                    "* If {{acct_curr_cyc_debit = 0}}, substitute {{2525.00}}\n"
                    "* Array-format record: 3 occurrences with hardcoded test "
                    "values for indices 2,3\n"
                    "* Two variable-length records: VB1 (12 bytes) and VB2 "
                    "(39 bytes)\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT01C.cbl}} lines 165-315"
                ),
            },
        ],
    },
    {
        "key": "EPIC-10",
        "summary": "EPIC 10: Orchestration & Scheduling",
        "description": (
            "Replace JCL job chains and CA7/Control-M scheduling with "
            "AWS Step Functions and EventBridge."
        ),
        "stories": [
            {
                "id": "US-10.1",
                "summary": "US-10.1: Implement Daily batch Step Function",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Chain: Backup Transactions -> Post Transactions -> "
                    "Check Rejects\n"
                    "* If rejects > 0: send SNS alert\n"
                    "* Triggered daily via EventBridge\n"
                    "* CLOSEFIL/OPENFIL eliminated; WAITSTEP replaced by "
                    "native Wait state\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/scheduler/CardDemo.controlm}} lines 1-25"
                ),
            },
            {
                "id": "US-10.2",
                "summary": "US-10.2: Implement Monthly batch Step Function",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Chain: Interest Calculation -> Combine Transactions (SQL) "
                    "-> Statement Generation\n"
                    "* COMBTRAN SORT replaced by SQL "
                    "{{INSERT...SELECT...UNION ALL...ORDER BY}}\n"
                    "* Triggered monthly via EventBridge\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/scheduler/CardDemo.controlm}} lines 64-65"
                ),
            },
            {
                "id": "US-10.3",
                "summary": "US-10.3: Implement Weekly batch Step Function",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Chain: Transaction Type Maintenance -> Disclosure Group "
                    "Refresh\n"
                    "* Triggered weekly (Saturday) via EventBridge\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/scheduler/CardDemo.controlm}} lines 26-63"
                ),
            },
        ],
    },
    {
        "key": "EPIC-11",
        "summary": "EPIC 11: Statement Generation Service (CBSTM03A)",
        "description": "Rewrite the statement generation program.",
        "stories": [
            {
                "id": "US-11.1",
                "summary": "US-11.1: Implement per-card statement generation",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Read transactions, xref, accounts, customers\n"
                    "* Generate text statement and HTML statement\n"
                    "* Output to S3 (replaces STATEMNT.PS and STATEMNT.HTML)\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/jcl/CREASTMT.JCL}} lines 79-96, "
                    "{{app/cbl/CBSTM03A.cbl}}"
                ),
            },
        ],
    },
    {
        "key": "EPIC-12",
        "summary": "EPIC 12: Utility Programs Replacement",
        "description": (
            "Replace simple utility programs with native AWS services."
        ),
        "stories": [
            {
                "id": "US-12.1",
                "summary": "US-12.1: Replace CBACT02C/CBACT03C/CBCUS01C with SQL queries",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* These are read-and-display programs; replace with "
                    "parameterized SQL queries or Athena views\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/CBACT02C.cbl}}, {{app/cbl/CBACT03C.cbl}}, "
                    "{{app/cbl/CBCUS01C.cbl}}"
                ),
            },
            {
                "id": "US-12.2",
                "summary": "US-12.2: Replace COBSWAIT with Step Functions Wait state",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* COBSWAIT functionality replaced by native AWS Step "
                    "Functions Wait state\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/cbl/COBSWAIT.cbl}} lines 34-40"
                ),
            },
            {
                "id": "US-12.3",
                "summary": "US-12.3: Eliminate CLOSEFIL/OPENFIL jobs",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* No VSAM file locking needed with database; these become "
                    "no-ops\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/jcl/CLOSEFIL.jcl}}"
                ),
            },
            {
                "id": "US-12.4",
                "summary": "US-12.4: Replace TRANBKP with RDS automated snapshots",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* TRANBKP job functionality replaced by RDS automated "
                    "snapshots\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/jcl/TRANBKP.jcl}}"
                ),
            },
            {
                "id": "US-12.5",
                "summary": "US-12.5: Replace COMBTRAN SORT with SQL merge",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Single SQL: {{INSERT INTO transactions SELECT ... FROM "
                    "system_transactions UNION ALL SELECT ... FROM "
                    "backup_transactions ORDER BY tran_id}}\n"
                    "\n"
                    "h3. Reference\n"
                    "{{app/jcl/COMBTRAN.jcl}} lines 22-48"
                ),
            },
        ],
    },
    {
        "key": "EPIC-13",
        "summary": "EPIC 13: Testing & Validation",
        "description": (
            "Ensure zero business logic deprecation through comprehensive "
            "testing."
        ),
        "stories": [
            {
                "id": "US-13.1",
                "summary": "US-13.1: Create parallel-run test harness",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Run both COBOL and modernized services with identical "
                    "input data\n"
                    "* Compare outputs byte-for-byte (or field-for-field after "
                    "format normalization)\n"
                    "* Cover: CBTRN02C (transaction posting), CBACT04C "
                    "(interest calc), CBTRN03C (report)"
                ),
            },
            {
                "id": "US-13.2",
                "summary": "US-13.2: Create decimal precision validation tests",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Verify that all NUMERIC(12,2) / BigDecimal calculations "
                    "match COBOL COMP-3 / PIC S9(10)V99 results\n"
                    "* Test interest formula: {{(balance * rate) / 1200}} with "
                    "edge cases (zero balance, zero rate, max balance, "
                    "negative balance)"
                ),
            },
            {
                "id": "US-13.3",
                "summary": "US-13.3: Create reject scenario test suite",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Test all 4 reject reasons: 100 (invalid card), "
                    "101 (account not found), 102 (overlimit), "
                    "103 (expired account)\n"
                    "* Verify reject file format matches original"
                ),
            },
            {
                "id": "US-13.4",
                "summary": "US-13.4: Create sequential processing order tests",
                "description": (
                    "h3. Acceptance Criteria\n"
                    "* Verify that processing transactions in file order produces "
                    "identical cumulative balance results\n"
                    "* Test with transactions that would produce different reject "
                    "outcomes if processed out of order"
                ),
            },
        ],
    },
]


def get_jira_client() -> JIRA:
    """Build and return an authenticated JIRA client."""
    url = os.environ.get("JIRA_URL")
    email = os.environ.get("JIRA_EMAIL")
    token = os.environ.get("JIRA_API_TOKEN")
    if not all([url, email, token]):
        log.error(
            "Missing one or more required environment variables: "
            "JIRA_URL, JIRA_EMAIL, JIRA_API_TOKEN"
        )
        sys.exit(1)
    return JIRA(server=url, basic_auth=(email, token))


def create_epic(
    jira: JIRA,
    project_key: str,
    epic_data: dict,
    dry_run: bool,
) -> str:
    """Create a JIRA Epic and return its issue key."""
    fields = {
        "project": {"key": project_key},
        "summary": epic_data["summary"],
        "description": epic_data["description"],
        "issuetype": {"name": "Epic"},
        "labels": LABELS,
    }
    if dry_run:
        log.info("[DRY-RUN] Would create Epic: %s", epic_data["summary"])
        return f"DRY-{epic_data['key']}"

    for attempt in range(1, 4):
        try:
            issue = jira.create_issue(fields=fields)
            log.info("Created Epic %s  -> %s", issue.key, epic_data["summary"])
            return issue.key
        except JIRAError as exc:
            if exc.status_code == 429:
                wait = 2 ** attempt
                log.warning("Rate-limited; retrying in %ds ...", wait)
                time.sleep(wait)
            else:
                raise
    log.error("Failed to create Epic after retries: %s", epic_data["summary"])
    sys.exit(1)


def create_story(
    jira: JIRA,
    project_key: str,
    epic_key: str,
    story_data: dict,
    dry_run: bool,
) -> str:
    """Create a JIRA Story linked to an Epic and return its issue key."""
    fields = {
        "project": {"key": project_key},
        "summary": story_data["summary"],
        "description": story_data["description"],
        "issuetype": {"name": "Story"},
        "labels": LABELS,
    }

    # Try the modern "Epic Link" custom field approach.
    # The exact field name varies by instance; common names are
    # "Epic Link" (customfield_10014) or "Epic Name".  We'll set
    # it via the key and fall back to adding a link if the field
    # is rejected.
    if not dry_run:
        # Attempt to discover the Epic Link custom field
        epic_link_field = _find_epic_link_field(jira)
        if epic_link_field:
            fields[epic_link_field] = epic_key

    if dry_run:
        log.info(
            "[DRY-RUN] Would create Story: %s  (Epic: %s)",
            story_data["summary"],
            epic_key,
        )
        return f"DRY-{story_data['id']}"

    for attempt in range(1, 4):
        try:
            issue = jira.create_issue(fields=fields)
            log.info(
                "Created Story %s  -> %s  (Epic: %s)",
                issue.key,
                story_data["summary"],
                epic_key,
            )
            # If we couldn't set the Epic Link field, create an
            # issue link instead.
            if not epic_link_field:
                try:
                    jira.create_issue_link(
                        type="Epic-Story Link",
                        inwardIssue=epic_key,
                        outwardIssue=issue.key,
                    )
                except JIRAError:
                    # Fallback: generic "Relates" link
                    try:
                        jira.create_issue_link(
                            type="Relates",
                            inwardIssue=epic_key,
                            outwardIssue=issue.key,
                        )
                    except JIRAError as link_err:
                        log.warning(
                            "Could not link %s to epic %s: %s",
                            issue.key,
                            epic_key,
                            link_err,
                        )
            return issue.key
        except JIRAError as exc:
            if exc.status_code == 429:
                wait = 2 ** attempt
                log.warning("Rate-limited; retrying in %ds ...", wait)
                time.sleep(wait)
            else:
                raise
    log.error("Failed to create Story after retries: %s", story_data["summary"])
    sys.exit(1)


_epic_link_field_cache: str | None = None
_epic_link_field_searched = False


def _find_epic_link_field(jira: JIRA) -> str | None:
    """Return the custom-field ID for 'Epic Link', or None."""
    global _epic_link_field_cache, _epic_link_field_searched
    if _epic_link_field_searched:
        return _epic_link_field_cache
    _epic_link_field_searched = True
    try:
        for field in jira.fields():
            name = (field.get("name") or "").lower()
            if name in ("epic link", "epic"):
                _epic_link_field_cache = field["id"]
                log.info("Discovered Epic Link field: %s", field["id"])
                return _epic_link_field_cache
    except Exception:
        pass
    log.info("Epic Link custom field not found; will use issue links instead.")
    return None


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Post CardDemo modernization EPICs & Stories to JIRA."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be created without actually posting to JIRA.",
    )
    args = parser.parse_args()

    project_key = os.environ.get("JIRA_PROJECT_KEY", "CARD")

    if args.dry_run:
        log.info("=== DRY-RUN MODE — no issues will be created ===")
        jira = None  # type: ignore[assignment]
    else:
        jira = get_jira_client()

    total_epics = 0
    total_stories = 0

    for epic_data in EPICS:
        epic_key = create_epic(jira, project_key, epic_data, args.dry_run)
        total_epics += 1
        for story_data in epic_data["stories"]:
            create_story(jira, project_key, epic_key, story_data, args.dry_run)
            total_stories += 1

    log.info(
        "Done! Created %d EPICs and %d Stories.",
        total_epics,
        total_stories,
    )


if __name__ == "__main__":
    main()
