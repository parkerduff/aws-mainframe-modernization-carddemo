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
 * BR-31 &mdash; Card number must be a non-zero 16-digit numeric value.
 *
 * <p>Migrated from COBOL paragraph {@code 1220-EDIT-CARD} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 762-799). The underlying field is
 * {@code CC-CARD-NUM PIC X(16)} defined in {@code app/cpy/CVCRD01Y.cpy}
 * (line 37), redefined as {@code CC-CARD-NUM-N PIC 9(16)} for the numeric and
 * non-zero checks. The same rule is shared across {@code COACTVWC.cbl},
 * {@code COCRDLIC.cbl}, and {@code COCRDSLC.cbl}.
 *
 * <p>The COBOL logic rejects the value when it is LOW-VALUES/SPACES (blank),
 * ZEROS (all zeros), or not a 16-digit number.
 */
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface CardNumber {

    String message() default "Card number must be a 16 digit number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * When {@code false}, a blank/empty value is allowed (mirrors the COBOL
     * "if supplied" filter behaviour). Defaults to {@code true}.
     */
    boolean required() default true;
}
