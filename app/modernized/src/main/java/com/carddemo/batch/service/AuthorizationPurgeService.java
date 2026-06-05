package com.carddemo.batch.service;

import com.carddemo.batch.model.AuthorizationDetail;
import com.carddemo.batch.model.AuthorizationSummary;
import com.carddemo.batch.model.PurgeResult;
import com.carddemo.batch.repository.AuthorizationRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Modern Java port of {@code CBPAUP0C.cbl} (R20 — Expired Authorization Purge).
 *
 * <p>The original IMS batch program walks every pending-authorization summary
 * (root) segment and its detail (child) segments, deletes details whose
 * authorization date is at least {@code expiryDays} old, adjusts the parent
 * summary's approved/declined counters accordingly, and finally deletes the
 * summary itself once it has no remaining approved or declined auths. A
 * checkpoint is taken after every {@code checkpointFrequency} summaries.
 *
 * <p>Key modernizations:
 * <ul>
 *   <li>The 9's-complement Julian {@code PA-AUTH-DATE-9C} encoding is resolved
 *       into a {@link LocalDate} at the model boundary, so expiry is computed
 *       with {@link ChronoUnit#DAYS} instead of integer Julian subtraction
 *       (mirrors {@code 4000-CHECK-IF-EXPIRED}, lines 277-300).</li>
 *   <li>The original double-checks {@code PA-APPROVED-AUTH-CNT} on line 156;
 *       this port applies the clearly-intended rule
 *       {@code approvedAuthCnt <= 0 AND declinedAuthCnt <= 0} (lines 156-158).</li>
 *   <li>Surviving summaries whose counters changed are persisted via
 *       {@link AuthorizationRepository#updateSummary(AuthorizationSummary)}.</li>
 * </ul>
 */
public class AuthorizationPurgeService {

    /** {@code MOVE 5 TO WS-EXPIRY-DAYS} default (line 199). */
    public static final int DEFAULT_EXPIRY_DAYS = 5;

    /** {@code MOVE 5 TO P-CHKP-FREQ} default (line 202). */
    public static final int DEFAULT_CHECKPOINT_FREQUENCY = 5;

    private final AuthorizationRepository repository;
    private final int expiryDays;
    private final int checkpointFrequency;

    public AuthorizationPurgeService(AuthorizationRepository repository) {
        this(repository, DEFAULT_EXPIRY_DAYS, DEFAULT_CHECKPOINT_FREQUENCY);
    }

    public AuthorizationPurgeService(AuthorizationRepository repository,
                                     int expiryDays,
                                     int checkpointFrequency) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.expiryDays = expiryDays >= 1 ? expiryDays : DEFAULT_EXPIRY_DAYS;
        this.checkpointFrequency = checkpointFrequency >= 1
                ? checkpointFrequency
                : DEFAULT_CHECKPOINT_FREQUENCY;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public int getCheckpointFrequency() {
        return checkpointFrequency;
    }

    /**
     * Parses a raw expiry-days parameter the way {@code CBPAUP0C} does:
     * use the value only when it is numeric, otherwise default to
     * {@link #DEFAULT_EXPIRY_DAYS} (mirrors {@code IF P-EXPIRY-DAYS IS NUMERIC}
     * on lines 196-200).
     */
    public static int resolveExpiryDays(String raw) {
        return parseNumericOrDefault(raw, DEFAULT_EXPIRY_DAYS);
    }

    /**
     * Parses a raw checkpoint-frequency parameter, defaulting to
     * {@link #DEFAULT_CHECKPOINT_FREQUENCY} when blank or non-numeric (mirrors
     * lines 201-203).
     */
    public static int resolveCheckpointFrequency(String raw) {
        return parseNumericOrDefault(raw, DEFAULT_CHECKPOINT_FREQUENCY);
    }

    private static int parseNumericOrDefault(String raw, int defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || !trimmed.chars().allMatch(Character::isDigit)) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(trimmed);
            return value >= 1 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Runs the purge against the current system date. */
    public PurgeResult purge() {
        return purge(LocalDate.now());
    }

    /**
     * Runs the purge as of {@code currentDate}.
     *
     * <p>Reproduces {@code MAIN-PARA} (lines 136-180): for each summary, delete
     * every expired detail and adjust counters; delete the summary if it is left
     * with no approved or declined auths; checkpoint every
     * {@code checkpointFrequency} summaries; and take a final checkpoint at the
     * end.
     */
    public PurgeResult purge(LocalDate currentDate) {
        Objects.requireNonNull(currentDate, "currentDate");

        int summariesRead = 0;
        int summariesDeleted = 0;
        int detailsRead = 0;
        int detailsDeleted = 0;
        int checkpoints = 0;
        int processedSinceCheckpoint = 0;

        for (AuthorizationSummary summary : repository.findAllSummaries()) {
            summariesRead++;
            processedSinceCheckpoint++;

            boolean countersChanged = false;
            List<AuthorizationDetail> details = repository.findDetailsBySummary(summary.getAcctId());
            for (AuthorizationDetail detail : details) {
                detailsRead++;
                if (isExpired(detail, currentDate)) {
                    adjustSummaryCounters(summary, detail);
                    repository.deleteDetail(detail.transactionId());
                    detailsDeleted++;
                    countersChanged = true;
                }
            }

            if (summary.getApprovedAuthCnt() <= 0 && summary.getDeclinedAuthCnt() <= 0) {
                repository.deleteSummary(summary.getAcctId());
                summariesDeleted++;
            } else if (countersChanged) {
                repository.updateSummary(summary);
            }

            if (processedSinceCheckpoint > checkpointFrequency) {
                repository.checkpoint();
                checkpoints++;
                processedSinceCheckpoint = 0;
            }
        }

        // Final checkpoint (line 169).
        repository.checkpoint();
        checkpoints++;

        return new PurgeResult(summariesRead, summariesDeleted, detailsRead, detailsDeleted, checkpoints);
    }

    /**
     * Expiry rule from {@code 4000-CHECK-IF-EXPIRED} (lines 280-296):
     * {@code daysDifference = currentDate - authDate; expired when >= expiryDays}.
     */
    public boolean isExpired(AuthorizationDetail detail, LocalDate currentDate) {
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(currentDate, "currentDate");
        long daysDifference = ChronoUnit.DAYS.between(detail.authDate(), currentDate);
        return daysDifference >= expiryDays;
    }

    /**
     * Counter adjustment from {@code 4000-CHECK-IF-EXPIRED} (lines 287-293):
     * deleting an approved auth ({@code PA-AUTH-RESP-CODE = '00'}) decrements the
     * approved count and subtracts its approved amount; deleting a declined auth
     * decrements the declined count and subtracts its transaction amount.
     */
    public void adjustSummaryCounters(AuthorizationSummary summary, AuthorizationDetail detail) {
        if (detail.isApproved()) {
            summary.setApprovedAuthCnt(summary.getApprovedAuthCnt() - 1);
            summary.setApprovedAuthAmt(summary.getApprovedAuthAmt().subtract(nullToZero(detail.approvedAmt())));
        } else {
            summary.setDeclinedAuthCnt(summary.getDeclinedAuthCnt() - 1);
            summary.setDeclinedAuthAmt(summary.getDeclinedAuthAmt().subtract(nullToZero(detail.transactionAmt())));
        }
    }

    private static java.math.BigDecimal nullToZero(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }
}
