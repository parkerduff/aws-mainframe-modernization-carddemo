package com.carddemo.authorization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carddemo.authorization.TestFixtures;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.messaging.AuthorizationReply;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import com.carddemo.authorization.service.decision.RuleBasedAuthorizationDecisionEngine;
import com.carddemo.authorization.service.xref.Account;
import com.carddemo.authorization.service.xref.AccountService;
import com.carddemo.authorization.service.xref.CardXref;
import com.carddemo.authorization.service.xref.CrossReferenceService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for authorization request orchestration.
 *
 * <p>Traceability: BR-07 (orchestration + persistence), BR-06.2 (reply), BR-07.4
 * (no persistence when card not found).
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationRequestServiceTest {

    @Mock
    private CrossReferenceService crossReferenceService;
    @Mock
    private AccountService accountService;
    @Mock
    private PendingAuthSummaryRepository summaryRepository;

    private AuthorizationRequestService service;

    private static final long ACCT_ID = 100000001L;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneOffset.UTC);
        service = new AuthorizationRequestService(
                crossReferenceService, accountService, summaryRepository,
                new RuleBasedAuthorizationDecisionEngine(context -> Optional.empty()), clock);
    }

    private void cardResolvesToAccount(BigDecimal creditLimit, BigDecimal currentBalance) {
        when(crossReferenceService.findByCardNum(TestFixtures.CARD_NUM))
                .thenReturn(Optional.of(new CardXref(TestFixtures.CARD_NUM, 11L, ACCT_ID)));
        when(accountService.findByAcctId(ACCT_ID))
                .thenReturn(Optional.of(new Account(ACCT_ID, "Y", currentBalance, creditLimit,
                        new BigDecimal("1000.00"))));
        when(summaryRepository.findById(ACCT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void approvedRequestPersistsSummaryAndDetail() {
        cardResolvesToAccount(new BigDecimal("5000.00"), new BigDecimal("250.00"));

        AuthorizationReply reply = service.process(TestFixtures.request(new BigDecimal("500.00")));

        assertThat(reply.authRespCode()).isEqualTo("00");
        assertThat(reply.authRespReason()).isEqualTo("0000");
        assertThat(reply.approvedAmt()).isEqualByComparingTo("500.00");
        assertThat(reply.cardNum()).isEqualTo(TestFixtures.CARD_NUM);

        ArgumentCaptor<PendingAuthSummary> saved = ArgumentCaptor.forClass(PendingAuthSummary.class);
        verify(summaryRepository).save(saved.capture());
        PendingAuthSummary summary = saved.getValue();
        assertThat(summary.getApprovedAuthCount()).isEqualTo(1);
        assertThat(summary.getApprovedAuthAmount()).isEqualByComparingTo("500.00");
        assertThat(summary.getCreditBalance()).isEqualByComparingTo("500.00");
        assertThat(summary.getDetails()).hasSize(1);
        assertThat(summary.getDetails().get(0).getMatchStatus()).isEqualTo(MatchStatus.PENDING.code());
    }

    @Test
    void insufficientFundsIsDeclinedAndCountsAsDeclined() {
        cardResolvesToAccount(new BigDecimal("1000.00"), new BigDecimal("900.00"));

        AuthorizationReply reply = service.process(TestFixtures.request(new BigDecimal("500.00")));

        assertThat(reply.authRespCode()).isEqualTo("05");
        assertThat(reply.authRespReason()).isEqualTo("4100");
        assertThat(reply.approvedAmt()).isEqualByComparingTo("0");

        ArgumentCaptor<PendingAuthSummary> saved = ArgumentCaptor.forClass(PendingAuthSummary.class);
        verify(summaryRepository).save(saved.capture());
        assertThat(saved.getValue().getDeclinedAuthCount()).isEqualTo(1);
        assertThat(saved.getValue().getDetails().get(0).getMatchStatus())
                .isEqualTo(MatchStatus.AUTH_DECLINED.code());
    }

    @Test
    void cardNotFoundIsDeclinedWithoutPersistence() {
        when(crossReferenceService.findByCardNum(TestFixtures.CARD_NUM)).thenReturn(Optional.empty());

        AuthorizationReply reply = service.process(TestFixtures.request(new BigDecimal("10.00")));

        assertThat(reply.authRespCode()).isEqualTo("05");
        assertThat(reply.authRespReason()).isEqualTo("3100");
        verify(summaryRepository, never()).save(any());
    }
}

