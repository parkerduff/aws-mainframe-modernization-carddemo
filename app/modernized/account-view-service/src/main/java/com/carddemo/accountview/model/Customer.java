package com.carddemo.accountview.model;

/**
 * Mirror of the COBOL CUSTOMER-RECORD copybook (CVCUS01Y, RECLN 500).
 *
 * <pre>
 * 05 CUST-ID                 PIC 9(09).
 * 05 CUST-FIRST-NAME         PIC X(25).
 * 05 CUST-MIDDLE-NAME        PIC X(25).
 * 05 CUST-LAST-NAME          PIC X(25).
 * 05 CUST-ADDR-LINE-1        PIC X(50).
 * 05 CUST-ADDR-LINE-2        PIC X(50).
 * 05 CUST-ADDR-LINE-3        PIC X(50).
 * 05 CUST-ADDR-STATE-CD      PIC X(02).
 * 05 CUST-ADDR-COUNTRY-CD    PIC X(03).
 * 05 CUST-ADDR-ZIP           PIC X(10).
 * 05 CUST-PHONE-NUM-1        PIC X(15).
 * 05 CUST-PHONE-NUM-2        PIC X(15).
 * 05 CUST-SSN                PIC 9(09).
 * 05 CUST-GOVT-ISSUED-ID     PIC X(20).
 * 05 CUST-DOB-YYYY-MM-DD     PIC X(10).
 * 05 CUST-EFT-ACCOUNT-ID     PIC X(10).
 * 05 CUST-PRI-CARD-HOLDER-IND PIC X(01).
 * 05 CUST-FICO-CREDIT-SCORE  PIC 9(03).
 * </pre>
 */
public record Customer(
        String customerId,
        String firstName,
        String middleName,
        String lastName,
        String addrLine1,
        String addrLine2,
        String addrLine3,
        String stateCode,
        String countryCode,
        String zip,
        String phoneNum1,
        String phoneNum2,
        String ssn,
        String govtIssuedId,
        String dateOfBirth,
        String eftAccountId,
        String priCardHolderInd,
        int ficoScore) {
}
