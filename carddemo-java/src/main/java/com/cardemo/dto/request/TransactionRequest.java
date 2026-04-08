package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "New transaction request - migrated from COTRN02 add transaction screen")
public record TransactionRequest(
    @NotBlank @Schema(description = "Transaction type code", example = "SA") String typeCd,
    @Schema(description = "Transaction category code", example = "5001") Integer catCd,
    @Schema(description = "Transaction source", example = "ONLINE") String source,
    @Schema(description = "Description", example = "PURCHASE") String description,
    @NotNull @DecimalMin("0.01") @Schema(description = "Transaction amount") BigDecimal amount,
    @Schema(description = "Merchant ID") Long merchantId,
    @Schema(description = "Merchant name") String merchantName,
    @Schema(description = "Merchant city") String merchantCity,
    @Schema(description = "Merchant zip code") String merchantZip,
    @NotBlank @Schema(description = "Card number", example = "4111111111111111") String cardNum
) {}
