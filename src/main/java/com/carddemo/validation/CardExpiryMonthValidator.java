package com.carddemo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * BR-33 &mdash; Validates that a card expiry month is an integer between 1 and 12.
 *
 * <p>Migrated from COBOL paragraph {@code 1250-EDIT-EXPIRY-MON} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 877-907), checked against
 * {@code 88 VALID-MONTH VALUES 1 THRU 12} (line 95).
 *
 * <p>COBOL equivalence:
 * <ul>
 *   <li>{@code CCUP-NEW-EXPMON EQUAL LOW-VALUES OR SPACES OR ZEROS}
 *       &rarr; blank rejected.</li>
 *   <li>{@code VALID-MONTH} (numeric, 1 THRU 12) &rarr; range check.</li>
 * </ul>
 */
public class CardExpiryMonthValidator implements ConstraintValidator<CardExpiryMonth, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Not supplied (LOW-VALUES / SPACES / ZEROS) -> reject.
        if (value == null || value.isBlank()) {
            return false;
        }
        final int month;
        try {
            month = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Not numeric -> reject (COBOL VALID-MONTH fails for non-numeric).
            return false;
        }
        return month >= 1 && month <= 12;
    }
}
