package com.carddemo.authorization.batch;

import com.carddemo.authorization.domain.PendingAuthDetail;
import com.carddemo.authorization.domain.PendingAuthSummary;
import com.carddemo.authorization.repository.PendingAuthSummaryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expired-authorization purge (BR-09) — ports the batch program {@code CBPAUP0C}
 * ({@code CBPAUP0J}).
 *
 * <p>For each account, expired authorization details are removed and the summary
 * counters/amounts are adjusted; for expired approved authorizations the held credit
 * is released (BR-09.2). Accounts with no remaining authorizations have their summary
 * deleted (BR-09.3).
 */
@Service
public class ExpiredAuthorizationPurgeService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredAuthorizationPurgeService.class);

    private final PendingAuthSummaryRepository summaryRepository;
    private final Clock clock;

    public ExpiredAuthorizationPurgeService(PendingAuthSummaryRepository summaryRepository, Clock clock) {
        this.summaryRepository = summaryRepository;
        this.clock = clock;
    }

    /**
     * Purge authorizations older than {@code expiryDays} days (BR-09.1).
     *
     * @return the number of authorization details deleted.
     */
    @Transactional
    public int purgeExpired(int expiryDays) {
        LocalDate today = LocalDate.now(clock);
        int purged = 0;

        for (PendingAuthSummary summary : summaryRepository.findAll()) {
            List<PendingAuthDetail> expired = new ArrayList<>();
            for (PendingAuthDetail detail : summary.getDetails()) {
                long ageDays = ChronoUnit.DAYS.between(detail.getAuthTs().toLocalDate(), today);
                if (ageDays >= expiryDays) {
                    expired.add(detail);
                }
            }

            for (PendingAuthDetail detail : expired) {
                applyRemovalToSummary(summary, detail);
                summary.getDetails().remove(detail); // orphanRemoval deletes the row
                purged++;
            }

            // BR-09.3 — drop the summary once it has no remaining authorizations.
            if (summary.getApprovedAuthCount() <= 0 && summary.getDeclinedAuthCount() <= 0) {
                summaryRepository.delete(summary);
            } else {
                summaryRepository.save(summary);
            }
        }

        log.info("Expired-authorization purge complete: expiryDays={}, purged={}", expiryDays, purged);
        return purged;
    }

    /**
     * BR-09.2 — reverse a deleted authorization's effect on the summary. Approved
     * authorizations release their held credit and decrement approved totals; declined
     * authorizations decrement declined totals.
     */
    private void applyRemovalToSummary(PendingAuthSummary summary, PendingAuthDetail detail) {
        if (detail.isApproved()) {
            summary.setApprovedAuthCount(summary.getApprovedAuthCount() - 1);
            summary.setApprovedAuthAmount(summary.getApprovedAuthAmount().subtract(nvl(detail.getApprovedAmt())));
            summary.setCreditBalance(summary.getCreditBalance().subtract(nvl(detail.getApprovedAmt())));
        } else {
            summary.setDeclinedAuthCount(summary.getDeclinedAuthCount() - 1);
            summary.setDeclinedAuthAmount(summary.getDeclinedAuthAmount().subtract(nvl(detail.getTransactionAmt())));
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

