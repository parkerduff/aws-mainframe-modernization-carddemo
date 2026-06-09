package com.aws.carddemo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account update request, mirroring the updatable fields in COACTUPC (account update).
 * Null fields are left unchanged.
 */
public record AccountUpdateDto(
        String activeStatus,
        BigDecimal currBal,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        LocalDate expirationDate,
        LocalDate reissueDate,
        BigDecimal currCycCredit,
        BigDecimal currCycDebit,
        String addrZip,
        String groupId) {
}
