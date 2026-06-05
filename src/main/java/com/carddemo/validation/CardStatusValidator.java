package com.carddemo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * BR-32 &mdash; Validates that a card active status is {@code Y} or {@code N}.
 *
 * <p>Migrated from COBOL paragraph {@code 1240-EDIT-CARDSTATUS} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 845-873), checked against
 * {@code 88 FLG-YES-NO-VALID VALUES 'Y', 'N'} (line 91).
 *
 * <p>COBOL equivalence:
 * <ul>
 *   <li>{@code CCUP-NEW-CRDSTCD EQUAL LOW-VALUES OR SPACES OR ZEROS}
 *       &rarr; blank rejected.</li>
 *   <li>{@code FLG-YES-NO-VALID} &rarr; only {@code Y} or {@code N} accepted.</li>
 * </ul>
 *
 * <p>The check is case-insensitive so callers may supply lower-case values.
 */
public class CardStatusValidator implements ConstraintValidator<CardStatus, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Not supplied (LOW-VALUES / SPACES / ZEROS) -> reject.
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("N");
    }
}
