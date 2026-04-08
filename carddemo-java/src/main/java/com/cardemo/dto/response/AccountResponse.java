package com.cardemo.dto.response;

import com.cardemo.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Account details response - migrated from COACTVW account view screen")
public record AccountResponse(
    Long acctId,
    String activeStatus,
    BigDecimal currBal,
    BigDecimal creditLimit,
    BigDecimal cashCreditLimit,
    LocalDate openDate,
    LocalDate expirationDate,
    BigDecimal currCycCredit,
    BigDecimal currCycDebit,
    String groupId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getAcctId(),
            account.getActiveStatus(),
            account.getCurrBal(),
            account.getCreditLimit(),
            account.getCashCreditLimit(),
            account.getOpenDate(),
            account.getExpirationDate(),
            account.getCurrCycCredit(),
            account.getCurrCycDebit(),
            account.getGroupId()
        );
    }
}
