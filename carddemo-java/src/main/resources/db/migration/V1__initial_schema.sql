-- Initial schema for the CardDemo migration.
-- Types map from COBOL pictures: PIC 9(n) -> BIGINT / INTEGER, PIC X(n) -> VARCHAR(n),
-- PIC S9(10)V99 -> NUMERIC(12,2), date display fields -> DATE.

CREATE TABLE sec_user (
    usr_id    VARCHAR(8)   PRIMARY KEY,
    usr_fname VARCHAR(20),
    usr_lname VARCHAR(20),
    usr_pwd   VARCHAR(100) NOT NULL,
    usr_type  VARCHAR(1)   NOT NULL
);

CREATE TABLE customer (
    cust_id             BIGINT      PRIMARY KEY,
    first_name          VARCHAR(25),
    middle_name         VARCHAR(25),
    last_name           VARCHAR(25),
    addr_line_1         VARCHAR(50),
    addr_line_2         VARCHAR(50),
    addr_line_3         VARCHAR(50),
    addr_state_cd       VARCHAR(2),
    addr_country_cd     VARCHAR(3),
    addr_zip            VARCHAR(10),
    phone_num_1         VARCHAR(15),
    phone_num_2         VARCHAR(15),
    ssn                 BIGINT,
    govt_issued_id      VARCHAR(20),
    dob_yyyy_mm_dd      VARCHAR(10),
    eft_account_id      VARCHAR(10),
    pri_card_holder_ind VARCHAR(1),
    fico_credit_score   INTEGER
);

CREATE TABLE account (
    acct_id           BIGINT        PRIMARY KEY,
    active_status     VARCHAR(1),
    curr_bal          NUMERIC(12,2),
    credit_limit      NUMERIC(12,2),
    cash_credit_limit NUMERIC(12,2),
    open_date         DATE,
    expiration_date   DATE,
    reissue_date      DATE,
    curr_cyc_credit   NUMERIC(12,2),
    curr_cyc_debit    NUMERIC(12,2),
    addr_zip          VARCHAR(10),
    group_id          VARCHAR(10)
);

CREATE TABLE card (
    card_num        VARCHAR(16) PRIMARY KEY,
    acct_id         BIGINT,
    cvv_cd          VARCHAR(3),
    embossed_name   VARCHAR(50),
    expiration_date VARCHAR(10),
    active_status   VARCHAR(1)
);

CREATE TABLE card_xref (
    card_num VARCHAR(16) PRIMARY KEY,
    cust_id  BIGINT,
    acct_id  BIGINT
);

CREATE TABLE transaction (
    tran_id            VARCHAR(16) PRIMARY KEY,
    tran_type_cd       VARCHAR(2),
    tran_cat_cd        INTEGER,
    tran_source        VARCHAR(10),
    tran_desc          VARCHAR(100),
    tran_amt           NUMERIC(11,2),
    tran_merchant_id   BIGINT,
    tran_merchant_name VARCHAR(50),
    tran_merchant_city VARCHAR(50),
    tran_merchant_zip  VARCHAR(10),
    tran_card_num      VARCHAR(16),
    tran_orig_ts       VARCHAR(26),
    tran_proc_ts       VARCHAR(26)
);

CREATE TABLE tran_cat_balance (
    acct_id BIGINT      NOT NULL,
    type_cd VARCHAR(2)  NOT NULL,
    cat_cd  INTEGER     NOT NULL,
    balance NUMERIC(11,2),
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);

CREATE INDEX idx_card_acct_id ON card (acct_id);
CREATE INDEX idx_card_xref_acct_id ON card_xref (acct_id);
CREATE INDEX idx_card_xref_cust_id ON card_xref (cust_id);
CREATE INDEX idx_transaction_card_num ON transaction (tran_card_num);
