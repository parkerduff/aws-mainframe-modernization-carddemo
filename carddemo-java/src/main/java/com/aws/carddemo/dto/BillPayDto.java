package com.aws.carddemo.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Bill-payment request, mirroring COBIL00C (online bill pay). When {@code payFullBalance}
 * is true the entire current balance is paid and {@code amount} is ignored.
 */
public record BillPayDto(
        @NotNull Long acctId,
        boolean payFullBalance,
        BigDecimal amount) {
}
