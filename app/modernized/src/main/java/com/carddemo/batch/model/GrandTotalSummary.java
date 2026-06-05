package com.carddemo.batch.model;

import java.math.BigDecimal;

/**
 * Grand total across every page of the report.
 *
 * <p>Mirrors {@code REPORT-GRAND-TOTALS} / {@code 1110-WRITE-GRAND-TOTALS} in
 * {@code CBTRN03C.cbl}, where the grand total is the running sum of all page
 * totals.
 */
public record GrandTotalSummary(BigDecimal total) {
}
