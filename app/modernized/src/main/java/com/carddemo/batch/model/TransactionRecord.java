package com.carddemo.batch.model;

import java.math.BigDecimal;

/**
 * Modern representation of a CardDemo transaction record.
 *
 * <p>Mirrors the COBOL {@code TRAN-RECORD} layout defined in copybook
 * {@code CVTRA05Y.cpy}. Field widths from the original PIC clauses are kept in
 * the comments for traceability; callers are responsible for supplying values
 * that respect those widths.
 */
public record TransactionRecord(
        String id,             // TRAN-ID            PIC X(16)
        String typeCd,         // TRAN-TYPE-CD       PIC X(02)
        int catCd,             // TRAN-CAT-CD        PIC 9(04)
        String source,         // TRAN-SOURCE        PIC X(10)
        String desc,           // TRAN-DESC          PIC X(100)
        BigDecimal amount,     // TRAN-AMT           PIC S9(09)V99
        long merchantId,       // TRAN-MERCHANT-ID   PIC 9(09)
        String merchantName,   // TRAN-MERCHANT-NAME PIC X(50)
        String merchantCity,   // TRAN-MERCHANT-CITY PIC X(50)
        String merchantZip,    // TRAN-MERCHANT-ZIP  PIC X(10)
        String cardNum,        // TRAN-CARD-NUM      PIC X(16)
        String origTimestamp,  // TRAN-ORIG-TS       PIC X(26)
        String procTimestamp   // TRAN-PROC-TS       PIC X(26)
) {
    /**
     * Returns the processing date portion ({@code YYYY-MM-DD}) of the
     * processing timestamp, mirroring the COBOL reference
     * {@code TRAN-PROC-TS (1:10)} used for date filtering.
     */
    public String procDate() {
        if (procTimestamp == null || procTimestamp.length() < 10) {
            return procTimestamp;
        }
        return procTimestamp.substring(0, 10);
    }
}
