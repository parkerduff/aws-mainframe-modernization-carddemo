package com.aws.carddemo.authorization.fraud.domain;

/**
 * Fraud status flag, mirroring the COBOL 88-levels in copybook CIPAUDTY:
 * <pre>
 *   88 PA-FRAUD-CONFIRMED  VALUE 'F'.
 *   88 PA-FRAUD-REMOVED    VALUE 'R'.
 * </pre>
 *
 * <p>FROZEN CONTRACT (Phase 0): the enum constants, their codes, {@link #getCode()},
 * {@link #fromCode(String)} and the {@link #toggle()} signature are stable. Sub-session 2
 * fills in the {@link #toggle()} logic (mirroring MARK-AUTH-FRAUD in COPAUS1C).
 */
public enum FraudStatus {

    CONFIRMED("F"),
    REMOVED("R");

    private final String code;

    FraudStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FraudStatus fromCode(String code) {
        if (code != null) {
            String trimmed = code.trim();
            for (FraudStatus status : values()) {
                if (status.code.equals(trimmed)) {
                    return status;
                }
            }
        }
        return null;
    }

    /**
     * Toggle the fraud status, mirroring MARK-AUTH-FRAUD in COPAUS1C
     * (PA-FRAUD-CONFIRMED &lt;-&gt; PA-FRAUD-REMOVED).
     *
     * <p>Sub-session 2 implements this logic.
     */
    public FraudStatus toggle() {
        return this == CONFIRMED ? REMOVED : CONFIRMED;
    }
}
