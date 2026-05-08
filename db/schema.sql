-- ============================================================================
-- CardDemo Modernised Schema (PostgreSQL dialect, ANSI-SQL where possible)
-- ============================================================================
-- This schema replaces the operational VSAM file inventory with a
-- relational store.  Column names mirror the COBOL field names from
-- the original copybooks so that the data-access layer (app/cbl/DBACCESS.cbl)
-- can be generated from, and validated against, those copybooks.
--
-- Source mapping (see app/cpy/*.cpy):
--   accounts             <- AWS.M2.CARDDEMO.ACCTDATA.VSAM.KSDS  (CVACT01Y, 300)
--   cards                <- AWS.M2.CARDDEMO.CARDDATA.VSAM.KSDS  (CVACT02Y, 150)
--   customers            <- AWS.M2.CARDDEMO.CUSTDATA.VSAM.KSDS  (CVCUS01Y, 500)
--   transactions         <- AWS.M2.CARDDEMO.TRANSACT.VSAM.KSDS  (CVTRA05Y, 350)
--   daily_transactions   <- AWS.M2.CARDDEMO.DALYTRAN.PS         (CVTRA06Y, 350)
--   card_xref            <- AWS.M2.CARDDEMO.CARDXREF.VSAM.KSDS  (CVACT03Y,  50)
--   tcat_balances        <- AWS.M2.CARDDEMO.TCATBALF.VSAM.KSDS  (CVTRA01Y,  50)
--   disclosure_groups    <- AWS.M2.CARDDEMO.DISCGRP.VSAM.KSDS   (CVTRA02Y,  50)
--   transaction_types    <- AWS.M2.CARDDEMO.TRANTYPE.VSAM.KSDS  (CVTRA03Y,  60)
--   transaction_categories <- AWS.M2.CARDDEMO.TRANCATG.VSAM.KSDS (CVTRA04Y, 60)
--   user_security        <- AWS.M2.CARDDEMO.USRSEC.VSAM.ESDS    (CSUSR01Y, 160)
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Reference data (no dependencies)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS transaction_types (
    tran_type_cd        CHAR(2)        NOT NULL PRIMARY KEY,
    tran_type_desc      VARCHAR(50)    NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_categories (
    tran_type_cd        CHAR(2)        NOT NULL,
    tran_cat_cd         INTEGER        NOT NULL,
    tran_cat_type_desc  VARCHAR(50)    NOT NULL,
    PRIMARY KEY (tran_type_cd, tran_cat_cd),
    CONSTRAINT fk_tran_cat_type
        FOREIGN KEY (tran_type_cd) REFERENCES transaction_types(tran_type_cd)
);

CREATE TABLE IF NOT EXISTS disclosure_groups (
    acct_group_id       CHAR(10)       NOT NULL,
    tran_type_cd        CHAR(2)        NOT NULL,
    tran_cat_cd         INTEGER        NOT NULL,
    int_rate            NUMERIC(6,2)   NOT NULL,
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd),
    CONSTRAINT fk_disc_tran_cat
        FOREIGN KEY (tran_type_cd, tran_cat_cd)
        REFERENCES transaction_categories(tran_type_cd, tran_cat_cd)
);

-- ----------------------------------------------------------------------------
-- 2. Customer / Account / Card domain
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS customers (
    cust_id                  NUMERIC(9,0)   NOT NULL PRIMARY KEY,
    cust_first_name          VARCHAR(25),
    cust_middle_name         VARCHAR(25),
    cust_last_name           VARCHAR(25),
    cust_addr_line_1         VARCHAR(50),
    cust_addr_line_2         VARCHAR(50),
    cust_addr_line_3         VARCHAR(50),
    cust_addr_state_cd       CHAR(2),
    cust_addr_country_cd     CHAR(3),
    cust_addr_zip            VARCHAR(10),
    cust_phone_num_1         VARCHAR(15),
    cust_phone_num_2         VARCHAR(15),
    cust_ssn                 NUMERIC(9,0),
    cust_govt_issued_id      VARCHAR(20),
    cust_dob                 DATE,
    cust_eft_account_id      VARCHAR(10),
    cust_pri_card_holder_ind CHAR(1),
    cust_fico_credit_score   SMALLINT
        CHECK (cust_fico_credit_score IS NULL OR cust_fico_credit_score BETWEEN 0 AND 999)
);

CREATE TABLE IF NOT EXISTS accounts (
    acct_id                NUMERIC(11,0)  NOT NULL PRIMARY KEY,
    acct_active_status     CHAR(1)        NOT NULL DEFAULT 'Y',
    acct_curr_bal          NUMERIC(12,2)  NOT NULL DEFAULT 0,
    acct_credit_limit      NUMERIC(12,2)  NOT NULL DEFAULT 0,
    acct_cash_credit_limit NUMERIC(12,2)  NOT NULL DEFAULT 0,
    acct_open_date         DATE,
    acct_expiration_date   DATE,
    acct_reissue_date      DATE,
    acct_curr_cyc_credit   NUMERIC(12,2)  NOT NULL DEFAULT 0,
    acct_curr_cyc_debit    NUMERIC(12,2)  NOT NULL DEFAULT 0,
    acct_addr_zip          VARCHAR(10),
    acct_group_id          CHAR(10)
);

CREATE TABLE IF NOT EXISTS cards (
    card_num               CHAR(16)       NOT NULL PRIMARY KEY,
    card_acct_id           NUMERIC(11,0)  NOT NULL,
    card_cvv_cd            SMALLINT       NOT NULL
        CHECK (card_cvv_cd BETWEEN 0 AND 999),
    card_embossed_name     VARCHAR(50),
    card_expiration_date   DATE,
    card_active_status     CHAR(1)        NOT NULL DEFAULT 'Y',
    CONSTRAINT fk_cards_account FOREIGN KEY (card_acct_id) REFERENCES accounts(acct_id)
);

CREATE INDEX IF NOT EXISTS idx_cards_acct_id ON cards(card_acct_id);

-- ----------------------------------------------------------------------------
-- 3. Cross reference (formerly the CARDXREF VSAM file with AIX on acct_id)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS card_xref (
    xref_card_num  CHAR(16)       NOT NULL PRIMARY KEY,
    xref_cust_id   NUMERIC(9,0)   NOT NULL,
    xref_acct_id   NUMERIC(11,0)  NOT NULL,
    CONSTRAINT fk_xref_card     FOREIGN KEY (xref_card_num) REFERENCES cards(card_num),
    CONSTRAINT fk_xref_customer FOREIGN KEY (xref_cust_id)  REFERENCES customers(cust_id),
    CONSTRAINT fk_xref_account  FOREIGN KEY (xref_acct_id)  REFERENCES accounts(acct_id)
);

CREATE INDEX IF NOT EXISTS idx_xref_acct_id ON card_xref(xref_acct_id);
CREATE INDEX IF NOT EXISTS idx_xref_cust_id ON card_xref(xref_cust_id);

-- ----------------------------------------------------------------------------
-- 4. Transactions (online + daily) and category balances
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS transactions (
    tran_id              CHAR(16)       NOT NULL PRIMARY KEY,
    tran_type_cd         CHAR(2)        NOT NULL,
    tran_cat_cd          INTEGER        NOT NULL,
    tran_source          VARCHAR(10),
    tran_desc            VARCHAR(100),
    tran_amt             NUMERIC(11,2)  NOT NULL,
    tran_merchant_id     NUMERIC(9,0),
    tran_merchant_name   VARCHAR(50),
    tran_merchant_city   VARCHAR(50),
    tran_merchant_zip    VARCHAR(10),
    tran_card_num        CHAR(16)       NOT NULL,
    tran_orig_ts         TIMESTAMP,
    tran_proc_ts         TIMESTAMP,
    CONSTRAINT fk_tx_card     FOREIGN KEY (tran_card_num) REFERENCES cards(card_num),
    CONSTRAINT fk_tx_tran_cat FOREIGN KEY (tran_type_cd, tran_cat_cd)
                              REFERENCES transaction_categories(tran_type_cd, tran_cat_cd)
);

CREATE INDEX IF NOT EXISTS idx_tx_card_num ON transactions(tran_card_num);
CREATE INDEX IF NOT EXISTS idx_tx_orig_ts  ON transactions(tran_orig_ts);

CREATE TABLE IF NOT EXISTS daily_transactions (
    dalytran_id            CHAR(16)       NOT NULL PRIMARY KEY,
    dalytran_type_cd       CHAR(2)        NOT NULL,
    dalytran_cat_cd        INTEGER        NOT NULL,
    dalytran_source        VARCHAR(10),
    dalytran_desc          VARCHAR(100),
    dalytran_amt           NUMERIC(11,2)  NOT NULL,
    dalytran_merchant_id   NUMERIC(9,0),
    dalytran_merchant_name VARCHAR(50),
    dalytran_merchant_city VARCHAR(50),
    dalytran_merchant_zip  VARCHAR(10),
    dalytran_card_num      CHAR(16)       NOT NULL,
    dalytran_orig_ts       TIMESTAMP,
    dalytran_proc_ts       TIMESTAMP,
    posted                 BOOLEAN        NOT NULL DEFAULT FALSE,
    posted_ts              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dalytran_card_num ON daily_transactions(dalytran_card_num);
CREATE INDEX IF NOT EXISTS idx_dalytran_posted   ON daily_transactions(posted);

CREATE TABLE IF NOT EXISTS tcat_balances (
    trancat_acct_id NUMERIC(11,0) NOT NULL,
    trancat_type_cd CHAR(2)       NOT NULL,
    trancat_cd      INTEGER       NOT NULL,
    tran_cat_bal    NUMERIC(11,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (trancat_acct_id, trancat_type_cd, trancat_cd),
    CONSTRAINT fk_tcat_account
        FOREIGN KEY (trancat_acct_id) REFERENCES accounts(acct_id),
    CONSTRAINT fk_tcat_tran_cat
        FOREIGN KEY (trancat_type_cd, trancat_cd)
        REFERENCES transaction_categories(tran_type_cd, tran_cat_cd)
);

-- ----------------------------------------------------------------------------
-- 5. User security (replaces USRSEC VSAM ESDS / RRDS)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS user_security (
    user_id        CHAR(8)       NOT NULL PRIMARY KEY,
    first_name     VARCHAR(20),
    last_name      VARCHAR(20),
    user_type      CHAR(1)       NOT NULL CHECK (user_type IN ('A','U')),
    password_hash  CHAR(64)      NOT NULL,
    password_salt  CHAR(16)      NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_security_type ON user_security(user_type);

-- ----------------------------------------------------------------------------
-- 6. Audit trail (new in modernised system; replaces JCL/CICS-only logs)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id        BIGSERIAL     PRIMARY KEY,
    occurred_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id         CHAR(8),
    operation       VARCHAR(32)   NOT NULL,
    target_entity   VARCHAR(32),
    target_key      VARCHAR(32),
    detail          TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at ON audit_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id     ON audit_log(user_id);

COMMIT;
