package com.carddemo.authorization.service.decision;

import com.carddemo.authorization.domain.AuthResponseReason;
import java.util.Optional;

/**
 * Pluggable fraud/scoring extension point (BR-01.8, BR-02).
 *
 * <p>The legacy statistical-profile paragraph {@code 5600-READ-PROFILE-DATA} in
 * {@code COPAUA0C} is a {@code CONTINUE} stub — there is no automated fraud scoring.
 * This interface preserves that limitation by default (see
 * {@link NoOpFraudScoringEngine}) while allowing a real implementation to be plugged
 * in later. An implementation returns the fraud decline reason
 * ({@link AuthResponseReason#CARD_FRAUD 5100}, {@link AuthResponseReason#MERCHANT_FRAUD 5200},
 * or {@link AuthResponseReason#LOST_CARD 5300}) or {@link Optional#empty()} to allow
 * the authorization to proceed.
 */
public interface FraudScoringEngine {

    Optional<AuthResponseReason> assess(AuthorizationContext context);
}

