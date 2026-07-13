package com.carddemo.authorization.messaging;

import java.math.BigDecimal;

/**
 * Outbound authorization reply (BR-06.2).
 *
 * <p>Field-for-field port of copybook {@code CCPAURLY} (the comma-separated response
 * message emitted by {@code COPAUA0C} {@code 6000-MAKE-DECISION}).
 */
public record AuthorizationReply(
        String cardNum,
        String transactionId,
        String authIdCode,
        String authRespCode,
        String authRespReason,
        BigDecimal approvedAmt) {
}

