package com.carddemo.authorization.domain;

/**
 * Fraud flag values (BR-03).
 *
 * <p>Ported from the {@code PA-AUTH-FRAUD} 88-levels in copybook {@code CIPAUDTY}
 * ({@code 'F'} = confirmed, {@code 'R'} = removed) and the {@code WS-FRD-ACTION}
 * codes in {@code COPAUS1C}/{@code COPAUS2C}. A blank/space means "never tagged".
 */
public final class FraudFlag {

    /** Fraud confirmed (COBOL {@code PA-FRAUD-CONFIRMED} / action REPORT). */
    public static final String CONFIRMED = "F";

    /** Fraud removed (COBOL {@code PA-FRAUD-REMOVED} / action REMOVE). */
    public static final String REMOVED = "R";

    /** Never tagged. */
    public static final String NONE = " ";

    private FraudFlag() {
    }

    public static boolean isConfirmed(String flag) {
        return CONFIRMED.equals(flag);
    }
}

