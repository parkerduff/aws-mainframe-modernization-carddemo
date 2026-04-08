-- Seed data migrated from COBOL VSAM sample data files

-- Users (from USRSEC VSAM file)
INSERT INTO users (user_id, first_name, last_name, password, user_type) VALUES
('ADMIN001', 'ADMIN', 'USER', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'A'),
('USER0001', 'FIRST', 'USER', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'U'),
('USER0002', 'SECOND', 'USER', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'U'),
('USER0003', 'THIRD', 'USER', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'U');

-- Customers (from CUSTDATA VSAM file)
INSERT INTO customers (cust_id, first_name, middle_name, last_name, addr_line_1, addr_state_cd, addr_country_cd, addr_zip, phone_num_1, ssn, dob, fico_credit_score) VALUES
(1, 'JOHN', 'M', 'DOE', '123 MAIN ST', 'NY', 'USA', '10001', '2125551234', 123456789, '1985-03-15', 750),
(2, 'JANE', 'A', 'SMITH', '456 OAK AVE', 'CA', 'USA', '90210', '3105555678', 987654321, '1990-07-22', 680),
(3, 'ROBERT', 'J', 'JOHNSON', '789 PINE RD', 'TX', 'USA', '75001', '2145559012', 456789123, '1978-11-30', 720),
(4, 'MARIA', 'L', 'GARCIA', '321 ELM BLVD', 'FL', 'USA', '33101', '3055553456', 789123456, '1995-01-08', 695),
(5, 'WILLIAM', 'T', 'BROWN', '654 MAPLE DR', 'IL', 'USA', '60601', '3125557890', 321654987, '1982-06-25', 710);

-- Accounts (from ACCTDATA VSAM file)
INSERT INTO accounts (acct_id, active_status, curr_bal, credit_limit, cash_credit_limit, open_date, expiration_date, addr_zip, group_id) VALUES
(10000000001, 'Y', 1500.00, 5000.00, 1000.00, '2020-01-15', '2025-01-15', '10001', 'GRP001'),
(10000000002, 'Y', 2750.50, 10000.00, 2500.00, '2019-06-01', '2024-06-01', '90210', 'GRP001'),
(10000000003, 'Y', 500.25, 3000.00, 500.00, '2021-03-20', '2026-03-20', '75001', 'GRP002'),
(10000000004, 'Y', 4200.00, 15000.00, 5000.00, '2018-11-10', '2023-11-10', '33101', 'GRP002'),
(10000000005, 'N', 0.00, 7500.00, 1500.00, '2022-08-05', '2027-08-05', '60601', 'GRP001');

-- Cards (from CARDDATA VSAM file)
INSERT INTO cards (card_num, acct_id, cvv_cd, embossed_name, expiration_date, active_status) VALUES
('4111111111111111', 10000000001, '123', 'JOHN M DOE', '2025-01-15', 'Y'),
('4222222222222222', 10000000002, '456', 'JANE A SMITH', '2024-06-01', 'Y'),
('4333333333333333', 10000000003, '789', 'ROBERT J JOHNSON', '2026-03-20', 'Y'),
('4444444444444444', 10000000004, '012', 'MARIA L GARCIA', '2023-11-10', 'Y'),
('4555555555555555', 10000000005, '345', 'WILLIAM T BROWN', '2027-08-05', 'N');

-- Card cross-references (from CARDXREF VSAM file)
INSERT INTO card_xrefs (card_num, cust_id, acct_id) VALUES
('4111111111111111', 1, 10000000001),
('4222222222222222', 2, 10000000002),
('4333333333333333', 3, 10000000003),
('4444444444444444', 4, 10000000004),
('4555555555555555', 5, 10000000005);

-- Transaction types (from TRANTYPE VSAM file)
INSERT INTO transaction_types (type_cd, type_desc) VALUES
('SA', 'SALE'),
('RT', 'RETURN'),
('CR', 'CREDIT'),
('DB', 'DEBIT'),
('IN', 'INTEREST'),
('FE', 'FEE'),
('PY', 'PAYMENT'),
('CA', 'CASH ADVANCE');

-- Transaction categories (from TRANCATG VSAM file)
INSERT INTO transaction_categories (type_cd, cat_cd, cat_desc) VALUES
('SA', 5001, 'RETAIL PURCHASE'),
('SA', 5002, 'ONLINE PURCHASE'),
('SA', 5003, 'GROCERY'),
('SA', 5004, 'RESTAURANT'),
('SA', 5005, 'GAS STATION'),
('RT', 5001, 'RETAIL RETURN'),
('CA', 6001, 'ATM CASH ADVANCE'),
('CA', 6002, 'BANK CASH ADVANCE'),
('IN', 7001, 'MONTHLY INTEREST'),
('FE', 8001, 'LATE FEE'),
('FE', 8002, 'ANNUAL FEE'),
('PY', 9001, 'ONLINE PAYMENT'),
('PY', 9002, 'CHECK PAYMENT');

-- Sample transactions (from TRANSACT VSAM file)
INSERT INTO transactions (tran_id, type_cd, cat_cd, source, description, amount, merchant_id, merchant_name, merchant_city, merchant_zip, card_num, orig_ts, proc_ts) VALUES
('TRN0000000000001', 'SA', 5001, 'ONLINE', 'AMAZON PURCHASE', 125.99, 100000001, 'AMAZON.COM', 'SEATTLE', '98109', '4111111111111111', '2024-01-10 14:30:00', '2024-01-10 14:30:05'),
('TRN0000000000002', 'SA', 5003, 'POS', 'GROCERY SHOPPING', 87.45, 100000002, 'WHOLE FOODS', 'NEW YORK', '10001', '4111111111111111', '2024-01-11 10:15:00', '2024-01-11 10:15:03'),
('TRN0000000000003', 'SA', 5004, 'POS', 'DINNER', 62.30, 100000003, 'OLIVE GARDEN', 'LOS ANGELES', '90210', '4222222222222222', '2024-01-12 19:45:00', '2024-01-12 19:45:02'),
('TRN0000000000004', 'PY', 9001, 'ONLINE', 'MONTHLY PAYMENT', -500.00, 0, 'PAYMENT', '', '', '4222222222222222', '2024-01-15 08:00:00', '2024-01-15 08:00:01'),
('TRN0000000000005', 'CA', 6001, 'ATM', 'ATM WITHDRAWAL', 200.00, 100000004, 'CHASE ATM', 'DALLAS', '75001', '4333333333333333', '2024-01-13 16:20:00', '2024-01-13 16:20:04');

-- Disclosure groups (from DISCGRP VSAM file)
INSERT INTO disclosure_groups (acct_group_id, tran_type_cd, tran_cat_cd, int_rate) VALUES
('GRP001', 'SA', 5001, 19.99),
('GRP001', 'CA', 6001, 24.99),
('GRP001', 'IN', 7001, 0.00),
('GRP002', 'SA', 5001, 17.99),
('GRP002', 'CA', 6001, 22.99);

-- Transaction category balances (from TCATBALF VSAM file)
INSERT INTO tran_cat_balances (acct_id, type_cd, cat_cd, balance) VALUES
(10000000001, 'SA', 5001, 125.99),
(10000000001, 'SA', 5003, 87.45),
(10000000002, 'SA', 5004, 62.30),
(10000000002, 'PY', 9001, -500.00),
(10000000003, 'CA', 6001, 200.00);
