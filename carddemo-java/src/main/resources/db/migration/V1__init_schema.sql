-- CardDemo Database Schema
-- Migrated from COBOL VSAM copybooks to relational tables

CREATE TABLE users (
    user_id VARCHAR(8) NOT NULL,
    first_name VARCHAR(20) NOT NULL,
    last_name VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type VARCHAR(1) NOT NULL DEFAULT 'U',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);

CREATE TABLE customers (
    cust_id BIGINT NOT NULL,
    first_name VARCHAR(25) NOT NULL,
    middle_name VARCHAR(25),
    last_name VARCHAR(25) NOT NULL,
    addr_line_1 VARCHAR(50),
    addr_line_2 VARCHAR(50),
    addr_line_3 VARCHAR(50),
    addr_state_cd VARCHAR(2),
    addr_country_cd VARCHAR(3),
    addr_zip VARCHAR(10),
    phone_num_1 VARCHAR(15),
    phone_num_2 VARCHAR(15),
    ssn BIGINT,
    govt_issued_id VARCHAR(20),
    dob DATE,
    eft_account_id VARCHAR(10),
    pri_card_holder_ind VARCHAR(1),
    fico_credit_score INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cust_id)
);

CREATE TABLE accounts (
    acct_id BIGINT NOT NULL,
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    curr_bal DECIMAL(12,2) DEFAULT 0.00,
    credit_limit DECIMAL(12,2) DEFAULT 0.00,
    cash_credit_limit DECIMAL(12,2) DEFAULT 0.00,
    open_date DATE,
    expiration_date DATE,
    reissue_date DATE,
    curr_cyc_credit DECIMAL(12,2) DEFAULT 0.00,
    curr_cyc_debit DECIMAL(12,2) DEFAULT 0.00,
    addr_zip VARCHAR(10),
    group_id VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (acct_id)
);

CREATE TABLE cards (
    card_num VARCHAR(16) NOT NULL,
    acct_id BIGINT NOT NULL,
    cvv_cd VARCHAR(3) NOT NULL,
    embossed_name VARCHAR(50),
    expiration_date DATE,
    active_status VARCHAR(1) NOT NULL DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (card_num),
    FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE card_xrefs (
    card_num VARCHAR(16) NOT NULL,
    cust_id BIGINT NOT NULL,
    acct_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (card_num),
    FOREIGN KEY (cust_id) REFERENCES customers(cust_id),
    FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE transactions (
    tran_id VARCHAR(16) NOT NULL,
    type_cd VARCHAR(2) NOT NULL,
    cat_cd INT,
    source VARCHAR(10),
    description VARCHAR(100),
    amount DECIMAL(11,2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    card_num VARCHAR(16) NOT NULL,
    orig_ts TIMESTAMP,
    proc_ts TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tran_id)
);

CREATE TABLE daily_transactions (
    tran_id VARCHAR(16) NOT NULL,
    type_cd VARCHAR(2) NOT NULL,
    cat_cd INT,
    source VARCHAR(10),
    description VARCHAR(100),
    amount DECIMAL(11,2) NOT NULL,
    merchant_id BIGINT,
    merchant_name VARCHAR(50),
    merchant_city VARCHAR(50),
    merchant_zip VARCHAR(10),
    card_num VARCHAR(16) NOT NULL,
    orig_ts TIMESTAMP,
    proc_ts TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tran_id)
);

CREATE TABLE transaction_types (
    type_cd VARCHAR(2) NOT NULL,
    type_desc VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (type_cd)
);

CREATE TABLE transaction_categories (
    type_cd VARCHAR(2) NOT NULL,
    cat_cd INT NOT NULL,
    cat_desc VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (type_cd, cat_cd)
);

CREATE TABLE tran_cat_balances (
    acct_id BIGINT NOT NULL,
    type_cd VARCHAR(2) NOT NULL,
    cat_cd INT NOT NULL,
    balance DECIMAL(11,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (acct_id, type_cd, cat_cd),
    FOREIGN KEY (acct_id) REFERENCES accounts(acct_id)
);

CREATE TABLE disclosure_groups (
    acct_group_id VARCHAR(10) NOT NULL,
    tran_type_cd VARCHAR(2) NOT NULL,
    tran_cat_cd INT NOT NULL,
    int_rate DECIMAL(6,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (acct_group_id, tran_type_cd, tran_cat_cd)
);

-- Indexes for common query patterns (replaces VSAM alternate indexes)
CREATE INDEX idx_cards_acct_id ON cards(acct_id);
CREATE INDEX idx_card_xrefs_cust_id ON card_xrefs(cust_id);
CREATE INDEX idx_card_xrefs_acct_id ON card_xrefs(acct_id);
CREATE INDEX idx_transactions_card_num ON transactions(card_num);
CREATE INDEX idx_transactions_proc_ts ON transactions(proc_ts);
CREATE INDEX idx_daily_transactions_card_num ON daily_transactions(card_num);
