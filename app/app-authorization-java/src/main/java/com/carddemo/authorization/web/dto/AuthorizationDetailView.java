package com.carddemo.authorization.web.dto;

import com.carddemo.authorization.domain.AuthResponseReason;
import com.carddemo.authorization.domain.PendingAuthDetail;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST view of an authorization detail (BR-08.2, BR-08.3) — replaces the
 * {@code COPAUS1C} BMS detail screen.
 */
public record AuthorizationDetailView(
        Long id,
        Long acctId,
        String cardNum,
        LocalDateTime authTs,
        String authOrigDate,
        String authOrigTime,
        String authType,
        String authRespCode,
        boolean approved,
        String authRespReason,
        String authRespReasonText,
        BigDecimal transactionAmt,
        BigDecimal approvedAmt,
        Integer posEntryMode,
        String merchantId,
        String merchantName,
        String merchantCity,
        String merchantState,
        String merchantZip,
        String transactionId,
        String matchStatus,
        boolean fraudConfirmed,
        String fraudRptDate) {

    public static AuthorizationDetailView from(PendingAuthDetail d) {
        return new AuthorizationDetailView(
                d.getId(),
                d.getSummary() == null ? null : d.getSummary().getAcctId(),
                d.getCardNum(),
                d.getAuthTs(),
                d.getAuthOrigDate(),
                d.getAuthOrigTime(),
                d.getAuthType(),
                d.getAuthRespCode(),
                d.isApproved(),
                d.getAuthRespReason(),
                AuthResponseReason.fromCode(d.getAuthRespReason()).description(),
                d.getTransactionAmt(),
                d.getApprovedAmt(),
                d.getPosEntryMode(),
                d.getMerchantId(),
                d.getMerchantName(),
                d.getMerchantCity(),
                d.getMerchantState(),
                d.getMerchantZip(),
                d.getTransactionId(),
                d.getMatchStatus(),
                d.isFraudConfirmed(),
                d.getFraudRptDate());
    }
}

