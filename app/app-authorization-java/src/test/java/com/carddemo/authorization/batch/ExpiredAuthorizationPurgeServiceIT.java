package com.carddemo.authorization.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Tests for the expired-authorization purge.
 *
 * <p>Traceability: BR-09.1 (expiry threshold), BR-09.2 (counter/credit adjustment),
 * BR-09.3 (empty-summary deletion).
 */
@DataJpaTest
class ExpiredAuthorizationPurgeServiceIT {

    private static final LocalDate TODAY = LocalDate.of(2024, 3, 1);

    @Autowired
    private PendingAuthSummaryRepository summaryRepository;
    @Autowired
    private TestEntityManager entityManager;

    private ExpiredAuthorizationPurgeService purgeService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        purgeService = new ExpiredAuthorizationPurgeService(summaryRepository, clock);
    }

    private PendingAuthDetail approvedDetail(int ageDays, BigDecimal amount) {
        PendingAuthDetail detail = new PendingAuthDetail();
        detail.setAuthTs(LocalDateTime.of(TODAY.minusDays(ageDays), java.time.LocalTime.NOON));
        detail.setCardNum("4111111111111111");
        detail.setAuthRespCode("00");
        detail.setTransactionAmt(amount);
        detail.setApprovedAmt(amount);
        detail.setMatchStatus(MatchStatus.PENDING.code());
        detail.setAuthFraud(FraudFlag.NONE);
        return detail;
    }

    @Test
    void purgesExpiredApprovedAndAdjustsSummary() {
        PendingAuthSummary summary = new PendingAuthSummary(100000001L, 11L);
        summary.setCreditLimit(new BigDecimal("1000.00"));
        summary.setCreditBalance(new BigDecimal("300.00"));
        summary.setApprovedAuthCount(2);
        summary.setApprovedAuthAmount(new BigDecimal("300.00"));
        summary.addDetail(approvedDetail(10, new BigDecimal("150.00"))); // expired
        summary.addDetail(approvedDetail(1, new BigDecimal("150.00")));  // recent
        summaryRepository.save(summary);
        entityManager.flush();
        entityManager.clear();

        int purged = purgeService.purgeExpired(5);

        assertThat(purged).isEqualTo(1);
        PendingAuthSummary reloaded = summaryRepository.findById(100000001L).orElseThrow();
        assertThat(reloaded.getDetails()).hasSize(1);
        assertThat(reloaded.getApprovedAuthCount()).isEqualTo(1);
        assertThat(reloaded.getApprovedAuthAmount()).isEqualByComparingTo("150.00");
        // BR-09.2 — held credit released for the purged approved auth.
        assertThat(reloaded.getCreditBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void deletesSummaryWhenAllAuthorizationsPurged() {
        PendingAuthSummary summary = new PendingAuthSummary(100000002L, 12L);
        summary.setCreditLimit(new BigDecimal("1000.00"));
        summary.setCreditBalance(new BigDecimal("150.00"));
        summary.setApprovedAuthCount(1);
        summary.setApprovedAuthAmount(new BigDecimal("150.00"));
        summary.addDetail(approvedDetail(30, new BigDecimal("150.00")));
        summaryRepository.save(summary);
        entityManager.flush();
        entityManager.clear();

        int purged = purgeService.purgeExpired(5);

        assertThat(purged).isEqualTo(1);
        assertThat(summaryRepository.findById(100000002L)).isEmpty();
    }
}

