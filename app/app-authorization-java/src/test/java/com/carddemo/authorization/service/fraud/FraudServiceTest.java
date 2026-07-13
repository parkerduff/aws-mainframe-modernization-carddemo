package com.carddemo.authorization.service.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.authorization.domain.AuthFraud;
import com.carddemo.authorization.domain.AuthFraudId;
import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.AuthFraudRepository;
import com.carddemo.authorization.repository.PendingAuthDetailRepository;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Persistence tests for the fraud toggle + upsert.
 *
 * <p>Traceability: BR-03 (mark/unmark toggle), BR-04 (fraud persistence + idempotent
 * upsert / -803 equivalent), BR-03.3 (report date stamping).
 */
@DataJpaTest
class FraudServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 1, 20);
    private static final LocalDateTime AUTH_TS = LocalDateTime.of(2024, 1, 15, 12, 0, 0);

    @Autowired
    private PendingAuthSummaryRepository summaryRepository;
    @Autowired
    private PendingAuthDetailRepository detailRepository;
    @Autowired
    private AuthFraudRepository authFraudRepository;
    @Autowired
    private TestEntityManager entityManager;

    private FraudService fraudService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        fraudService = new FraudService(detailRepository, authFraudRepository, clock);
    }

    private Long seedApprovedDetail() {
        PendingAuthSummary summary = new PendingAuthSummary(100000001L, 11L);
        PendingAuthDetail detail = new PendingAuthDetail();
        detail.setAuthTs(AUTH_TS);
        detail.setCardNum("4111111111111111");
        detail.setAuthType("0100");
        detail.setAuthRespCode("00");
        detail.setTransactionAmt(new BigDecimal("500.00"));
        detail.setApprovedAmt(new BigDecimal("500.00"));
        detail.setPosEntryMode(90);
        detail.setMatchStatus(MatchStatus.PENDING.code());
        detail.setAuthFraud(FraudFlag.NONE);
        summary.addDetail(detail);
        PendingAuthSummary saved = summaryRepository.saveAndFlush(summary);
        Long id = saved.getDetails().get(0).getId();
        entityManager.clear();
        return id;
    }

    @Test
    void toggleMarksFraudAndUpsertsRow() {
        // BR-03.2, BR-04.1, BR-03.3
        Long id = seedApprovedDetail();

        FraudToggleResult result = fraudService.toggleFraud(id);

        assertThat(result.fraudConfirmed()).isTrue();
        assertThat(result.action()).isEqualTo("REPORT");
        assertThat(result.fraudRptDate()).isEqualTo(TODAY);

        PendingAuthDetail reloaded = detailRepository.findById(id).orElseThrow();
        assertThat(reloaded.getAuthFraud()).isEqualTo(FraudFlag.CONFIRMED);
        assertThat(reloaded.getFraudRptDate()).isEqualTo(TODAY.toString());

        AuthFraud fraud = authFraudRepository
                .findById(new AuthFraudId("4111111111111111", AUTH_TS)).orElseThrow();
        assertThat(fraud.getAuthFraud()).isEqualTo(FraudFlag.CONFIRMED);
        assertThat(fraud.getFraudRptDate()).isEqualTo(TODAY);
        assertThat(fraud.getApprovedAmt()).isEqualByComparingTo("500.00");
        assertThat(fraud.getAcctId()).isEqualTo(100000001L);
        assertThat(authFraudRepository.count()).isEqualTo(1);
    }

    @Test
    void toggleTwiceRemovesFraud() {
        // BR-03.1 — second toggle flips confirmed -> removed, updating the same row.
        Long id = seedApprovedDetail();

        fraudService.toggleFraud(id);
        FraudToggleResult result = fraudService.toggleFraud(id);

        assertThat(result.fraudConfirmed()).isFalse();
        assertThat(result.action()).isEqualTo("REMOVE");

        PendingAuthDetail reloaded = detailRepository.findById(id).orElseThrow();
        assertThat(reloaded.getAuthFraud()).isEqualTo(FraudFlag.REMOVED);

        AuthFraud fraud = authFraudRepository
                .findById(new AuthFraudId("4111111111111111", AUTH_TS)).orElseThrow();
        assertThat(fraud.getAuthFraud()).isEqualTo(FraudFlag.REMOVED);
        assertThat(authFraudRepository.count()).isEqualTo(1);
    }

    @Test
    void repeatedToggleNeverDuplicatesFraudRow() {
        // BR-04.2, BR-04.3 — upsert idempotency (equivalent of DB2 SQLCODE -803).
        Long id = seedApprovedDetail();

        fraudService.toggleFraud(id);
        assertThat(authFraudRepository.count()).isEqualTo(1);
        fraudService.toggleFraud(id);
        assertThat(authFraudRepository.count()).isEqualTo(1);
        fraudService.toggleFraud(id);
        assertThat(authFraudRepository.count()).isEqualTo(1);

        AuthFraud fraud = authFraudRepository
                .findById(new AuthFraudId("4111111111111111", AUTH_TS)).orElseThrow();
        assertThat(fraud.getAuthFraud()).isEqualTo(FraudFlag.CONFIRMED);
    }
}

