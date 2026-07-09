package com.aws.carddemo.authorization.fraud.service;

import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.AUTH_TS;
import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.CARD_NUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.aws.carddemo.authorization.fraud.entity.AuthFraud;
import com.aws.carddemo.authorization.fraud.repository.AuthFraudRepository;
import com.aws.carddemo.authorization.fraud.repository.PendingAuthDetailRepository;
import com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Verifies the single {@code @Transactional} boundary rolls back the pending-auth-detail
 * update when the AUTHFRDS persistence fails — the Java equivalent of the CICS
 * SYNCPOINT ROLLBACK path in COPAUS1C.
 */
@SpringBootTest
class FraudServiceRollbackTest {

    @Autowired
    private FraudService fraudService;

    @Autowired
    private PendingAuthDetailRepository pendingAuthDetailRepository;

    @MockBean
    private AuthFraudRepository authFraudRepository;

    @BeforeEach
    void reset() {
        pendingAuthDetailRepository.deleteAll();
    }

    @Test
    void authfrdsSaveFailure_rollsBackPendingDetailUpdate() {
        pendingAuthDetailRepository.save(PendingAuthDetailFixture.newDetail());
        given(authFraudRepository.save(any(AuthFraud.class)))
                .willThrow(new RuntimeException("simulated DB2 failure"));

        assertThatThrownBy(() -> fraudService.markAuthFraud(CARD_NUM, AUTH_TS))
                .isInstanceOf(RuntimeException.class);

        // Detail fraud flag must remain unset — the transaction rolled back.
        assertThat(pendingAuthDetailRepository.findByCardNumAndAuthTs(CARD_NUM, AUTH_TS)
                .orElseThrow().getFraudFlag()).isNull();
    }
}
