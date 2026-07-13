package com.carddemo.authorization.service.decision;

import com.carddemo.authorization.domain.AuthResponseCode;
import com.carddemo.authorization.domain.AuthResponseReason;

/**
 * Outcome of the authorization decision (BR-01).
 */
public record AuthorizationDecision(boolean approved, String respCode, AuthResponseReason reason) {

    public static AuthorizationDecision approve() {
        return new AuthorizationDecision(true, AuthResponseCode.APPROVED, AuthResponseReason.APPROVED);
    }

    public static AuthorizationDecision decline(AuthResponseReason reason) {
        return new AuthorizationDecision(false, AuthResponseCode.DECLINED, reason);
    }
}

