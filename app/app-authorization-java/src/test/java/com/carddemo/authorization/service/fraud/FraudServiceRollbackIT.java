package com.carddemo.authorization.service.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.MatchStatus;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.AuthFraudRepository;
import com.carddemo.authorization.repository.PendingAuthDetailRepository;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies the fraud toggle's transactional integrity.
 *
 * <p>Traceability: BR-05.2 — if the fraud-table write fails, the authorization-detail
 * update is rolled back (equivalent of the legacy SYNCPOINT ROLLBACK).
 */
@SpringBootTest(properties = "carddemo.purge.enabled=false")
class FraudServiceRollbackIT {

    private static final LocalDateTime AUTH_TS = LocalDateTime.of(2024, 2, 1, 9, 30, 0);

    @Autowired
    private FraudService fraudService;
    @Autowired
    private PendingAuthSummaryRepository summaryRepository;
    @Autowired
    private PendingAuthDetailRepository detailRepository;

    @MockBean
    private AuthFraudRepository authFraudRepository;

    private Long summaryId;

    @AfterEach
    void cleanUp() {
        if (summaryId != null) {
            summaryRepository.deleteById(summaryId);
        }
    }

    @Test
    void toggleRollsBackDetailWhenFraudWriteFails() {
        Long detailId = seedDetail();

        when(authFraudRepository.findById(any())).thenReturn(Optional.empty());
        when(authFraudRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("simulated DB2 failure"));

        assertThatThrownBy(() -> fraudService.toggleFraud(detailId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // BR-05.2 — the detail's fraud flag must be unchanged after rollback.
        PendingAuthDetail reloaded = detailRepository.findById(detailId).orElseThrow();
        assertThat(reloaded.getAuthFraud()).isEqualTo(FraudFlag.NONE);
        assertThat(reloaded.getFraudRptDate()).isNull();
    }

    private Long seedDetail() {
        PendingAuthSummary summary = new PendingAuthSummary(200000002L, 22L);
        PendingAuthDetail detail = new PendingAuthDetail();
        detail.setAuthTs(AUTH_TS);
        detail.setCardNum("4222222222222222");
        detail.setAuthRespCode("00");
        detail.setTransactionAmt(new BigDecimal("100.00"));
        detail.setApprovedAmt(new BigDecimal("100.00"));
        detail.setMatchStatus(MatchStatus.PENDING.code());
        detail.setAuthFraud(FraudFlag.NONE);
        summary.addDetail(detail);
        PendingAuthSummary saved = summaryRepository.saveAndFlush(summary);
        summaryId = saved.getAcctId();
        return saved.getDetails().get(0).getId();
    }
}

