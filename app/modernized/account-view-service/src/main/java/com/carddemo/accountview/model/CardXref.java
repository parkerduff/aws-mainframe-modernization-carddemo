package com.carddemo.accountview.model;

/**
 * Mirror of the COBOL CARD-XREF-RECORD copybook (CVACT03Y, RECLN 50).
 *
 * <pre>
 * 05 XREF-CARD-NUM   PIC X(16).
 * 05 XREF-CUST-ID    PIC 9(09).
 * 05 XREF-ACCT-ID    PIC 9(11).
 * 05 FILLER          PIC X(14).
 * </pre>
 *
 * In the legacy CICS program this file is read via the alternate index
 * CXACAIX keyed by account id (9200-GETCARDXREF-BYACCT).
 */
public record CardXref(
        String cardNumber,
        String customerId,
        String accountId) {
}
