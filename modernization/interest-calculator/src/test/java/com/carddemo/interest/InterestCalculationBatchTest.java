package com.carddemo.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carddemo.interest.domain.AccountRecord;
import com.carddemo.interest.domain.CardXrefRecord;
import com.carddemo.interest.domain.DisclosureGroupRecord;
import com.carddemo.interest.domain.TranCatBalRecord;
import com.carddemo.interest.domain.TransactionRecord;
import com.carddemo.interest.repository.InMemoryRepositories;
import com.carddemo.interest.service.InterestCalculationBatch;
import com.carddemo.interest.service.TimestampProvider;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Equivalence tests for the modernized CBACT04C. Each test states the COBOL rule it
 * pins down; expected monetary values are computed with the COBOL formula
 * {@code trunc2((TRAN-CAT-BAL * DIS-INT-RATE) / 1200)}.
 */
class InterestCalculationBatchTest {

    private static final String FIXED_TS = "2024-01-15-10.30.00.000000";
    private static final String PARM_DATE = "2024-01-15";

    private InMemoryRepositories.Accounts accounts;
    private InMemoryRepositories.CardXrefs xrefs;
    private InMemoryRepositories.DisclosureGroups discGroups;
    private InMemoryRepositories.CollectingTransactionWriter txWriter;
    private InterestCalculationBatch batch;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryRepositories.Accounts();
        xrefs = new InMemoryRepositories.CardXrefs();
        discGroups = new InMemoryRepositories.DisclosureGroups();
        txWriter = new InMemoryRepositories.CollectingTransactionWriter();
        batch = new InterestCalculationBatch(accounts, xrefs, discGroups, txWriter, () -> FIXED_TS);
    }

    private AccountRecord account(long id, String groupId, String balance) {
        AccountRecord a = new AccountRecord(id, "Y", new BigDecimal(balance),
                new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                "2014-11-20", "2025-05-20", "2025-05-20",
                new BigDecimal("10.00"), new BigDecimal("20.00"), "0000000000", groupId);
        accounts.put(a);
        return a;
    }

    private void xref(long acctId, String card) {
        xrefs.put(new CardXrefRecord(card, 123456789L, acctId));
    }

    private void rate(String groupId, String type, String cat, String rate) {
        discGroups.put(new DisclosureGroupRecord(groupId, type, cat, new BigDecimal(rate)));
    }

    private TranCatBalRecord tcb(long acctId, String type, String cat, String bal) {
        return new TranCatBalRecord(acctId, type, cat, new BigDecimal(bal));
    }

    @Test
    void computesMonthlyInterestUsingTruncationNotRounding() {
        // 100.00 * 23.00 / 1200 = 1.91666...; COBOL COMPUTE (no ROUNDED) truncates to 1.91
        account(1, "A000000000", "0.00");
        xref(1, "1111222233334444");
        rate("A000000000", "01", "0001", "23.00");

        batch.process(PARM_DATE, List.of(tcb(1, "01", "0001", "100.00")));

        assertThat(txWriter.getWritten()).hasSize(1);
        assertThat(txWriter.getWritten().get(0).getTranAmt()).isEqualByComparingTo("1.91");
    }

    @Test
    void postsAccumulatedInterestToAccountBalanceAndResetsCycleBuckets() {
        // cat1: 1000.00 * 15.00 /1200 = 12.50 ; cat2: 194.00 * 25.00 /1200 = 4.0416.. -> 4.04
        AccountRecord acct = account(1, "A000000000", "500.00");
        xref(1, "1111222233334444");
        rate("A000000000", "01", "0001", "15.00");
        rate("A000000000", "01", "0002", "25.00");

        batch.process(PARM_DATE, List.of(
                tcb(1, "01", "0001", "1000.00"),
                tcb(1, "01", "0002", "194.00")));

        // total interest 16.54 added to 500.00 -> 516.54; cycle buckets zeroed
        assertThat(accounts.findById(1)).get().satisfies(a -> {
            assertThat(a.getAcctCurrBal()).isEqualByComparingTo("516.54");
            assertThat(a.getAcctCurrCycCredit()).isEqualByComparingTo("0.00");
            assertThat(a.getAcctCurrCycDebit()).isEqualByComparingTo("0.00");
        });
        assertThat(txWriter.getWritten()).hasSize(2);
        assertThat(acct.getAcctCurrBal()).isEqualByComparingTo("516.54");
    }

    @Test
    void skipsInterestAndTransactionWhenRateIsZero() {
        account(1, "A000000000", "500.00");
        xref(1, "1111222233334444");
        rate("A000000000", "02", "0001", "0.00");

        batch.process(PARM_DATE, List.of(tcb(1, "02", "0001", "1000.00")));

        // no transaction written; balance unchanged (interest 0) but 1050 still zeroes cycle buckets
        assertThat(txWriter.getWritten()).isEmpty();
        assertThat(accounts.findById(1)).get().satisfies(a -> {
            assertThat(a.getAcctCurrBal()).isEqualByComparingTo("500.00");
            assertThat(a.getAcctCurrCycCredit()).isEqualByComparingTo("0.00");
        });
    }

    @Test
    void handlesNegativeBalanceInterest() {
        account(1, "A000000000", "0.00");
        xref(1, "1111222233334444");
        rate("A000000000", "01", "0001", "18.00");

        batch.process(PARM_DATE, List.of(tcb(1, "01", "0001", "-500.00")));

        // -500.00 * 18.00 / 1200 = -7.50
        assertThat(txWriter.getWritten().get(0).getTranAmt()).isEqualByComparingTo("-7.50");
        assertThat(accounts.findById(1)).get()
                .extracting(AccountRecord::getAcctCurrBal)
                .satisfies(b -> assertThat((BigDecimal) b).isEqualByComparingTo("-7.50"));
    }

    @Test
    void fallsBackToDefaultGroupWhenAccountGroupHasNoRate() {
        // account group has no matching rate; DEFAULT group supplies it (paragraph 1200-A)
        account(1, "MISSINGGRP", "0.00");
        xref(1, "1111222233334444");
        rate("DEFAULT", "01", "0001", "12.00");

        batch.process(PARM_DATE, List.of(tcb(1, "01", "0001", "1000.00")));

        // 1000.00 * 12.00 / 1200 = 10.00
        assertThat(txWriter.getWritten().get(0).getTranAmt()).isEqualByComparingTo("10.00");
    }

    @Test
    void abendsWhenNeitherGroupNorDefaultRateExists() {
        account(1, "MISSINGGRP", "0.00");
        xref(1, "1111222233334444");

        assertThatThrownBy(() -> batch.process(PARM_DATE, List.of(tcb(1, "01", "0001", "1000.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DISCLOSURE GROUP RECORD MISSING");
    }

    @Test
    void buildsInterestTransactionRecordExactlyLikeParagraph1300B() {
        account(1, "A000000000", "0.00");
        xref(1, "4444333322221111");
        rate("A000000000", "01", "0001", "15.00");

        batch.process(PARM_DATE, List.of(tcb(1, "01", "0001", "1000.00")));

        TransactionRecord tx = txWriter.getWritten().get(0);
        assertThat(tx.getTranId()).isEqualTo("2024-01-15000001"); // PARM-DATE(10) + suffix 9(06)
        assertThat(tx.getTranTypeCd()).isEqualTo("01");
        assertThat(tx.getTranCatCd()).isEqualTo("05");
        assertThat(tx.getTranSource()).isEqualTo("System");
        assertThat(tx.getTranDesc()).isEqualTo("Int. for a/c 00000000001");
        assertThat(tx.getTranCardNum()).isEqualTo("4444333322221111");
        assertThat(tx.getTranOrigTs()).isEqualTo(FIXED_TS);
        assertThat(tx.getTranProcTs()).isEqualTo(FIXED_TS);
        // fixed-width rendering is exactly 350 bytes with the interest amount zoned-encoded
        String line = tx.toFixedWidth();
        assertThat(line).hasSize(350);
        // TRAN-ID(16) + TRAN-TYPE-CD(01) + TRAN-CAT-CD numeric 0005 + TRAN-SOURCE
        assertThat(line).startsWith("2024-01-15000001010005System");
        // TRAN-AMT 12.50 zoned-encoded as 0000000125{ at the S9(09)V99 offset
        assertThat(line).contains("0000000125{");
    }

    @Test
    void transactionIdSuffixIncrementsGloballyAcrossAccounts() {
        account(1, "A000000000", "0.00");
        account(2, "A000000000", "0.00");
        xref(1, "1111111111111111");
        xref(2, "2222222222222222");
        rate("A000000000", "01", "0001", "15.00");

        InterestCalculationBatch.Result result = batch.process(PARM_DATE, List.of(
                tcb(1, "01", "0001", "1000.00"),
                tcb(2, "01", "0001", "1000.00")));

        assertThat(result.recordsRead()).isEqualTo(2);
        assertThat(result.transactionsWritten()).isEqualTo(2);
        assertThat(txWriter.getWritten().get(0).getTranId()).isEqualTo("2024-01-15000001");
        assertThat(txWriter.getWritten().get(1).getTranId()).isEqualTo("2024-01-15000002");
        // both accounts posted (one 1050 on key change, one on EOF)
        assertThat(accounts.findById(1)).get()
                .extracting(AccountRecord::getAcctCurrBal).isEqualTo(new BigDecimal("12.50"));
        assertThat(accounts.findById(2)).get()
                .extracting(AccountRecord::getAcctCurrBal).isEqualTo(new BigDecimal("12.50"));
    }

    @Test
    void updatesEachAccountExactlyOnce() {
        account(1, "A000000000", "100.00");
        xref(1, "1111111111111111");
        rate("A000000000", "01", "0001", "15.00");

        batch.process(PARM_DATE, List.of(
                tcb(1, "01", "0001", "1000.00"),
                tcb(1, "01", "0001", "1000.00")));

        // two accruals of 12.50 -> 25.00 posted once to 100.00 => 125.00
        assertThat(accounts.findById(1)).get()
                .extracting(AccountRecord::getAcctCurrBal).isEqualTo(new BigDecimal("125.00"));
    }
}
