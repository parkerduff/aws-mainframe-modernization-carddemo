package com.aws.carddemo.exception;

/**
 * Thrown when a business validation rule is violated (e.g. duplicate user id, overlimit
 * transaction, invalid card number). Corresponds to the COBOL validation-failure flags
 * (e.g. WS-VALIDATION-FAIL-REASON in CBTRN02C).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
