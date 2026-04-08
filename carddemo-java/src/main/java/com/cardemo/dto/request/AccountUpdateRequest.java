package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Schema(description = "Account update request - migrated from COACTUPC account update transaction")
public record AccountUpdateRequest(
    @Schema(description = "Active status (Y/N)") String activeStatus,
    @DecimalMin("0.00") @Schema(description = "Credit limit") BigDecimal creditLimit,
    @DecimalMin("0.00") @Schema(description = "Cash credit limit") BigDecimal cashCreditLimit,
    @Schema(description = "Account group ID") String groupId
) {}
