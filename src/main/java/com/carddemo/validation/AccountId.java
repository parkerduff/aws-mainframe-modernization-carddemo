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
 * BR-30 &mdash; Account ID must be a non-zero 11-digit numeric value.
 *
 * <p>Migrated from COBOL paragraph {@code 1210-EDIT-ACCOUNT} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 721-755). The underlying field is
 * {@code CC-ACCT-ID PIC X(11)} defined in {@code app/cpy/CVCRD01Y.cpy}
 * (line 34), redefined as {@code CC-ACCT-ID-N PIC 9(11)} for the numeric and
 * non-zero checks. The same rule is shared across {@code COACTVWC.cbl},
 * {@code COCRDLIC.cbl}, and {@code COCRDSLC.cbl}.
 *
 * <p>The COBOL logic rejects the value when it is LOW-VALUES/SPACES (blank),
 * ZEROS (all zeros), or not an 11-digit number.
 */
@Documented
@Constraint(validatedBy = AccountIdValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface AccountId {

    String message() default "Account number must be a non-zero 11 digit number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * When {@code false}, a blank/empty value is allowed (mirrors the COBOL
     * "if supplied" filter behaviour). Defaults to {@code true}.
     */
    boolean required() default true;
}
