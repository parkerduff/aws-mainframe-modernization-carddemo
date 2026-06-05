package com.carddemo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * BR-30 &mdash; Validates that an account ID is a non-zero 11-digit numeric value.
 *
 * <p>Migrated from COBOL paragraph {@code 1210-EDIT-ACCOUNT} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 721-755). Field definition:
 * {@code CC-ACCT-ID PIC X(11)} / {@code CC-ACCT-ID-N PIC 9(11)} in
 * {@code app/cpy/CVCRD01Y.cpy} (line 34).
 *
 * <p>COBOL equivalence:
 * <ul>
 *   <li>{@code CC-ACCT-ID EQUAL LOW-VALUES OR SPACES OR CC-ACCT-ID-N EQUAL ZEROS}
 *       &rarr; blank / all-zeros rejected.</li>
 *   <li>{@code CC-ACCT-ID IS NOT NUMERIC} &rarr; must be 11 digits.</li>
 * </ul>
 */
public class AccountIdValidator implements ConstraintValidator<AccountId, String> {

    private static final Pattern ELEVEN_DIGITS = Pattern.compile("\\d{11}");
    private static final String ALL_ZEROS = "00000000000";

    private boolean required;

    @Override
    public void initialize(AccountId constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Not supplied (LOW-VALUES / SPACES). Allowed only when not required.
        if (value == null || value.isBlank()) {
            return !required;
        }
        // Must be a numeric value exactly 11 digits long (PIC 9(11)).
        if (!ELEVEN_DIGITS.matcher(value).matches()) {
            return false;
        }
        // CC-ACCT-ID-N EQUAL ZEROS -> reject all zeros.
        return !ALL_ZEROS.equals(value);
    }
}
