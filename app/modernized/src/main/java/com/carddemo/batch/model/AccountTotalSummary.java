package com.carddemo.batch.model;

import java.math.BigDecimal;

/**
 * Account-level total emitted whenever the card number changes (and once more
 * at end of input).
 *
 * <p>Mirrors {@code REPORT-ACCOUNT-TOTALS} / {@code 1120-WRITE-ACCOUNT-TOTALS}
 * in {@code CBTRN03C.cbl}. The grouping key is the card number; {@code accountId}
 * is the value resolved via the card cross-reference lookup.
 */
public record AccountTotalSummary(String cardNum, String accountId, BigDecimal total) {
}
