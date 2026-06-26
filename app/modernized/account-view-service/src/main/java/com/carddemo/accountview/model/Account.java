package com.carddemo.accountview.model;

import java.math.BigDecimal;

/**
 * Mirror of the COBOL ACCOUNT-RECORD copybook (CVACT01Y, RECLN 300).
 *
 * <pre>
 * 05 ACCT-ID                PIC 9(11).
 * 05 ACCT-ACTIVE-STATUS     PIC X(01).
 * 05 ACCT-CURR-BAL          PIC S9(10)V99.
 * 05 ACCT-CREDIT-LIMIT      PIC S9(10)V99.
 * 05 ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99.
 * 05 ACCT-OPEN-DATE         PIC X(10).
 * 05 ACCT-EXPIRAION-DATE    PIC X(10).
 * 05 ACCT-REISSUE-DATE      PIC X(10).
 * 05 ACCT-CURR-CYC-CREDIT   PIC S9(10)V99.
 * 05 ACCT-CURR-CYC-DEBIT    PIC S9(10)V99.
 * 05 ACCT-ADDR-ZIP          PIC X(10).
 * 05 ACCT-GROUP-ID          PIC X(10).
 * </pre>
 *
 * Signed fixed-point COMP/display amounts (S9(10)V99) are represented with
 * {@link BigDecimal} (scale 2) to preserve exact decimal arithmetic; never use
 * floating point for these fields.
 */
public record Account(
        String accountId,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        String openDate,
        String expirationDate,
        String reissueDate,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String addrZip,
        String groupId) {
}
