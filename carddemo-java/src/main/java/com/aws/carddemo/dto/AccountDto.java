package com.aws.carddemo.dto;

import com.aws.carddemo.entity.AccountEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Account view response, mirroring the data shown by COACTVWC (account view).
 */
public record AccountDto(
        Long acctId,
        String activeStatus,
        BigDecimal currBal,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        LocalDate openDate,
        LocalDate expirationDate,
        LocalDate reissueDate,
        BigDecimal currCycCredit,
        BigDecimal currCycDebit,
        String addrZip,
        String groupId) {

    public static AccountDto fromEntity(AccountEntity e) {
        return new AccountDto(
                e.getAcctId(),
                e.getActiveStatus(),
                e.getCurrBal(),
                e.getCreditLimit(),
                e.getCashCreditLimit(),
                e.getOpenDate(),
                e.getExpirationDate(),
                e.getReissueDate(),
                e.getCurrCycCredit(),
                e.getCurrCycDebit(),
                e.getAddrZip(),
                e.getGroupId());
    }
}
