package com.aws.carddemo.batch;

/**
 * Outcome of validating a {@link DailyTransaction} in the post-transaction batch
 * (CBTRN02C 1500-VALIDATE-TRAN). Either valid (with the resolved account id) or rejected
 * (with a numeric reason code and description, mirroring WS-VALIDATION-FAIL-REASON).
 */
public record ProcessedTransaction(
        DailyTransaction transaction,
        boolean valid,
        Long acctId,
        int rejectReasonCode,
        String rejectReasonDesc) {

    public static ProcessedTransaction valid(DailyTransaction tran, long acctId) {
        return new ProcessedTransaction(tran, true, acctId, 0, null);
    }

    public static ProcessedTransaction rejected(DailyTransaction tran, int code, String desc) {
        return new ProcessedTransaction(tran, false, null, code, desc);
    }
}
