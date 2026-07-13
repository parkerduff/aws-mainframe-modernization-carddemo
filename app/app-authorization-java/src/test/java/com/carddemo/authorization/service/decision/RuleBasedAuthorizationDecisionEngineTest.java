package com.carddemo.authorization.service.decision;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.authorization.TestFixtures;
import com.carddemo.authorization.domain.AuthResponseCode;
import com.carddemo.authorization.domain.AuthResponseReason;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.service.xref.Account;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the authorization decision engine.
 *
 * <p>Traceability: BR-01 (decisioning rules), BR-01.4 (limit boundary), BR-01.5
 * (missing data), BR-02 (fraud reason codes), BR-01.8 (pluggable scoring).
 */
class RuleBasedAuthorizationDecisionEngineTest {

    private final AuthorizationDecisionEngine engine =
            new RuleBasedAuthorizationDecisionEngine(context -> Optional.empty());

    private static PendingAuthSummary summaryWithAvailable(BigDecimal limit, BigDecimal balance) {
        PendingAuthSummary summary = new PendingAuthSummary(100000001L, 11L);
        summary.setCreditLimit(limit);
        summary.setCreditBalance(balance);
        return summary;
    }

    @Test
    void approvesWhenAmountBelowAvailable_usingSummary() {
        // BR-01.1, BR-01.2 — available = 1000 - 200 = 800.
        PendingAuthSummary summary = summaryWithAvailable(new BigDecimal("1000.00"), new BigDecimal("200.00"));
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("500.00")),
                Optional.of(summary), Optional.empty(), true, true);

        AuthorizationDecision decision = engine.decide(ctx);

        assertThat(decision.approved()).isTrue();
        assertThat(decision.respCode()).isEqualTo(AuthResponseCode.APPROVED);
        assertThat(decision.reason()).isEqualTo(AuthResponseReason.APPROVED);
    }

    @Test
    void approvesWhenAmountEqualsAvailable_boundary() {
        // BR-01.4 — boundary uses '>' not '>=', so amount == available approves.
        PendingAuthSummary summary = summaryWithAvailable(new BigDecimal("1000.00"), new BigDecimal("200.00"));
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("800.00")),
                Optional.of(summary), Optional.empty(), true, true);

        assertThat(engine.decide(ctx).approved()).isTrue();
    }

    @Test
    void declinesInsufficientFundsWhenAmountExceedsAvailable() {
        // BR-01.4 — 4100 INSUFFICIENT FUND.
        PendingAuthSummary summary = summaryWithAvailable(new BigDecimal("1000.00"), new BigDecimal("200.00"));
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("800.01")),
                Optional.of(summary), Optional.empty(), true, true);

        AuthorizationDecision decision = engine.decide(ctx);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.respCode()).isEqualTo(AuthResponseCode.DECLINED);
        assertThat(decision.reason()).isEqualTo(AuthResponseReason.INSUFFICIENT_FUND);
    }

    @Test
    void usesAccountMasterWhenNoSummary() {
        // BR-01.3 — available = 300 - 300 = 0, amount 0.01 declines.
        Account account = new Account(100000001L, "Y",
                new BigDecimal("300.00"), new BigDecimal("300.00"), new BigDecimal("100.00"));
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("0.01")),
                Optional.empty(), Optional.of(account), true, true);

        assertThat(engine.decide(ctx).reason()).isEqualTo(AuthResponseReason.INSUFFICIENT_FUND);
    }

    @Test
    void declinesInvalidCardWhenNoSummaryOrAccount() {
        // BR-01.5 — 3100 INVALID CARD.
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("10.00")),
                Optional.empty(), Optional.empty(), false, false);

        AuthorizationDecision decision = engine.decide(ctx);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo(AuthResponseReason.INVALID_CARD);
    }

    @Test
    void declinesInvalidCardWhenCustomerNotFound() {
        // BR-01.5 — customer master missing maps to 3100 even if an account exists.
        Account account = new Account(100000001L, "Y",
                new BigDecimal("1000.00"), new BigDecimal("0.00"), new BigDecimal("100.00"));
        AuthorizationContext ctx = new AuthorizationContext(
                TestFixtures.request(new BigDecimal("10.00")),
                Optional.empty(), Optional.of(account), true, false);

        assertThat(engine.decide(ctx).reason()).isEqualTo(AuthResponseReason.INVALID_CARD);
    }

    @Test
    void pluggableScoringCanDeclineWithFraudReasons() {
        // BR-01.8, BR-02 — a scoring engine can drive 5100/5200/5300 declines.
        for (AuthResponseReason fraud : new AuthResponseReason[]{
                AuthResponseReason.CARD_FRAUD,
                AuthResponseReason.MERCHANT_FRAUD,
                AuthResponseReason.LOST_CARD}) {
            AuthorizationDecisionEngine fraudEngine =
                    new RuleBasedAuthorizationDecisionEngine(context -> Optional.of(fraud));
            PendingAuthSummary summary =
                    summaryWithAvailable(new BigDecimal("1000.00"), BigDecimal.ZERO);
            AuthorizationContext ctx = new AuthorizationContext(
                    TestFixtures.request(new BigDecimal("1.00")),
                    Optional.of(summary), Optional.empty(), true, true);

            AuthorizationDecision decision = fraudEngine.decide(ctx);

            assertThat(decision.approved()).isFalse();
            assertThat(decision.reason()).isEqualTo(fraud);
        }
    }
}

