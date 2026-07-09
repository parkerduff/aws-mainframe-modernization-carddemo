package com.aws.carddemo.authorization.fraud.domain;

/**
 * Fraud authorization response reason codes, mirroring the COBOL logic in
 * COPAUA0C where PA-RL-AUTH-RESP-REASON is set for fraud conditions:
 * <pre>
 *   WHEN CARD-FRAUD      MOVE '5100' TO PA-RL-AUTH-RESP-REASON
 *   WHEN MERCHANT-FRAUD  MOVE '5200' TO PA-RL-AUTH-RESP-REASON
 * </pre>
 */
public enum FraudReasonCode {

    CARD_FRAUD(5100),
    MERCHANT_FRAUD(5200);

    private final int code;

    FraudReasonCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static FraudReasonCode fromCode(int code) {
        for (FraudReasonCode reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        return null;
    }
}
