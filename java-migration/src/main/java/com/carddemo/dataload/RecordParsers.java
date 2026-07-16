package com.carddemo.dataload;

import com.carddemo.domain.Account;
import com.carddemo.domain.CardXref;
import com.carddemo.domain.DailyTransaction;
import com.carddemo.domain.TranCatBalance;
import com.carddemo.domain.TranCatBalanceId;

/**
 * Pure functions that parse a single fixed-width line into a domain entity.
 * Field widths come directly from the copybook PIC clauses (app/cpy/) and are
 * consumed in copybook order via {@link FixedWidthRecord}.
 */
public final class RecordParsers {

    /** Record length of ACCOUNT-RECORD (CVACT01Y). */
    public static final int ACCOUNT_LEN = 300;
    /** Record length of CARD-XREF-RECORD (CVACT03Y). */
    public static final int CARD_XREF_LEN = 50;
    /** Record length of TRAN-CAT-BAL-RECORD (CVTRA01Y). */
    public static final int TRAN_CAT_BAL_LEN = 50;
    /** Record length of DALYTRAN-RECORD (CVTRA06Y). */
    public static final int DAILY_TRANSACTION_LEN = 350;

    private static final int AMOUNT_SCALE = 2;

    private RecordParsers() {
    }

    /** Parses one ACCOUNT-RECORD line (copybook CVACT01Y). */
    public static Account parseAccount(String line) {
        FixedWidthRecord r = FixedWidthRecord.of(line, ACCOUNT_LEN);
        Account a = new Account();
        a.setAcctId(r.unsignedLong(11));               // ACCT-ID           PIC 9(11)
        a.setAcctActiveStatus(r.text(1));              // ACCT-ACTIVE-STATUS PIC X(01)
        a.setAcctCurrBal(r.amount(12, AMOUNT_SCALE));  // ACCT-CURR-BAL      PIC S9(10)V99
        a.setAcctCreditLimit(r.amount(12, AMOUNT_SCALE));       // ACCT-CREDIT-LIMIT      PIC S9(10)V99
        a.setAcctCashCreditLimit(r.amount(12, AMOUNT_SCALE));   // ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99
        a.setAcctOpenDate(r.text(10));                 // ACCT-OPEN-DATE     PIC X(10)
        a.setAcctExpirationDate(r.text(10));           // ACCT-EXPIRAION-DATE PIC X(10)
        a.setAcctReissueDate(r.text(10));              // ACCT-REISSUE-DATE  PIC X(10)
        a.setAcctCurrCycCredit(r.amount(12, AMOUNT_SCALE)); // ACCT-CURR-CYC-CREDIT PIC S9(10)V99
        a.setAcctCurrCycDebit(r.amount(12, AMOUNT_SCALE));  // ACCT-CURR-CYC-DEBIT  PIC S9(10)V99
        a.setAcctAddrZip(r.text(10));                  // ACCT-ADDR-ZIP      PIC X(10)
        a.setAcctGroupId(r.text(10));                  // ACCT-GROUP-ID      PIC X(10)
        // remaining 178 bytes are FILLER
        return a;
    }

    /** Parses one CARD-XREF-RECORD line (copybook CVACT03Y). */
    public static CardXref parseCardXref(String line) {
        FixedWidthRecord r = FixedWidthRecord.of(line, CARD_XREF_LEN);
        CardXref x = new CardXref();
        x.setXrefCardNum(r.text(16)); // XREF-CARD-NUM PIC X(16)
        x.setXrefCustId(r.unsignedLong(9));  // XREF-CUST-ID  PIC 9(09)
        x.setXrefAcctId(r.unsignedLong(11)); // XREF-ACCT-ID  PIC 9(11)
        // remaining 14 bytes are FILLER
        return x;
    }

    /** Parses one TRAN-CAT-BAL-RECORD line (copybook CVTRA01Y). */
    public static TranCatBalance parseTranCatBalance(String line) {
        FixedWidthRecord r = FixedWidthRecord.of(line, TRAN_CAT_BAL_LEN);
        TranCatBalanceId id = new TranCatBalanceId();
        id.setTrancatAcctId(r.unsignedLong(11)); // TRANCAT-ACCT-ID PIC 9(11)
        id.setTrancatTypeCd(r.text(2));          // TRANCAT-TYPE-CD PIC X(02)
        id.setTrancatCd(r.unsignedInt(4));       // TRANCAT-CD      PIC 9(04)
        TranCatBalance b = new TranCatBalance();
        b.setId(id);
        b.setTranCatBal(r.amount(11, AMOUNT_SCALE)); // TRAN-CAT-BAL PIC S9(09)V99
        // remaining 22 bytes are FILLER
        return b;
    }

    /** Parses one DALYTRAN-RECORD line (copybook CVTRA06Y). */
    public static DailyTransaction parseDailyTransaction(String line) {
        FixedWidthRecord r = FixedWidthRecord.of(line, DAILY_TRANSACTION_LEN);
        DailyTransaction d = new DailyTransaction();
        d.setDalytranId(r.text(16));            // DALYTRAN-ID        PIC X(16)
        d.setDalytranTypeCd(r.text(2));         // DALYTRAN-TYPE-CD   PIC X(02)
        d.setDalytranCatCd(r.unsignedInt(4));   // DALYTRAN-CAT-CD    PIC 9(04)
        d.setDalytranSource(r.text(10));        // DALYTRAN-SOURCE    PIC X(10)
        d.setDalytranDesc(r.text(100));         // DALYTRAN-DESC      PIC X(100)
        d.setDalytranAmt(r.amount(11, AMOUNT_SCALE)); // DALYTRAN-AMT PIC S9(09)V99
        d.setDalytranMerchantId(r.unsignedLong(9));   // DALYTRAN-MERCHANT-ID   PIC 9(09)
        d.setDalytranMerchantName(r.text(50));  // DALYTRAN-MERCHANT-NAME PIC X(50)
        d.setDalytranMerchantCity(r.text(50));  // DALYTRAN-MERCHANT-CITY PIC X(50)
        d.setDalytranMerchantZip(r.text(10));   // DALYTRAN-MERCHANT-ZIP  PIC X(10)
        d.setDalytranCardNum(r.text(16));       // DALYTRAN-CARD-NUM  PIC X(16)
        d.setDalytranOrigTs(r.text(26));        // DALYTRAN-ORIG-TS   PIC X(26)
        d.setDalytranProcTs(r.text(26));        // DALYTRAN-PROC-TS   PIC X(26)
        // remaining 20 bytes are FILLER
        return d;
    }
}
