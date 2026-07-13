package com.carddemo.authorization.service.xref;

import java.math.BigDecimal;

/**
 * Account master data used for decisioning (BR-01.3).
 *
 * <p>Ports the fields of the VSAM account record (copybook {@code CVACT01Y}) that the
 * authorization decision needs: active status, current balance, and credit limits.
 */
public record Account(
        Long acctId,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit) {
}

