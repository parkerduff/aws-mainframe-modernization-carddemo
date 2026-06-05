package com.carddemo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BR-34 &mdash; Card expiry year must be between 1950 and 2099.
 *
 * <p>Migrated from COBOL paragraph {@code 1260-EDIT-EXPIRY-YEAR} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 913-944). The value is checked against
 * the 88-level condition {@code 88 VALID-YEAR VALUES 1950 THRU 2099} (line 99).
 * The same rule is shared across {@code COACTVWC.cbl}, {@code COCRDLIC.cbl},
 * and {@code COCRDSLC.cbl}.
 *
 * <p>The COBOL logic rejects the value when it is LOW-VALUES/SPACES/ZEROS
 * (blank), non-numeric, or outside the 1950-2099 range.
 */
@Documented
@Constraint(validatedBy = CardExpiryYearValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface CardExpiryYear {

    String message() default "Card expiry year must be between 1950 and 2099";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
