package com.carddemo.batch.model;

import java.util.List;

/**
 * The structured result of running the transaction report.
 *
 * <p>This replaces the flat 133-column print file ({@code REPORT-FILE}) produced
 * by {@code CBTRN03C.cbl} with an in-memory data structure. Detail lines are in
 * input (file) order. Page and account totals are exposed as separate ordered
 * lists, and the grand total is the sum of all page totals.
 */
public record TransactionReport(
        String startDate,
        String endDate,
        List<TransactionReportLine> lines,
        List<PageTotalSummary> pageTotals,
        List<AccountTotalSummary> accountTotals,
        GrandTotalSummary grandTotal
) {
}
