package com.aws.carddemo.batch;

import java.math.BigDecimal;

/**
 * Daily transaction input record, mapped from the fixed-width DALYTRAN file
 * ({@code app/cpy/CVTRA06Y.cpy}, identical layout to CVTRA05Y) read by CBTRN02C.
 */
public record DailyTransaction(
        String tranId,
        String typeCd,
        Integer catCd,
        String source,
        String desc,
        BigDecimal amount,
        Long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNum,
        String origTs,
        String procTs) {
}
