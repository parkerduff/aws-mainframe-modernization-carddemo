package com.carddemo.authorization.web.dto;

import com.carddemo.authorization.domain.PendingAuthSummary;
import java.math.BigDecimal;

/**
 * REST view of an account's pending-authorization summary (BR-08.1) — replaces the
 * header of the {@code COPAUS0C} BMS summary screen.
 */
public record AuthorizationSummaryView(
        Long acctId,
        Long custId,
        String authStatus,
        BigDecimal creditLimit,
        BigDecimal creditBalance,
        BigDecimal availableAmount,
        BigDecimal cashLimit,
        BigDecimal cashBalance,
        int approvedAuthCount,
        int declinedAuthCount,
        BigDecimal approvedAuthAmount,
        BigDecimal declinedAuthAmount) {

    public static AuthorizationSummaryView from(PendingAuthSummary s) {
        return new AuthorizationSummaryView(
                s.getAcctId(),
                s.getCustId(),
                s.getAuthStatus(),
                s.getCreditLimit(),
                s.getCreditBalance(),
                s.availableAmount(),
                s.getCashLimit(),
                s.getCashBalance(),
                s.getApprovedAuthCount(),
                s.getDeclinedAuthCount(),
                s.getApprovedAuthAmount(),
                s.getDeclinedAuthAmount());
    }
}

