package com.aws.carddemo.authorization.fraud.service;

import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.AUTH_TS;
import static com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture.CARD_NUM;
import static org.assertj.core.api.Assertions.assertThat;

import com.aws.carddemo.authorization.fraud.dto.FraudActionResult;
import com.aws.carddemo.authorization.fraud.entity.AuthFraud;
import com.aws.carddemo.authorization.fraud.entity.AuthFraudId;
import com.aws.carddemo.authorization.fraud.entity.PendingAuthDetail;
import com.aws.carddemo.authorization.fraud.repository.AuthFraudRepository;
import com.aws.carddemo.authorization.fraud.repository.PendingAuthDetailRepository;
import com.aws.carddemo.authorization.fraud.support.PendingAuthDetailFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * End-to-end integration tests (H2) for the fraud upsert flow, exercising the paths that
 * COPAUS1C/COPAUS2C implement against IMS + DB2.
 */
@SpringBootTest
class FraudServiceIntegrationTest {

    @Autowired
    private FraudService fraudService;

    @Autowired
    private AuthFraudRepository authFraudRepository;

    @Autowired
    private PendingAuthDetailRepository pendingAuthDetailRepository;

    @BeforeEach
    void reset() {
        authFraudRepository.deleteAll();
        pendingAuthDetailRepository.deleteAll();
    }

    @Test
    void marksFraudFirstTime_insertsAuthfrdsRow() {
        pendingAuthDetailRepository.save(PendingAuthDetailFixture.newDetail());

        FraudActionResult result = fraudService.markAuthFraud(CARD_NUM, AUTH_TS);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("AUTH MARKED FRAUD...");
        assertThat(authFraudRepository.count()).isEqualTo(1);
        AuthFraud row = authFraudRepository.findById(new AuthFraudId(CARD_NUM, AUTH_TS)).orElseThrow();
        assertThat(row.getAuthFraud()).isEqualTo("F");
        assertThat(row.getFraudRptDate()).isNotNull();
        assertThat(row.getMerchantName()).isEqualTo("ACME STORE");
        PendingAuthDetail detail = pendingAuthDetailRepository.findByCardNumAndAuthTs(CARD_NUM, AUTH_TS)
                .orElseThrow();
        assertThat(detail.getFraudFlag()).isEqualTo("F");
    }

    @Test
    void reMarkingAlreadyFraudRecord_togglesToRemoved() {
        pendingAuthDetailRepository.save(PendingAuthDetailFixture.newDetail(CARD_NUM, AUTH_TS, "F"));
        AuthFraud existing = AuthFraud.fromPendingAuthDetail(
                PendingAuthDetailFixture.newDetail(CARD_NUM, AUTH_TS, "F"));
        existing.setAuthFraud("F");
        authFraudRepository.save(existing);

        FraudActionResult result = fraudService.markAuthFraud(CARD_NUM, AUTH_TS);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("AUTH FRAUD REMOVED...");
        assertThat(authFraudRepository.count()).isEqualTo(1);
        AuthFraud row = authFraudRepository.findById(new AuthFraudId(CARD_NUM, AUTH_TS)).orElseThrow();
        assertThat(row.getAuthFraud()).isEqualTo("R");
        assertThat(pendingAuthDetailRepository.findByCardNumAndAuthTs(CARD_NUM, AUTH_TS)
                .orElseThrow().getFraudFlag()).isEqualTo("R");
    }

    @Test
    void existingAuthfrdsRow_updatesInsteadOfInserting() {
        // Pre-existing DB2 row: reproduces the SQLCODE -803 duplicate-key -> UPDATE branch.
        pendingAuthDetailRepository.save(PendingAuthDetailFixture.newDetail());
        AuthFraud existing = new AuthFraud(new AuthFraudId(CARD_NUM, AUTH_TS));
        existing.setAuthFraud("R");
        authFraudRepository.save(existing);
        assertThat(authFraudRepository.count()).isEqualTo(1);

        FraudActionResult result = fraudService.markAuthFraud(CARD_NUM, AUTH_TS);

        assertThat(result.success()).isTrue();
        // No duplicate row inserted — the existing row is updated in place.
        assertThat(authFraudRepository.count()).isEqualTo(1);
        AuthFraud row = authFraudRepository.findById(new AuthFraudId(CARD_NUM, AUTH_TS)).orElseThrow();
        assertThat(row.getAuthFraud()).isEqualTo("F");
    }
}
