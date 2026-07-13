package com.carddemo.authorization.service.decision;

/**
 * Authorization decisioning contract (BR-01) — replaces {@code COPAUA0C}
 * {@code 6000-MAKE-DECISION}.
 */
public interface AuthorizationDecisionEngine {

    AuthorizationDecision decide(AuthorizationContext context);
}

