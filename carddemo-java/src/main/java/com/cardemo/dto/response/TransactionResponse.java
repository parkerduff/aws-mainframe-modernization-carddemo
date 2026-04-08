package com.cardemo.dto.response;

import com.cardemo.entity.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Transaction details response - migrated from COTRN01 transaction view screen")
public record TransactionResponse(
    String tranId,
    String typeCd,
    Integer catCd,
    String source,
    String description,
    BigDecimal amount,
    Long merchantId,
    String merchantName,
    String merchantCity,
    String merchantZip,
    String cardNum,
    LocalDateTime origTs,
    LocalDateTime procTs
) {
    public static TransactionResponse from(Transaction txn) {
        return new TransactionResponse(
            txn.getTranId(),
            txn.getTypeCd(),
            txn.getCatCd(),
            txn.getSource(),
            txn.getDescription(),
            txn.getAmount(),
            txn.getMerchantId(),
            txn.getMerchantName(),
            txn.getMerchantCity(),
            txn.getMerchantZip(),
            txn.getCardNum(),
            txn.getOrigTs(),
            txn.getProcTs()
        );
    }
}
