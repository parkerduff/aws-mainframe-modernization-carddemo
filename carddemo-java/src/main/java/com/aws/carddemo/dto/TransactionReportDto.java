package com.aws.carddemo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transaction report, replacing CORPT00C (transaction report). Summarizes the transactions
 * for a card within an optional timestamp range.
 */
public record TransactionReportDto(
        String cardNum,
        String fromTs,
        String toTs,
        int transactionCount,
        BigDecimal totalAmount,
        List<TransactionDto> transactions) {
}
