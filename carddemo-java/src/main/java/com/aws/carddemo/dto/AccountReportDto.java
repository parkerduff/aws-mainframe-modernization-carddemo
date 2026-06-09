package com.aws.carddemo.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Account report, summarizing accounts (optionally filtered by status). Provides totals
 * across the included accounts.
 */
public record AccountReportDto(
        int accountCount,
        BigDecimal totalCurrentBalance,
        BigDecimal totalCreditLimit,
        List<AccountDto> accounts) {
}
