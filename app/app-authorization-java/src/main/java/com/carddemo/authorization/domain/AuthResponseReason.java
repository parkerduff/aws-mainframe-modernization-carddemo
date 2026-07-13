package com.carddemo.authorization.domain;

/**
 * Authorization response reason codes (BR-01.7, BR-02).
 *
 * <p>Ported from the {@code WS-DECLINE-REASON-TABLE} in {@code COPAUS1C.cbl}. Each
 * constant carries the exact 4-character code and the short description used by the
 * legacy BMS detail screen.
 */
public enum AuthResponseReason {

    APPROVED("0000", "APPROVED"),
    INVALID_CARD("3100", "INVALID CARD"),
    INSUFFICIENT_FUND("4100", "INSUFFICNT FUND"),
    CARD_NOT_ACTIVE("4200", "CARD NOT ACTIVE"),
    ACCOUNT_CLOSED("4300", "ACCOUNT CLOSED"),
    EXCEEDED_DAILY_LIMIT("4400", "EXCED DAILY LMT"),
    CARD_FRAUD("5100", "CARD FRAUD"),
    MERCHANT_FRAUD("5200", "MERCHANT FRAUD"),
    LOST_CARD("5300", "LOST CARD"),
    UNKNOWN("9000", "UNKNOWN");

    private final String code;
    private final String description;

    AuthResponseReason(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String description() {
        return description;
    }

    /**
     * Resolve a reason from its 4-character code, falling back to {@link #UNKNOWN}
     * when the code is not recognised (mirrors the {@code AT END} branch of the
     * legacy {@code SEARCH ALL}).
     */
    public static AuthResponseReason fromCode(String code) {
        if (code != null) {
            for (AuthResponseReason reason : values()) {
                if (reason.code.equals(code.trim())) {
                    return reason;
                }
            }
        }
        return UNKNOWN;
    }
}

