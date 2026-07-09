package com.aws.carddemo.authorization.fraud.service;

import com.aws.carddemo.authorization.fraud.dto.FraudActionResult;
import java.time.LocalDateTime;

/**
 * Fraud-marking orchestration, mirroring MARK-AUTH-FRAUD in COPAUS1C: load the pending auth
 * detail, toggle its fraud flag, set the fraud report date, upsert the DB2 AUTHFRDS row
 * (COPAUS2C: INSERT, and on duplicate key / SQLCODE -803 an UPDATE) and persist the detail —
 * all inside a single {@code @Transactional} boundary that replaces the CICS
 * SYNCPOINT / SYNCPOINT ROLLBACK two-phase commit.
 *
 * <p>FROZEN CONTRACT (Phase 0): do not change this signature in sub-sessions.
 */
public interface FraudService {

    /**
     * Toggle the fraud status of the authorization identified by {@code cardNum} + {@code authTs}.
     *
     * @return a {@link FraudActionResult} with a message such as {@code "AUTH MARKED FRAUD..."}
     *         (fraud confirmed) or {@code "AUTH FRAUD REMOVED..."} (fraud removed)
     */
    FraudActionResult markAuthFraud(String cardNum, LocalDateTime authTs);
}
