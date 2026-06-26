package com.carddemo.accountview.service;

import java.math.BigDecimal;

/**
 * The data the legacy program maps onto the CACTVWA screen on a successful read
 * (paragraphs {@code 1200-SETUP-SCREEN-VARS}, lines 471-523 of COACTVWC).
 * Combines the account-master fields with the associated customer-master fields.
 */
public record AccountDetails(
        // ---- Account master (ACCTDAT) ----
        String accountId,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String openDate,
        String expirationDate,
        String reissueDate,
        String groupId,
        // ---- Customer master (CUSTDAT) ----
        String customerId,
        /** CUST-SSN reformatted as XXX-XX-XXXX (COACTVWC lines 496-503). */
        String ssnFormatted,
        int ficoScore,
        String dateOfBirth,
        String firstName,
        String middleName,
        String lastName,
        String addrLine1,
        String addrLine2,
        String city,
        String stateCode,
        String zip,
        String countryCode,
        String phoneNum1,
        String phoneNum2,
        String govtIssuedId,
        String eftAccountId,
        String priCardHolderInd) {
}
