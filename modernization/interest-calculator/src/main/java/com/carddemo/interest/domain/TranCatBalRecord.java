package com.carddemo.interest.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction category balance record. Mirrors copybook {@code CVTRA01Y} (RECLN=50),
 * the primary sequential input to CBACT04C.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranCatBalRecord {

    /** COBOL {@code TRANCAT-ACCT-ID PIC 9(11)} — account id (part of key). */
    private long tranCatAcctId;

    /** COBOL {@code TRANCAT-TYPE-CD PIC X(02)} — transaction type code (part of key). */
    private String tranCatTypeCd;

    /** COBOL {@code TRANCAT-CD PIC 9(04)} — transaction category code (part of key). */
    private String tranCatCd;

    /** COBOL {@code TRAN-CAT-BAL PIC S9(09)V99} — category balance to accrue interest on. */
    private BigDecimal tranCatBal;

    /** Parse a 50-byte fixed-width {@code CVTRA01Y} record. */
    public static TranCatBalRecord parse(String line) {
        long acctId = CobolCodec.parseNumeric(line.substring(0, 11));
        String typeCd = line.substring(11, 13);
        String catCd = line.substring(13, 17);
        BigDecimal bal = CobolCodec.parseZonedDecimal(line.substring(17, 28), 2);
        return new TranCatBalRecord(acctId, typeCd, catCd, bal);
    }
}
