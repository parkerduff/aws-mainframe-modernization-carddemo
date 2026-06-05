package com.carddemo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * BR-31 &mdash; Validates that a card number is a non-zero 16-digit numeric value.
 *
 * <p>Migrated from COBOL paragraph {@code 1220-EDIT-CARD} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 762-799). Field definition:
 * {@code CC-CARD-NUM PIC X(16)} / {@code CC-CARD-NUM-N PIC 9(16)} in
 * {@code app/cpy/CVCRD01Y.cpy} (line 37).
 *
 * <p>COBOL equivalence:
 * <ul>
 *   <li>{@code CC-CARD-NUM EQUAL LOW-VALUES OR SPACES OR CC-CARD-NUM-N EQUAL ZEROS}
 *       &rarr; blank / all-zeros rejected.</li>
 *   <li>{@code CC-CARD-NUM IS NOT NUMERIC} &rarr; must be 16 digits.</li>
 * </ul>
 */
public class CardNumberValidator implements ConstraintValidator<CardNumber, String> {

    private static final Pattern SIXTEEN_DIGITS = Pattern.compile("\\d{16}");
    private static final String ALL_ZEROS = "0000000000000000";

    private boolean required;

    @Override
    public void initialize(CardNumber constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Not supplied (LOW-VALUES / SPACES). Allowed only when not required.
        if (value == null || value.isBlank()) {
            return !required;
        }
        // Must be a numeric value exactly 16 digits long (PIC 9(16)).
        if (!SIXTEEN_DIGITS.matcher(value).matches()) {
            return false;
        }
        // CC-CARD-NUM-N EQUAL ZEROS -> reject all zeros.
        return !ALL_ZEROS.equals(value);
    }
}
