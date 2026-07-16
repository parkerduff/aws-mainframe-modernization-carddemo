-- CardDemo transaction subsystem: initial relational schema (Phase 1 VSAM -> relational).
-- Column precision mirrors the COBOL copybooks in app/cpy/.
--   PIC S9(09)V99 -> DECIMAL(11,2)
--   PIC S9(10)V99 -> DECIMAL(12,2)
--   PIC 9(nn)     -> integer/BIGINT
--   PIC X(nn)     -> CHAR/VARCHAR(nn)

-- ACCOUNT  (copybook CVACT01Y, ACCOUNT-RECORD, RECLN 300)
CREATE TABLE ACCOUNT (
    ACCT_ID                 BIGINT         NOT NULL,   -- PIC 9(11)
    ACCT_ACTIVE_STATUS      CHAR(1),                   -- PIC X(01)
    ACCT_CURR_BAL           DECIMAL(12,2),             -- PIC S9(10)V99
    ACCT_CREDIT_LIMIT       DECIMAL(12,2),             -- PIC S9(10)V99
    ACCT_CASH_CREDIT_LIMIT  DECIMAL(12,2),             -- PIC S9(10)V99
    ACCT_OPEN_DATE          CHAR(10),                  -- PIC X(10)
    ACCT_EXPIRATION_DATE    CHAR(10),                  -- PIC X(10)
    ACCT_REISSUE_DATE       CHAR(10),                  -- PIC X(10)
    ACCT_CURR_CYC_CREDIT    DECIMAL(12,2),             -- PIC S9(10)V99
    ACCT_CURR_CYC_DEBIT     DECIMAL(12,2),             -- PIC S9(10)V99
    ACCT_ADDR_ZIP           CHAR(10),                  -- PIC X(10)
    ACCT_GROUP_ID           CHAR(10),                  -- PIC X(10)
    CONSTRAINT PK_ACCOUNT PRIMARY KEY (ACCT_ID)
);

-- CARD_XREF  (copybook CVACT03Y, CARD-XREF-RECORD, RECLN 50)
CREATE TABLE CARD_XREF (
    XREF_CARD_NUM   CHAR(16)    NOT NULL,   -- PIC X(16)
    XREF_CUST_ID    BIGINT,                 -- PIC 9(09)
    XREF_ACCT_ID    BIGINT,                 -- PIC 9(11)
    CONSTRAINT PK_CARD_XREF PRIMARY KEY (XREF_CARD_NUM)
);

-- TRAN_CAT_BALANCE  (copybook CVTRA01Y, TRAN-CAT-BAL-RECORD, RECLN 50)
CREATE TABLE TRAN_CAT_BALANCE (
    TRANCAT_ACCT_ID     BIGINT      NOT NULL,   -- PIC 9(11)
    TRANCAT_TYPE_CD     CHAR(2)     NOT NULL,   -- PIC X(02)
    TRANCAT_CD          INTEGER     NOT NULL,   -- PIC 9(04)
    TRAN_CAT_BAL        DECIMAL(11,2),          -- PIC S9(09)V99
    CONSTRAINT PK_TRAN_CAT_BALANCE PRIMARY KEY (TRANCAT_ACCT_ID, TRANCAT_TYPE_CD, TRANCAT_CD)
);

-- TRANSACTION  (copybook CVTRA05Y, TRAN-RECORD, RECLN 350)
CREATE TABLE TRANSACTION (
    TRAN_ID             CHAR(16)    NOT NULL,   -- PIC X(16)
    TRAN_TYPE_CD        CHAR(2),                -- PIC X(02)
    TRAN_CAT_CD         INTEGER,                -- PIC 9(04)
    TRAN_SOURCE         VARCHAR(10),            -- PIC X(10)
    TRAN_DESC           VARCHAR(100),           -- PIC X(100)
    TRAN_AMT            DECIMAL(11,2),          -- PIC S9(09)V99
    TRAN_MERCHANT_ID    BIGINT,                 -- PIC 9(09)
    TRAN_MERCHANT_NAME  VARCHAR(50),            -- PIC X(50)
    TRAN_MERCHANT_CITY  VARCHAR(50),            -- PIC X(50)
    TRAN_MERCHANT_ZIP   VARCHAR(10),            -- PIC X(10)
    TRAN_CARD_NUM       CHAR(16),               -- PIC X(16)
    TRAN_ORIG_TS        CHAR(26),               -- PIC X(26)
    TRAN_PROC_TS        CHAR(26),               -- PIC X(26)
    CONSTRAINT PK_TRANSACTION PRIMARY KEY (TRAN_ID)
);

-- DAILY_TRANSACTION  (copybook CVTRA06Y, DALYTRAN-RECORD, RECLN 350)
-- Staging table for input records read by the posting engine (CBTRN02C).
-- Uses a surrogate key because daily files may repeat a DALYTRAN-ID across runs.
CREATE TABLE DAILY_TRANSACTION (
    ID                      BIGINT          NOT NULL AUTO_INCREMENT,
    DALYTRAN_ID             CHAR(16),               -- PIC X(16)
    DALYTRAN_TYPE_CD        CHAR(2),                -- PIC X(02)
    DALYTRAN_CAT_CD         INTEGER,                -- PIC 9(04)
    DALYTRAN_SOURCE         VARCHAR(10),            -- PIC X(10)
    DALYTRAN_DESC           VARCHAR(100),           -- PIC X(100)
    DALYTRAN_AMT            DECIMAL(11,2),          -- PIC S9(09)V99
    DALYTRAN_MERCHANT_ID    BIGINT,                 -- PIC 9(09)
    DALYTRAN_MERCHANT_NAME  VARCHAR(50),            -- PIC X(50)
    DALYTRAN_MERCHANT_CITY  VARCHAR(50),            -- PIC X(50)
    DALYTRAN_MERCHANT_ZIP   VARCHAR(10),            -- PIC X(10)
    DALYTRAN_CARD_NUM       CHAR(16),               -- PIC X(16)
    DALYTRAN_ORIG_TS        CHAR(26),               -- PIC X(26)
    DALYTRAN_PROC_TS        CHAR(26),               -- PIC X(26)
    CONSTRAINT PK_DAILY_TRANSACTION PRIMARY KEY (ID)
);
