package com.carddemo.authorization.service.decision;

import com.carddemo.authorization.domain.AuthResponseReason;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default fraud scoring engine — a no-op (BR-01.8).
 *
 * <p>Mirrors the legacy {@code 5600-READ-PROFILE-DATA} {@code CONTINUE} stub: it never
 * flags fraud, so authorization decisioning stays rule-based + manual operator
 * tagging. Provided only when no other {@link FraudScoringEngine} bean is defined, so
 * a scoring implementation can be dropped in without touching the decision engine.
 */
@Configuration
public class NoOpFraudScoringEngine {

    @Bean
    @ConditionalOnMissingBean(FraudScoringEngine.class)
    public FraudScoringEngine fraudScoringEngine() {
        return new FraudScoringEngine() {
            @Override
            public Optional<AuthResponseReason> assess(AuthorizationContext context) {
                return Optional.empty();
            }
        };
    }
}

