package com.carddemo.interest.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction record. Mirrors copybook {@code CVTRA05Y} (RECLN=350).
 * CBACT04C writes one of these per non-zero interest accrual (1300-B-WRITE-TX).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRecord {

    /** COBOL {@code TRAN-ID PIC X(16)} — PARM-DATE (10) + zero-padded suffix (6). */
    private String tranId;

    /** COBOL {@code TRAN-TYPE-CD PIC X(02)} — always '01' for interest. */
    private String tranTypeCd;

    /** COBOL {@code TRAN-CAT-CD PIC 9(04)} — always '05' for interest. */
    private String tranCatCd;

    /** COBOL {@code TRAN-SOURCE PIC X(10)} — always 'System'. */
    private String tranSource;

    /** COBOL {@code TRAN-DESC PIC X(100)} — 'Int. for a/c ' + account id. */
    private String tranDesc;

    /** COBOL {@code TRAN-AMT PIC S9(09)V99} — the monthly interest amount. */
    private BigDecimal tranAmt;

    /** COBOL {@code TRAN-MERCHANT-ID PIC 9(09)} — 0. */
    private long tranMerchantId;

    /** COBOL {@code TRAN-MERCHANT-NAME PIC X(50)} — spaces. */
    private String tranMerchantName;

    /** COBOL {@code TRAN-MERCHANT-CITY PIC X(50)} — spaces. */
    private String tranMerchantCity;

    /** COBOL {@code TRAN-MERCHANT-ZIP PIC X(10)} — spaces. */
    private String tranMerchantZip;

    /** COBOL {@code TRAN-CARD-NUM PIC X(16)} — card number from the xref record. */
    private String tranCardNum;

    /** COBOL {@code TRAN-ORIG-TS PIC X(26)} — DB2-format origination timestamp. */
    private String tranOrigTs;

    /** COBOL {@code TRAN-PROC-TS PIC X(26)} — DB2-format processing timestamp. */
    private String tranProcTs;

    /** Render the record as a 350-byte fixed-width line, matching the COBOL WRITE layout. */
    public String toFixedWidth() {
        StringBuilder sb = new StringBuilder(350);
        sb.append(CobolCodec.padRight(tranId, 16));
        sb.append(CobolCodec.padRight(tranTypeCd, 2));
        sb.append(CobolCodec.padLeft(tranCatCd, 4, '0'));
        sb.append(CobolCodec.padRight(tranSource, 10));
        sb.append(CobolCodec.padRight(tranDesc, 100));
        sb.append(CobolCodec.formatZonedDecimal(tranAmt, 11, 2));
        sb.append(CobolCodec.formatNumeric(tranMerchantId, 9));
        sb.append(CobolCodec.padRight(tranMerchantName, 50));
        sb.append(CobolCodec.padRight(tranMerchantCity, 50));
        sb.append(CobolCodec.padRight(tranMerchantZip, 10));
        sb.append(CobolCodec.padRight(tranCardNum, 16));
        sb.append(CobolCodec.padRight(tranOrigTs, 26));
        sb.append(CobolCodec.padRight(tranProcTs, 26));
        sb.append(CobolCodec.padRight("", 20)); // FILLER PIC X(20)
        return sb.toString();
    }
}
