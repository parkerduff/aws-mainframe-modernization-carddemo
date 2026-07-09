package com.aws.carddemo.authorization.fraud.service;

import com.aws.carddemo.authorization.fraud.domain.FraudStatus;
import com.aws.carddemo.authorization.fraud.dto.FraudActionResult;
import com.aws.carddemo.authorization.fraud.entity.AuthFraud;
import com.aws.carddemo.authorization.fraud.entity.AuthFraudId;
import com.aws.carddemo.authorization.fraud.entity.PendingAuthDetail;
import com.aws.carddemo.authorization.fraud.repository.AuthFraudRepository;
import com.aws.carddemo.authorization.fraud.repository.PendingAuthDetailRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link FraudService} implementation mirroring MARK-AUTH-FRAUD in COPAUS1C.
 *
 * <p>COPAUS1C LINKs to COPAUS2C to upsert the DB2 AUTHFRDS row and then updates the pending
 * auth detail, committing with a CICS SYNCPOINT (or rolling back with SYNCPOINT ROLLBACK on
 * error). Here the entire orchestration runs inside a single {@link Transactional} boundary:
 * any {@link RuntimeException} rolls back both the AUTHFRDS upsert and the detail update.
 */
@Service
public class FraudServiceImpl implements FraudService {

    private final PendingAuthDetailRepository pendingAuthDetailRepository;
    private final AuthFraudRepository authFraudRepository;

    public FraudServiceImpl(
            PendingAuthDetailRepository pendingAuthDetailRepository,
            AuthFraudRepository authFraudRepository) {
        this.pendingAuthDetailRepository = pendingAuthDetailRepository;
        this.authFraudRepository = authFraudRepository;
    }

    @Override
    @Transactional
    public FraudActionResult markAuthFraud(String cardNum, LocalDateTime authTs) {
        Optional<PendingAuthDetail> maybeDetail =
                pendingAuthDetailRepository.findByCardNumAndAuthTs(cardNum, authTs);
        if (maybeDetail.isEmpty()) {
            return FraudActionResult.failure(
                    "PENDING AUTH DETAIL NOT FOUND FOR CARD " + cardNum + " AT " + authTs);
        }
        PendingAuthDetail detail = maybeDetail.get();

        // Mirror COBOL: a not-confirmed (or unset) flag toggles to CONFIRMED first.
        FraudStatus current = FraudStatus.fromCode(detail.getFraudFlag());
        if (current == null) {
            current = FraudStatus.REMOVED;
        }
        FraudStatus newStatus = current.toggle();

        LocalDate reportDate = LocalDate.now();
        detail.setFraudFlag(newStatus.getCode());
        detail.setFraudRptDate(reportDate);

        // Upsert AUTHFRDS (COPAUS2C: INSERT, and on SQLCODE -803 duplicate-key -> UPDATE).
        AuthFraudId id = new AuthFraudId(cardNum, authTs);
        AuthFraud authFraud = authFraudRepository.findById(id).orElse(null);
        if (authFraud == null) {
            authFraud = AuthFraud.fromPendingAuthDetail(detail);
        }
        authFraud.setAuthFraud(newStatus.getCode());
        authFraud.setFraudRptDate(reportDate);
        authFraudRepository.save(authFraud);

        pendingAuthDetailRepository.save(detail);

        if (newStatus == FraudStatus.CONFIRMED) {
            return FraudActionResult.success("AUTH MARKED FRAUD...");
        }
        return FraudActionResult.success("AUTH FRAUD REMOVED...");
    }
}
