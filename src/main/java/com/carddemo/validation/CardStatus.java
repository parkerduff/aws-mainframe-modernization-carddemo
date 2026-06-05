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
 * BR-32 &mdash; Card active status must be {@code Y} or {@code N}.
 *
 * <p>Migrated from COBOL paragraph {@code 1240-EDIT-CARDSTATUS} in
 * {@code app/cbl/COCRDUPC.cbl} (lines 845-873). The value is checked against
 * the 88-level condition {@code FLG-YES-NO-VALID VALUES 'Y', 'N'} (line 91).
 * The same rule is shared across {@code COACTVWC.cbl}, {@code COCRDLIC.cbl},
 * and {@code COCRDSLC.cbl}.
 *
 * <p>The COBOL logic rejects the value when it is LOW-VALUES/SPACES/ZEROS
 * (blank) or anything other than {@code Y}/{@code N}.
 */
@Documented
@Constraint(validatedBy = CardStatusValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface CardStatus {

    String message() default "Card Active Status must be Y or N";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
