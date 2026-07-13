package com.carddemo.authorization.messaging;

import java.math.BigDecimal;

/**
 * Inbound authorization request (BR-06.1).
 *
 * <p>Field-for-field port of copybook {@code CCPAURQY} (the comma-separated request
 * message). Amounts use {@link BigDecimal} to preserve COBOL fixed-point semantics.
 */
public record AuthorizationRequest(
        String authDate,
        String authTime,
        String cardNum,
        String authType,
        String cardExpiryDate,
        String messageType,
        String messageSource,
        String processingCode,
        BigDecimal transactionAmt,
        String merchantCategoryCode,
        String acqrCountryCode,
        Integer posEntryMode,
        String merchantId,
        String merchantName,
        String merchantCity,
        String merchantState,
        String merchantZip,
        String transactionId) {
}

