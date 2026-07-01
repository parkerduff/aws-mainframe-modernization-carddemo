package com.carddemo.interest.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account master record. Mirrors copybook {@code CVACT01Y} (RECLN=300).
 * CBACT04C reads it randomly by account id and rewrites it after posting the
 * accumulated interest for the account (paragraph 1050-UPDATE-ACCOUNT).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecord {

    /** COBOL {@code ACCT-ID PIC 9(11)} — primary key. */
    private long acctId;

    /** COBOL {@code ACCT-ACTIVE-STATUS PIC X(01)}. */
    private String acctActiveStatus;

    /** COBOL {@code ACCT-CURR-BAL PIC S9(10)V99} — current balance (interest is added here). */
    private BigDecimal acctCurrBal;

    /** COBOL {@code ACCT-CREDIT-LIMIT PIC S9(10)V99}. */
    private BigDecimal acctCreditLimit;

    /** COBOL {@code ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99}. */
    private BigDecimal acctCashCreditLimit;

    /** COBOL {@code ACCT-OPEN-DATE PIC X(10)} (YYYY-MM-DD). */
    private String acctOpenDate;

    /** COBOL {@code ACCT-EXPIRAION-DATE PIC X(10)} (YYYY-MM-DD). */
    private String acctExpirationDate;

    /** COBOL {@code ACCT-REISSUE-DATE PIC X(10)} (YYYY-MM-DD). */
    private String acctReissueDate;

    /** COBOL {@code ACCT-CURR-CYC-CREDIT PIC S9(10)V99} — reset to 0 on update. */
    private BigDecimal acctCurrCycCredit;

    /** COBOL {@code ACCT-CURR-CYC-DEBIT PIC S9(10)V99} — reset to 0 on update. */
    private BigDecimal acctCurrCycDebit;

    /** COBOL {@code ACCT-ADDR-ZIP PIC X(10)}. */
    private String acctAddrZip;

    /** COBOL {@code ACCT-GROUP-ID PIC X(10)} — disclosure group used for rate lookup. */
    private String acctGroupId;

    /** Parse a 300-byte fixed-width {@code CVACT01Y} record. */
    public static AccountRecord parse(String r) {
        return new AccountRecord(
                CobolCodec.parseNumeric(r.substring(0, 11)),
                r.substring(11, 12),
                CobolCodec.parseZonedDecimal(r.substring(12, 24), 2),
                CobolCodec.parseZonedDecimal(r.substring(24, 36), 2),
                CobolCodec.parseZonedDecimal(r.substring(36, 48), 2),
                CobolCodec.parseText(r.substring(48, 58)),
                CobolCodec.parseText(r.substring(58, 68)),
                CobolCodec.parseText(r.substring(68, 78)),
                CobolCodec.parseZonedDecimal(r.substring(78, 90), 2),
                CobolCodec.parseZonedDecimal(r.substring(90, 102), 2),
                CobolCodec.parseText(r.substring(102, 112)),
                r.substring(112, 122));
    }
}
