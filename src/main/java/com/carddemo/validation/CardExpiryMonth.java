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
 * BR-33 &mdash; Card expiry month must be between 1 and 12.
 *
 * <p>Migrated from COBOL paragraph {@code 1250-EDIT-EXPIRY-MON} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 877-907). The value is checked against
 * the 88-level condition {@code 88 VALID-MONTH VALUES 1 THRU 12} (line 95).
 * The same rule is shared across {@code COACTVWC.cbl}, {@code COCRDLIC.cbl},
 * and {@code COCRDSLC.cbl}.
 *
 * <p>The COBOL logic rejects the value when it is LOW-VALUES/SPACES/ZEROS
 * (blank), non-numeric, or outside the 1-12 range.
 */
@Documented
@Constraint(validatedBy = CardExpiryMonthValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface CardExpiryMonth {

    String message() default "Card expiry month must be between 1 and 12";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
