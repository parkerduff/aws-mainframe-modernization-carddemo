package com.carddemo.interest.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Card cross-reference record. Mirrors copybook {@code CVACT03Y} (RECLN=50).
 * CBACT04C reads it by account id (alternate key) to obtain the card number
 * placed on the generated interest transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardXrefRecord {

    /** COBOL {@code XREF-CARD-NUM PIC X(16)} — primary card number. */
    private String xrefCardNum;

    /** COBOL {@code XREF-CUST-ID PIC 9(09)} — customer id. */
    private long xrefCustId;

    /** COBOL {@code XREF-ACCT-ID PIC 9(11)} — account id (alternate key). */
    private long xrefAcctId;

    /**
     * Parse a fixed-width {@code CVACT03Y} record. The ASCII seed file omits the
     * trailing 14-byte FILLER, so records may be 36 or 50 bytes long.
     */
    public static CardXrefRecord parse(String line) {
        String cardNum = CobolCodec.parseText(line.substring(0, 16));
        long custId = CobolCodec.parseNumeric(line.substring(16, 25));
        long acctId = CobolCodec.parseNumeric(line.substring(25, 36));
        return new CardXrefRecord(cardNum, custId, acctId);
    }
}
