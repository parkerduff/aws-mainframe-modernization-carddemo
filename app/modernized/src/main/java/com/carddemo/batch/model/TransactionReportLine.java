package com.carddemo.batch.model;

import java.math.BigDecimal;

/**
 * A single detail line in the transaction report.
 *
 * <p>Mirrors the COBOL {@code TRANSACTION-DETAIL-REPORT} layout from copybook
 * {@code CVTRA07Y.cpy}, enriched with the cross-referenced account id and the
 * transaction type / category descriptions looked up while building the report
 * (see {@code 1120-WRITE-DETAIL} in {@code CBTRN03C.cbl}).
 */
public record TransactionReportLine(
        String transactionId,
        String accountId,
        String typeCd,
        String typeDesc,
        int catCd,
        String catDesc,
        String source,
        BigDecimal amount
) {
}
