package com.carddemo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * BR-34 &mdash; Validates that a card expiry year is an integer between 1950 and 2099.
 *
 * <p>Migrated from COBOL paragraph {@code 1260-EDIT-EXPIRY-YEAR} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 913-944), checked against
 * {@code 88 VALID-YEAR VALUES 1950 THRU 2099} (line 99).
 *
 * <p>COBOL equivalence:
 * <ul>
 *   <li>{@code CCUP-NEW-EXPYEAR EQUAL LOW-VALUES OR SPACES OR ZEROS}
 *       &rarr; blank rejected.</li>
 *   <li>{@code VALID-YEAR} (numeric, 1950 THRU 2099) &rarr; range check.</li>
 * </ul>
 */
public class CardExpiryYearValidator implements ConstraintValidator<CardExpiryYear, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Not supplied (LOW-VALUES / SPACES / ZEROS) -> reject.
        if (value == null || value.isBlank()) {
            return false;
        }
        final int year;
        try {
            year = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // Not numeric -> reject (COBOL VALID-YEAR fails for non-numeric).
            return false;
        }
        return year >= 1950 && year <= 2099;
    }
}
