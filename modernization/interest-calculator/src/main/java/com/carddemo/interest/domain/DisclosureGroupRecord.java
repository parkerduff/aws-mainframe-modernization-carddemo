package com.carddemo.interest.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Disclosure group record. Mirrors copybook {@code CVTRA02Y} (RECLN=50).
 * Holds the interest rate applied to a (group id, transaction type, transaction
 * category) combination. CBACT04C reads it randomly by that composite key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisclosureGroupRecord {

    /** COBOL {@code DIS-ACCT-GROUP-ID PIC X(10)} — account group id (part of key). */
    private String disAcctGroupId;

    /** COBOL {@code DIS-TRAN-TYPE-CD PIC X(02)} — transaction type code (part of key). */
    private String disTranTypeCd;

    /** COBOL {@code DIS-TRAN-CAT-CD PIC 9(04)} — transaction category code (part of key). */
    private String disTranCatCd;

    /** COBOL {@code DIS-INT-RATE PIC S9(04)V99} — annual interest rate percentage. */
    private BigDecimal disIntRate;

    /** Parse a 50-byte fixed-width {@code CVTRA02Y} record. */
    public static DisclosureGroupRecord parse(String line) {
        String groupId = line.substring(0, 10);
        String typeCd = line.substring(10, 12);
        String catCd = line.substring(12, 16);
        BigDecimal rate = CobolCodec.parseZonedDecimal(line.substring(16, 22), 2);
        return new DisclosureGroupRecord(groupId, typeCd, catCd, rate);
    }
}
