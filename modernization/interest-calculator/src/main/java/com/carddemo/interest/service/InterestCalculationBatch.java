package com.carddemo.interest.service;

import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.domain.TranCatBalRecord;
import com.carddemo.interest.domain.TransactionRecord;
import com.carddemo.interest.repository.AccountRepository;
import com.carddemo.interest.repository.CardXrefRepository;
import com.carddemo.interest.repository.DisclosureGroupRepository;
import com.carddemo.interest.repository.TransactionWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Java modernization of the COBOL batch program <b>CBACT04C</b> — the CardDemo
 * interest calculator.
 *
 * <p>The program reads the transaction-category-balance file (TCATBAL) in account-key
 * order, accrues monthly interest per category using the rate from the disclosure-group
 * file, writes one interest transaction per non-zero accrual, and posts the accumulated
 * interest to the account master balance when the account key changes (and at end-of-file).
 *
 * <p>Method names deliberately preserve the COBOL paragraph numbers/names for traceability.
 * All monetary arithmetic uses {@link BigDecimal}; the monthly-interest formula truncates
 * to two decimals with {@link RoundingMode#DOWN} to match COBOL's non-{@code ROUNDED}
 * {@code COMPUTE} into a {@code PIC S9(09)V99} field.
 */
@Service
public class InterestCalculationBatch {

    /** Divisor in COBOL: {@code (TRAN-CAT-BAL * DIS-INT-RATE) / 1200}. */
    private static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(1200);

    /** WS-MONTHLY-INT / WS-TOTAL-INT / TRAN-AMT scale (V99). */
    private static final int MONEY_SCALE = 2;

    /** Fallback disclosure group id used when the account's group has no matching rate. */
    private static final String DEFAULT_GROUP_ID = "DEFAULT";

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionWriter transactionWriter;
    private final TimestampProvider timestampProvider;

    // --- WORKING-STORAGE equivalents (per-run state) ---
    private Long wsLastAcctNum;
    private BigDecimal wsTotalInt;
    private boolean wsFirstTime;
    private long wsRecordCount;
    private long wsTranidSuffix;

    // in-flight account/xref for the current key group (COBOL ACCOUNT-RECORD / CARD-XREF-RECORD)
    private AccountRecord currentAccount;
    private CardXrefRecord currentXref;

    public InterestCalculationBatch(AccountRepository accountRepository,
                                    CardXrefRepository cardXrefRepository,
                                    DisclosureGroupRepository disclosureGroupRepository,
                                    TransactionWriter transactionWriter,
                                    TimestampProvider timestampProvider) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionWriter = transactionWriter;
        this.timestampProvider = timestampProvider;
    }

    /**
     * PROCEDURE DIVISION main flow. Processes the transaction-category-balance records
     * (supplied in account-key order, as read sequentially from TCATBAL) and returns
     * run counters.
     *
     * @param parmDate the run date (EXTERNAL-PARMS PARM-DATE, {@code PIC X(10)}) used as the
     *                 transaction-id prefix
     * @param tranCatBalRecords TCATBAL records in account-key order
     */
    public Result process(String parmDate, List<TranCatBalRecord> tranCatBalRecords) {
        // Initialize WORKING-STORAGE (VALUE clauses).
        wsLastAcctNum = null;
        wsTotalInt = zero();
        wsFirstTime = true;
        wsRecordCount = 0;
        wsTranidSuffix = 0;
        currentAccount = null;
        currentXref = null;

        String parm = CobolPad(parmDate);

        for (TranCatBalRecord rec : tranCatBalRecords) {
            wsRecordCount++; // ADD 1 TO WS-RECORD-COUNT

            if (wsLastAcctNum == null || rec.getTranCatAcctId() != wsLastAcctNum) {
                if (!wsFirstTime) {
                    updateAccount1050();
                } else {
                    wsFirstTime = false;
                }
                wsTotalInt = zero();                       // MOVE 0 TO WS-TOTAL-INT
                wsLastAcctNum = rec.getTranCatAcctId();     // MOVE TRANCAT-ACCT-ID TO WS-LAST-ACCT-NUM
                currentAccount = getAcctData1100(rec.getTranCatAcctId());
                currentXref = getXrefData1110(rec.getTranCatAcctId());
            }

            DisclosureGroupRecord disc = getInterestRate1200(
                    currentAccount.getAcctGroupId(), rec.getTranCatTypeCd(), rec.getTranCatCd());

            if (disc.getDisIntRate().signum() != 0) { // IF DIS-INT-RATE NOT = 0
                computeInterest1300(rec, disc, parm);
                computeFees1400();
            }
        }

        // COBOL end-of-file branch performs 1050-UPDATE-ACCOUNT for the final account.
        if (!wsFirstTime) {
            updateAccount1050();
        }

        return new Result(wsRecordCount, wsTranidSuffix);
    }

    /**
     * 1050-UPDATE-ACCOUNT — post accumulated interest to the account balance and reset
     * the current-cycle credit/debit buckets, then REWRITE the account record.
     */
    void updateAccount1050() {
        currentAccount.setAcctCurrBal(currentAccount.getAcctCurrBal().add(wsTotalInt));
        currentAccount.setAcctCurrCycCredit(zero());
        currentAccount.setAcctCurrCycDebit(zero());
        accountRepository.save(currentAccount);
    }

    /** 1100-GET-ACCT-DATA — keyed READ of the account master. */
    AccountRecord getAcctData1100(long acctId) {
        return accountRepository.findById(acctId)
                .orElseThrow(() -> new IllegalStateException("ACCOUNT NOT FOUND: " + acctId));
    }

    /** 1110-GET-XREF-DATA — keyed READ of the card cross-reference by account id. */
    CardXrefRecord getXrefData1110(long acctId) {
        return cardXrefRepository.findByAccountId(acctId)
                .orElseThrow(() -> new IllegalStateException("XREF NOT FOUND: " + acctId));
    }

    /**
     * 1200-GET-INTEREST-RATE — read the disclosure group by (group id, type, category).
     * On a "record not found" (COBOL status 23) it retries against the DEFAULT group
     * (paragraph 1200-A-GET-DEFAULT-INT-RATE); a still-missing DEFAULT is a fatal error,
     * matching the COBOL abend.
     */
    DisclosureGroupRecord getInterestRate1200(String acctGroupId, String tranTypeCd, String tranCatCd) {
        Optional<DisclosureGroupRecord> found =
                disclosureGroupRepository.find(acctGroupId, tranTypeCd, tranCatCd);
        if (found.isPresent()) {
            return found.get();
        }
        return disclosureGroupRepository.find(DEFAULT_GROUP_ID, tranTypeCd, tranCatCd)
                .orElseThrow(() -> new IllegalStateException(
                        "DISCLOSURE GROUP RECORD MISSING for DEFAULT/" + tranTypeCd + "/" + tranCatCd));
    }

    /**
     * 1300-COMPUTE-INTEREST — {@code WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200},
     * accumulate into WS-TOTAL-INT, then write the interest transaction.
     */
    void computeInterest1300(TranCatBalRecord rec, DisclosureGroupRecord disc, String parmDate) {
        BigDecimal monthlyInterest = rec.getTranCatBal()
                .multiply(disc.getDisIntRate())
                .divide(MONTHLY_DIVISOR, MONEY_SCALE, RoundingMode.DOWN);
        wsTotalInt = wsTotalInt.add(monthlyInterest);
        writeTransaction1300B(rec, monthlyInterest, parmDate);
    }

    /** 1300-B-WRITE-TX — build and write the interest transaction record. */
    void writeTransaction1300B(TranCatBalRecord rec, BigDecimal monthlyInterest, String parmDate) {
        wsTranidSuffix++; // ADD 1 TO WS-TRANID-SUFFIX

        TransactionRecord tx = new TransactionRecord();
        tx.setTranId(parmDate + String.format("%06d", wsTranidSuffix));
        tx.setTranTypeCd("01");
        tx.setTranCatCd("05");
        tx.setTranSource("System");
        tx.setTranDesc("Int. for a/c " + String.format("%011d", currentAccount.getAcctId()));
        tx.setTranAmt(monthlyInterest);
        tx.setTranMerchantId(0);
        tx.setTranMerchantName("");
        tx.setTranMerchantCity("");
        tx.setTranMerchantZip("");
        tx.setTranCardNum(currentXref.getXrefCardNum());
        String ts = timestampProvider.currentDb2Timestamp();
        tx.setTranOrigTs(ts);
        tx.setTranProcTs(ts);

        transactionWriter.write(tx);
    }

    /** 1400-COMPUTE-FEES — "To be implemented" in the COBOL source; kept as a no-op. */
    void computeFees1400() {
        // Intentionally empty: the COBOL paragraph body is a stub (EXIT only).
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    /** PARM-DATE is PIC X(10); pad/truncate to exactly 10 bytes as the COBOL field would hold. */
    private static String CobolPad(String parmDate) {
        String p = parmDate == null ? "" : parmDate;
        if (p.length() > 10) {
            return p.substring(0, 10);
        }
        StringBuilder sb = new StringBuilder(p);
        while (sb.length() < 10) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Run counters returned by {@link #process}. */
    public record Result(long recordsRead, long transactionsWritten) {
    }
}
