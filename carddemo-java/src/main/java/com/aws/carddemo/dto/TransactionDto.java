package com.aws.carddemo.dto;

import com.aws.carddemo.entity.TransactionEntity;
import java.math.BigDecimal;

/**
 * Transaction response, mirroring COTRN01C (transaction detail) and the rows shown
 * by COTRN00C (transaction list).
 */
public record TransactionDto(
        String tranId,
        String tranTypeCd,
        Integer tranCatCd,
        String tranSource,
        String tranDesc,
        BigDecimal tranAmt,
        Long tranMerchantId,
        String tranMerchantName,
        String tranMerchantCity,
        String tranMerchantZip,
        String tranCardNum,
        String tranOrigTs,
        String tranProcTs) {

    public static TransactionDto fromEntity(TransactionEntity e) {
        return new TransactionDto(
                e.getTranId(),
                e.getTranTypeCd(),
                e.getTranCatCd(),
                e.getTranSource(),
                e.getTranDesc(),
                e.getTranAmt(),
                e.getTranMerchantId(),
                e.getTranMerchantName(),
                e.getTranMerchantCity(),
                e.getTranMerchantZip(),
                e.getTranCardNum(),
                e.getTranOrigTs(),
                e.getTranProcTs());
    }
}
