package com.carddemo.batch;

import com.carddemo.batch.model.AccountTotalSummary;
import com.carddemo.batch.model.PageTotalSummary;
import com.carddemo.batch.model.TransactionRecord;
import com.carddemo.batch.model.TransactionReport;
import com.carddemo.batch.model.TransactionReportLine;
import com.carddemo.batch.repository.TransactionRepository;
import com.carddemo.batch.service.TransactionReportService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionReportServiceTest {

    /** Simple in-memory stand-in for the four VSAM files. */
    private static final class InMemoryTransactionRepository implements TransactionRepository {
        final List<TransactionRecord> transactions = new ArrayList<>();
        final Map<String, String> cardToAccount = new HashMap<>();
        final Map<String, String> typeDesc = new HashMap<>();
        final Map<String, String> catDesc = new HashMap<>();

        @Override
        public List<TransactionRecord> findAllTransactions() {
            return transactions;
        }

        @Override
        public Optional<String> findAccountIdByCardNum(String cardNum) {
            return Optional.ofNullable(cardToAccount.get(cardNum));
        }

        @Override
        public Optional<String> findTypeDescription(String typeCd) {
            return Optional.ofNullable(typeDesc.get(typeCd));
        }

        @Override
        public Optional<String> findCategoryDescription(String typeCd, int catCd) {
            return Optional.ofNullable(catDesc.get(typeCd + "|" + catCd));
        }
    }

    private static TransactionRecord tran(String id, String cardNum, String procDate, String amount) {
        return new TransactionRecord(
                id,
                "01",
                1000,
                "POS",
                "desc",
                new BigDecimal(amount),
                123456789L,
                "Merchant",
                "City",
                "00000",
                cardNum,
                procDate + "-00.00.00.000000",
                procDate + "-00.00.00.000000");
    }

    private static InMemoryTransactionRepository repoWith(TransactionRecord... trans) {
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        for (TransactionRecord t : trans) {
            repo.transactions.add(t);
            repo.cardToAccount.putIfAbsent(t.cardNum(), "ACCT-" + t.cardNum());
        }
        repo.typeDesc.put("01", "Purchase");
        repo.catDesc.put("01|1000", "Groceries");
        return repo;
    }

    @Test
    void includesOnlyTransactionsInsideInclusiveDateRange() {
        InMemoryTransactionRepository repo = repoWith(
                tran("T-BEFORE", "CARD1", "2023-01-09", "10.00"),
                tran("T-START", "CARD1", "2023-01-10", "20.00"),
                tran("T-MID", "CARD1", "2023-01-15", "30.00"),
                tran("T-END", "CARD1", "2023-01-20", "40.00"),
                tran("T-AFTER", "CARD1", "2023-01-21", "50.00"));

        TransactionReport report = new TransactionReportService(repo)
                .generateReport("2023-01-10", "2023-01-20");

        List<String> includedIds = report.lines().stream().map(TransactionReportLine::transactionId).toList();
        assertEquals(List.of("T-START", "T-MID", "T-END"), includedIds);
        // Boundary dates are inclusive; out-of-range rows excluded.
        assertTrue(includedIds.contains("T-START"));
        assertTrue(includedIds.contains("T-END"));
    }

    @Test
    void enrichesLinesWithXrefAccountAndDescriptions() {
        InMemoryTransactionRepository repo = repoWith(
                tran("T1", "CARD1", "2023-01-15", "12.34"));

        TransactionReport report = new TransactionReportService(repo)
                .generateReport("2023-01-01", "2023-12-31");

        assertEquals(1, report.lines().size());
        TransactionReportLine line = report.lines().get(0);
        assertEquals("ACCT-CARD1", line.accountId());
        assertEquals("Purchase", line.typeDesc());
        assertEquals("Groceries", line.catDesc());
        assertEquals(0, new BigDecimal("12.34").compareTo(line.amount()));
    }

    @Test
    void groupsByCardEmittingAccountTotalsPerCard() {
        InMemoryTransactionRepository repo = repoWith(
                tran("T1", "CARD1", "2023-01-15", "10.00"),
                tran("T2", "CARD1", "2023-01-16", "15.00"),
                tran("T3", "CARD2", "2023-01-17", "30.00"),
                tran("T4", "CARD2", "2023-01-18", "5.00"));

        TransactionReport report = new TransactionReportService(repo)
                .generateReport("2023-01-01", "2023-12-31");

        List<AccountTotalSummary> totals = report.accountTotals();
        assertEquals(2, totals.size());
        assertEquals("CARD1", totals.get(0).cardNum());
        assertEquals(0, new BigDecimal("25.00").compareTo(totals.get(0).total()));
        assertEquals("CARD2", totals.get(1).cardNum());
        assertEquals(0, new BigDecimal("35.00").compareTo(totals.get(1).total()));
    }

    @Test
    void emitsSeparateAccountTotalsForNonContiguousCardGroups() {
        // Card order CARD1, CARD2, CARD1 -> three account groups (mirrors the
        // control-break being driven by "card changed", not a global grouping).
        InMemoryTransactionRepository repo = repoWith(
                tran("T1", "CARD1", "2023-01-15", "10.00"),
                tran("T2", "CARD2", "2023-01-16", "20.00"),
                tran("T3", "CARD1", "2023-01-17", "30.00"));

        TransactionReport report = new TransactionReportService(repo)
                .generateReport("2023-01-01", "2023-12-31");

        assertEquals(3, report.accountTotals().size());
        assertEquals("CARD1", report.accountTotals().get(0).cardNum());
        assertEquals("CARD2", report.accountTotals().get(1).cardNum());
        assertEquals("CARD1", report.accountTotals().get(2).cardNum());
    }

    @Test
    void breaksIntoPagesEveryPageSizeLinesIncludingPartialFinalPage() {
        List<TransactionRecord> trans = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            trans.add(tran("T" + i, "CARD1", "2023-01-15", "10.00"));
        }
        InMemoryTransactionRepository repo = repoWith(trans.toArray(new TransactionRecord[0]));

        // pageSize 2 -> pages of [2,2,1].
        TransactionReport report = new TransactionReportService(repo, 2)
                .generateReport("2023-01-01", "2023-12-31");

        List<PageTotalSummary> pages = report.pageTotals();
        assertEquals(3, pages.size());
        assertEquals(1, pages.get(0).pageNumber());
        assertEquals(0, new BigDecimal("20.00").compareTo(pages.get(0).total()));
        assertEquals(0, new BigDecimal("20.00").compareTo(pages.get(1).total()));
        assertEquals(0, new BigDecimal("10.00").compareTo(pages.get(2).total()));
    }

    @Test
    void grandTotalEqualsSumOfAllIncludedAmounts() {
        InMemoryTransactionRepository repo = repoWith(
                tran("T1", "CARD1", "2023-01-15", "10.00"),
                tran("T2", "CARD1", "2023-01-16", "15.50"),
                tran("T3", "CARD2", "2023-01-17", "4.50"),
                tran("OUT", "CARD2", "2030-01-17", "999.00")); // filtered out

        TransactionReport report = new TransactionReportService(repo, 2)
                .generateReport("2023-01-01", "2023-12-31");

        assertEquals(0, new BigDecimal("30.00").compareTo(report.grandTotal().total()));
        // Grand total equals the sum of the page totals.
        BigDecimal pageSum = report.pageTotals().stream()
                .map(PageTotalSummary::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, report.grandTotal().total().compareTo(pageSum));
    }

    @Test
    void emptyInputProducesEmptyReport() {
        InMemoryTransactionRepository repo = repoWith();

        TransactionReport report = new TransactionReportService(repo)
                .generateReport("2023-01-01", "2023-12-31");

        assertTrue(report.lines().isEmpty());
        assertTrue(report.pageTotals().isEmpty());
        assertTrue(report.accountTotals().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.grandTotal().total()));
    }
}
