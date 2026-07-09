package com.aws.carddemo.authorization.fraud.dto;

/**
 * Result of a fraud-marking action.
 *
 * <p>Mirrors the COBOL {@code WS-FRAUD-STATUS-RECORD} outcome in COPAUS1C:
 * a success indicator plus a human-readable message such as
 * {@code "AUTH MARKED FRAUD..."} or {@code "AUTH FRAUD REMOVED..."}.
 *
 * <p>FROZEN CONTRACT (Phase 0): do not change this signature in sub-sessions.
 */
public record FraudActionResult(boolean success, String message) {

    public static FraudActionResult success(String message) {
        return new FraudActionResult(true, message);
    }

    public static FraudActionResult failure(String message) {
        return new FraudActionResult(false, message);
    }
}
