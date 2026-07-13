package com.carddemo.authorization.service.decision;

import com.carddemo.authorization.domain.AuthResponseReason;
import com.carddemo.authorization.service.xref.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Rule-based authorization decision engine (BR-01) — ports {@code COPAUA0C}
 * {@code 6000-MAKE-DECISION}.
 *
 * <p>Decision order:
 * <ol>
 *   <li>Pluggable fraud scoring (BR-01.8) — declines with {@code 5100}/{@code 5200}/
 *       {@code 5300} if the {@link FraudScoringEngine} flags fraud. Default is a
 *       no-op, mirroring the legacy {@code 5600-READ-PROFILE-DATA} stub.</li>
 *   <li>Missing card/account/customer → decline {@code 3100} (BR-01.5).</li>
 *   <li>Transaction amount &gt; available amount → decline {@code 4100} (BR-01.4).</li>
 *   <li>Otherwise approve (BR-01.1).</li>
 * </ol>
 * Available amount uses the pending-auth summary when present, otherwise the account
 * master (BR-01.2, BR-01.3).
 */
@Service
public class RuleBasedAuthorizationDecisionEngine implements AuthorizationDecisionEngine {

    private final FraudScoringEngine fraudScoringEngine;

    public RuleBasedAuthorizationDecisionEngine(FraudScoringEngine fraudScoringEngine) {
        this.fraudScoringEngine = fraudScoringEngine;
    }

    @Override
    public AuthorizationDecision decide(AuthorizationContext context) {
        // BR-01.8 — pluggable fraud/scoring extension point (default no-op).
        Optional<AuthResponseReason> fraudReason = fraudScoringEngine.assess(context);
        if (fraudReason.isPresent()) {
            return AuthorizationDecision.decline(fraudReason.get());
        }

        // BR-01.5 — neither cross-reference/customer nor any credit data available.
        boolean hasCredit = context.summary().isPresent() || context.account().isPresent();
        if (!context.cardFoundInXref() || !context.customerFound() || !hasCredit) {
            return AuthorizationDecision.decline(AuthResponseReason.INVALID_CARD);
        }

        // BR-01.2 / BR-01.3 — prefer summary data, fall back to account master.
        BigDecimal available = availableAmount(context);

        // BR-01.4 — decline only when strictly greater than available (boundary = approve).
        if (context.request().transactionAmt().compareTo(available) > 0) {
            return AuthorizationDecision.decline(AuthResponseReason.INSUFFICIENT_FUND);
        }

        return AuthorizationDecision.approve();
    }

    private static BigDecimal availableAmount(AuthorizationContext context) {
        if (context.summary().isPresent()) {
            return context.summary().get().availableAmount();
        }
        Account account = context.account().orElseThrow();
        return account.creditLimit().subtract(account.currentBalance());
    }
}

