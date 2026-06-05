package com.carddemo.batch.model;

import java.math.BigDecimal;

/**
 * Page-level total emitted every {@code pageSize} detail lines.
 *
 * <p>Mirrors {@code REPORT-PAGE-TOTALS} / {@code 1110-WRITE-PAGE-TOTALS} in
 * {@code CBTRN03C.cbl}. {@code pageNumber} is 1-based.
 */
public record PageTotalSummary(int pageNumber, BigDecimal total) {
}
