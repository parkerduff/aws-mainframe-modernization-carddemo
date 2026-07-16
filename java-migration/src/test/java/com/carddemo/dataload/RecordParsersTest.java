package com.carddemo.dataload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.carddemo.domain.Account;
import com.carddemo.domain.CardXref;
import com.carddemo.domain.DailyTransaction;
import com.carddemo.domain.TranCatBalance;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies byte-for-byte field parsing against the real fixed-width sample files
 * in {@code app/data/ASCII/}. Each field is checked against values decoded from
 * the copybook offsets, and amount fields are asserted to carry scale-2 precision.
 */
class RecordParsersTest {

    private static final Path ASCII_DIR = Path.of("..", "app", "data", "ASCII");

    private static List<String> lines(String file) throws IOException {
        Path p = ASCII_DIR.resolve(file);
        assumeTrue(Files.exists(p), "sample data file not found: " + p);
        return Files.readAllLines(p, StandardCharsets.ISO_8859_1);
    }

    @Test
    void parsesFirstAccountRecord() throws IOException {
        Account a = RecordParsers.parseAccount(lines("acctdata.txt").get(0));
        assertThat(a.getAcctId()).isEqualTo(1L);
        assertThat(a.getAcctActiveStatus()).isEqualTo("Y");
        assertThat(a.getAcctCurrBal()).isEqualTo(new BigDecimal("194.00"));
        assertThat(a.getAcctCreditLimit()).isEqualTo(new BigDecimal("2020.00"));
        assertThat(a.getAcctCashCreditLimit()).isEqualTo(new BigDecimal("1020.00"));
        assertThat(a.getAcctOpenDate()).isEqualTo("2014-11-20");
        assertThat(a.getAcctExpirationDate()).isEqualTo("2025-05-20");
        assertThat(a.getAcctReissueDate()).isEqualTo("2025-05-20");
        assertThat(a.getAcctCurrCycCredit()).isEqualByComparingTo("0.00");
        assertThat(a.getAcctCurrCycDebit()).isEqualByComparingTo("0.00");
        // every amount keeps scale 2
        assertThat(a.getAcctCurrBal().scale()).isEqualTo(2);
        assertThat(a.getAcctCreditLimit().scale()).isEqualTo(2);
        assertThat(a.getAcctCashCreditLimit().scale()).isEqualTo(2);
    }

    @Test
    void parsesFirstCardXrefRecord() throws IOException {
        CardXref x = RecordParsers.parseCardXref(lines("cardxref.txt").get(0));
        assertThat(x.getXrefCardNum()).isEqualTo("0500024453765740");
        assertThat(x.getXrefCustId()).isEqualTo(50L);
        assertThat(x.getXrefAcctId()).isEqualTo(50L);
    }

    @Test
    void parsesFirstTranCatBalanceRecord() throws IOException {
        TranCatBalance b = RecordParsers.parseTranCatBalance(lines("tcatbal.txt").get(0));
        assertThat(b.getId().getTrancatAcctId()).isEqualTo(1L);
        assertThat(b.getId().getTrancatTypeCd()).isEqualTo("01");
        assertThat(b.getId().getTrancatCd()).isEqualTo(1);
        assertThat(b.getTranCatBal()).isEqualByComparingTo("0.00");
        assertThat(b.getTranCatBal().scale()).isEqualTo(2);
    }

    @Test
    void parsesDailyTransactionRecordsIncludingNegativeAmount() throws IOException {
        List<String> lines = lines("dailytran.txt");

        DailyTransaction d1 = RecordParsers.parseDailyTransaction(lines.get(0));
        assertThat(d1.getDalytranId()).isEqualTo("0000000000683580");
        assertThat(d1.getDalytranTypeCd()).isEqualTo("01");
        assertThat(d1.getDalytranCatCd()).isEqualTo(1);
        assertThat(d1.getDalytranSource()).isEqualTo("POS TERM");
        assertThat(d1.getDalytranAmt()).isEqualTo(new BigDecimal("504.77"));
        assertThat(d1.getDalytranMerchantId()).isEqualTo(800000000L);
        assertThat(d1.getDalytranOrigTs()).isEqualTo("2022-06-10 19:27:53.000000");
        // TRAN-PROC-TS is blank in the daily file -> trimmed to empty
        assertThat(d1.getDalytranProcTs()).isEmpty();

        DailyTransaction d2 = RecordParsers.parseDailyTransaction(lines.get(1));
        assertThat(d2.getDalytranId()).isEqualTo("0000000001774260");
        assertThat(d2.getDalytranAmt()).isEqualTo(new BigDecimal("-919.00"));
        assertThat(d2.getDalytranAmt().scale()).isEqualTo(2);
    }
}
