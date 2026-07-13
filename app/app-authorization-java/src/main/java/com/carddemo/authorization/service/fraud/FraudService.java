package com.carddemo.authorization.service.fraud;

import com.carddemo.authorization.domain.AuthFraud;
import com.carddemo.authorization.domain.AuthFraudId;
import com.carddemo.authorization.domain.FraudFlag;
import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.AuthFraudRepository;
import com.carddemo.authorization.repository.PendingAuthDetailRepository;
import com.carddemo.authorization.service.AuthorizationNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fraud marking workflow (BR-03, BR-04, BR-05) — ports {@code COPAUS1C}
 * {@code MARK-AUTH-FRAUD} + {@code COPAUS2C} (DB2 {@code AUTHFRDS} upsert).
 *
 * <p>{@link #toggleFraud(Long)} runs in a single transaction so the authorization
 * detail update and the fraud-table upsert either both commit or both roll back —
 * the equivalent of the legacy IMS/DB2 {@code SYNCPOINT}/{@code ROLLBACK}
 * (BR-05).
 */
@Service
public class FraudService {

    private static final DateTimeFormatter RPT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PendingAuthDetailRepository detailRepository;
    private final AuthFraudRepository authFraudRepository;
    private final Clock clock;

    public FraudService(PendingAuthDetailRepository detailRepository,
                        AuthFraudRepository authFraudRepository,
                        Clock clock) {
        this.detailRepository = detailRepository;
        this.authFraudRepository = authFraudRepository;
        this.clock = clock;
    }

    /**
     * Toggle the fraud flag on an authorization detail and upsert the fraud record
     * (BR-03, BR-04, BR-05).
     */
    @Transactional
    public FraudToggleResult toggleFraud(Long detailId) {
        PendingAuthDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new AuthorizationNotFoundException(
                        "Authorization detail not found: " + detailId));

        // BR-03.1 / BR-03.2 — toggle confirmed <-> removed.
        boolean nowConfirmed = !detail.isFraudConfirmed();
        String newFlag = nowConfirmed ? FraudFlag.CONFIRMED : FraudFlag.REMOVED;
        String action = nowConfirmed ? "REPORT" : "REMOVE";
        LocalDate reportDate = LocalDate.now(clock);

        // BR-04 — upsert the fraud record first; if it fails the whole tx rolls back
        // (BR-05), so the detail update below is never persisted on its own.
        upsertFraudRecord(detail, newFlag, reportDate);

        // BR-03.3 — stamp the detail and persist the toggled flag (IMS REPL equivalent).
        detail.setAuthFraud(newFlag);
        detail.setFraudRptDate(reportDate.format(RPT_DATE));
        detailRepository.save(detail);

        return new FraudToggleResult(detailId, detail.getCardNum(), nowConfirmed, action, reportDate);
    }

    /**
     * Insert-or-update the {@code AUTHFRDS} row (BR-04). On insert all detail fields
     * are copied (BR-04.1); on an existing row only {@code AUTH_FRAUD} and
     * {@code FRAUD_RPT_DATE} are updated — the equivalent of the DB2 {@code -803}
     * duplicate-key branch in {@code COPAUS2C} (BR-04.2, BR-04.3).
     */
    private void upsertFraudRecord(PendingAuthDetail detail, String flag, LocalDate reportDate) {
        AuthFraudId id = new AuthFraudId(detail.getCardNum(), detail.getAuthTs());
        Optional<AuthFraud> existing = authFraudRepository.findById(id);

        AuthFraud fraud;
        if (existing.isPresent()) {
            fraud = existing.get();
        } else {
            fraud = new AuthFraud(detail.getCardNum(), detail.getAuthTs());
            copyDetailFields(fraud, detail);
        }
        fraud.setAuthFraud(flag);
        fraud.setFraudRptDate(reportDate);
        authFraudRepository.save(fraud);
    }

    private static void copyDetailFields(AuthFraud fraud, PendingAuthDetail detail) {
        fraud.setAuthType(detail.getAuthType());
        fraud.setCardExpiryDate(detail.getCardExpiryDate());
        fraud.setMessageType(detail.getMessageType());
        fraud.setMessageSource(detail.getMessageSource());
        fraud.setAuthIdCode(detail.getAuthIdCode());
        fraud.setAuthRespCode(detail.getAuthRespCode());
        fraud.setAuthRespReason(detail.getAuthRespReason());
        fraud.setProcessingCode(detail.getProcessingCode());
        fraud.setTransactionAmt(detail.getTransactionAmt());
        fraud.setApprovedAmt(detail.getApprovedAmt());
        fraud.setMerchantCategoryCode(detail.getMerchantCategoryCode());
        fraud.setAcqrCountryCode(detail.getAcqrCountryCode());
        fraud.setPosEntryMode(detail.getPosEntryMode() == null
                ? null : detail.getPosEntryMode().shortValue());
        fraud.setMerchantId(detail.getMerchantId());
        fraud.setMerchantName(detail.getMerchantName());
        fraud.setMerchantCity(detail.getMerchantCity());
        fraud.setMerchantState(detail.getMerchantState());
        fraud.setMerchantZip(detail.getMerchantZip());
        fraud.setTransactionId(detail.getTransactionId());
        fraud.setMatchStatus(detail.getMatchStatus());

        PendingAuthSummary summary = detail.getSummary();
        if (summary != null) {
            fraud.setAcctId(summary.getAcctId());
            fraud.setCustId(summary.getCustId());
        }
    }
}

