package com.carddemo.authorization.domain;

/**
 * Authorization match status (BR-07.3).
 *
 * <p>Ported from the {@code PA-MATCH-STATUS} 88-levels in copybook {@code CIPAUDTY}.
 */
public enum MatchStatus {

    PENDING("P"),
    AUTH_DECLINED("D"),
    PENDING_EXPIRED("E"),
    MATCHED_WITH_TRAN("M");

    private final String code;

    MatchStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static MatchStatus fromCode(String code) {
        if (code != null) {
            for (MatchStatus status : values()) {
                if (status.code.equals(code.trim())) {
                    return status;
                }
            }
        }
        return null;
    }
}

