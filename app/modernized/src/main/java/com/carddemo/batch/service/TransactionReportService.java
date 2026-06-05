package com.carddemo.batch.service;

import com.carddemo.batch.model.AccountTotalSummary;
import com.carddemo.batch.model.GrandTotalSummary;
import com.carddemo.batch.model.PageTotalSummary;
import com.carddemo.batch.model.TransactionRecord;
import com.carddemo.batch.model.TransactionReport;
import com.carddemo.batch.model.TransactionReportLine;
import com.carddemo.batch.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modern Java port of {@code CBTRN03C.cbl} (R19 — Transaction Report Date
 * Filtering).
 *
 * <p>The original batch program reads a sequential transaction file, keeps only
 * transactions whose processing date falls inside an inclusive date range,
 * groups them by card number, enriches each one via three indexed lookups (card
 * cross-reference, transaction type, transaction category) and prints a paged
 * report with page totals, per-account totals and a grand total.
 *
 * <p>This service reproduces that business logic but returns a structured
 * {@link TransactionReport} instead of writing a 133-column flat file.
 *
 * <p>Notable faithful behaviours:
 * <ul>
 *   <li><b>Date filter</b> — inclusive string comparison on the first 10
 *       characters of the processing timestamp (mirrors {@code CBTRN03C} lines
 *       173-178). Because the timestamps use {@code YYYY-MM-DD} ordering, a
 *       lexicographic comparison is equivalent to a chronological one.</li>
 *   <li><b>Card grouping</b> — an account total is emitted whenever the card
 *       number changes and once more after the last transaction (mirrors lines
 *       181-187).</li>
 *   <li><b>Paging</b> — a page total is accumulated and flushed every
 *       {@code pageSize} detail lines (default 20, configurable), and the grand
 *       total is the running sum of those page totals.</li>
 * </ul>
 */
public class TransactionReportService {

    /** Default number of detail lines per page ({@code WS-PAGE-SIZE} VALUE 20). */
    public static final int DEFAULT_PAGE_SIZE = 20;

    private final TransactionRepository repository;
    private final int pageSize;

    public TransactionReportService(TransactionRepository repository) {
        this(repository, DEFAULT_PAGE_SIZE);
    }

    public TransactionReportService(TransactionRepository repository, int pageSize) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, was " + pageSize);
        }
        this.pageSize = pageSize;
    }

    /**
     * Builds the transaction report for the inclusive date range
     * {@code [startDate, endDate]} (both formatted {@code YYYY-MM-DD}).
     */
    public TransactionReport generateReport(String startDate, String endDate) {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");

        List<TransactionReportLine> lines = new ArrayList<>();
        List<PageTotalSummary> pageTotals = new ArrayList<>();
        List<AccountTotalSummary> accountTotals = new ArrayList<>();

        BigDecimal pageTotal = BigDecimal.ZERO;
        BigDecimal accountTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        String currentCard = null;
        String currentAccountId = null;
        int pageLineCount = 0;
        int pageNumber = 1;

        for (TransactionRecord tran : repository.findAllTransactions()) {
            if (!isInRange(tran, startDate, endDate)) {
                continue;
            }

            // Card break: flush the previous account's total, then resolve the
            // new card's account id (mirrors 1120-WRITE-ACCOUNT-TOTALS +
            // 1500-A-LOOKUP-XREF).
            if (currentCard == null || !currentCard.equals(tran.cardNum())) {
                if (currentCard != null) {
                    accountTotals.add(new AccountTotalSummary(currentCard, currentAccountId, accountTotal));
                    accountTotal = BigDecimal.ZERO;
                }
                currentCard = tran.cardNum();
                currentAccountId = repository.findAccountIdByCardNum(tran.cardNum()).orElse("");
            }

            String typeDesc = repository.findTypeDescription(tran.typeCd()).orElse("");
            String catDesc = repository.findCategoryDescription(tran.typeCd(), tran.catCd()).orElse("");

            lines.add(new TransactionReportLine(
                    tran.id(),
                    currentAccountId,
                    tran.typeCd(),
                    typeDesc,
                    tran.catCd(),
                    catDesc,
                    tran.source(),
                    tran.amount()));

            BigDecimal amount = tran.amount() == null ? BigDecimal.ZERO : tran.amount();
            pageTotal = pageTotal.add(amount);
            accountTotal = accountTotal.add(amount);
            pageLineCount++;

            // Page break: flush the page total and roll it into the grand total.
            if (pageLineCount == pageSize) {
                pageTotals.add(new PageTotalSummary(pageNumber, pageTotal));
                grandTotal = grandTotal.add(pageTotal);
                pageTotal = BigDecimal.ZERO;
                pageLineCount = 0;
                pageNumber++;
            }
        }

        // Flush the final (partial) page, if any.
        if (pageLineCount > 0) {
            pageTotals.add(new PageTotalSummary(pageNumber, pageTotal));
            grandTotal = grandTotal.add(pageTotal);
        }

        // Flush the final account total (mirrors the last card group).
        if (currentCard != null) {
            accountTotals.add(new AccountTotalSummary(currentCard, currentAccountId, accountTotal));
        }

        return new TransactionReport(
                startDate,
                endDate,
                List.copyOf(lines),
                List.copyOf(pageTotals),
                List.copyOf(accountTotals),
                new GrandTotalSummary(grandTotal));
    }

    /**
     * Date filtering rule from {@code CBTRN03C} lines 173-178:
     * {@code TRAN-PROC-TS(1:10) >= startDate AND <= endDate}.
     */
    private boolean isInRange(TransactionRecord tran, String startDate, String endDate) {
        String procDate = tran.procDate();
        if (procDate == null) {
            return false;
        }
        return procDate.compareTo(startDate) >= 0 && procDate.compareTo(endDate) <= 0;
    }
}
