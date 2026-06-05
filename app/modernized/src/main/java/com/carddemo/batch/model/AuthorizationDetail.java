package com.carddemo.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Modern representation of the IMS pending-authorization <em>detail</em>
 * (child) segment, copybook {@code CIPAUDTY.cpy}.
 *
 * <p>The original {@code PA-AUTH-DATE-9C} field stores a 9's-complement Julian
 * date ({@code authDate = 99999 - PA-AUTH-DATE-9C}). That encoding is resolved
 * up-front here into a plain {@link LocalDate} so downstream logic can use
 * standard {@code java.time} date arithmetic.
 */
public record AuthorizationDetail(
        LocalDate authDate,          // derived from PA-AUTH-DATE-9C (9's complement Julian)
        LocalTime authTime,          // derived from PA-AUTH-TIME-9C
        String origDate,             // PA-AUTH-ORIG-DATE        PIC X(06)
        String origTime,             // PA-AUTH-ORIG-TIME        PIC X(06)
        String cardNum,              // PA-CARD-NUM              PIC X(16)
        String authType,             // PA-AUTH-TYPE             PIC X(04)
        String cardExpiryDate,       // PA-CARD-EXPIRY-DATE      PIC X(04)
        String messageType,          // PA-MESSAGE-TYPE          PIC X(06)
        String messageSource,        // PA-MESSAGE-SOURCE        PIC X(06)
        String authIdCode,           // PA-AUTH-ID-CODE          PIC X(06)
        String authRespCode,         // PA-AUTH-RESP-CODE        PIC X(02) ('00' = approved)
        String authRespReason,       // PA-AUTH-RESP-REASON      PIC X(04)
        int processingCode,          // PA-PROCESSING-CODE       PIC 9(06)
        BigDecimal transactionAmt,   // PA-TRANSACTION-AMT       PIC S9(10)V99 COMP-3
        BigDecimal approvedAmt,      // PA-APPROVED-AMT          PIC S9(10)V99 COMP-3
        String merchantCategoryCode, // PA-MERCHANT-CATAGORY-CODE PIC X(04)
        String acquirerCountryCode,  // PA-ACQR-COUNTRY-CODE     PIC X(03)
        int posEntryMode,            // PA-POS-ENTRY-MODE        PIC 9(02)
        String merchantId,           // PA-MERCHANT-ID           PIC X(15)
        String merchantName,         // PA-MERCHANT-NAME         PIC X(22)
        String merchantCity,         // PA-MERCHANT-CITY         PIC X(13)
        String merchantState,        // PA-MERCHANT-STATE        PIC X(02)
        String merchantZip,          // PA-MERCHANT-ZIP          PIC X(09)
        String transactionId,        // PA-TRANSACTION-ID        PIC X(15)
        String matchStatus,          // PA-MATCH-STATUS          PIC X(01)
        String authFraud,            // PA-AUTH-FRAUD            PIC X(01)
        String fraudReportDate       // PA-FRAUD-RPT-DATE        PIC X(08)
) {
    /** Authorization response code that marks an <em>approved</em> auth. */
    public static final String APPROVED_RESP_CODE = "00";

    /** True when this auth was approved ({@code PA-AUTH-RESP-CODE = '00'}). */
    public boolean isApproved() {
        return APPROVED_RESP_CODE.equals(authRespCode);
    }
}
