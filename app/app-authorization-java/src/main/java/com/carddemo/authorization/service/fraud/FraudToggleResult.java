package com.carddemo.authorization.service.fraud;

import java.time.LocalDate;

/**
 * Result of a fraud mark/unmark toggle (BR-03).
 *
 * @param detailId       the affected authorization detail
 * @param cardNum        card number
 * @param fraudConfirmed new fraud state after the toggle
 * @param action         {@code "REPORT"} (marked) or {@code "REMOVE"} (unmarked)
 * @param fraudRptDate   the stamped fraud report date
 */
public record FraudToggleResult(
        Long detailId,
        String cardNum,
        boolean fraudConfirmed,
        String action,
        LocalDate fraudRptDate) {
}

