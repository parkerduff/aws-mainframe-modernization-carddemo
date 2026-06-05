package com.carddemo.batch;

import com.carddemo.batch.model.AuthorizationDetail;
import com.carddemo.batch.model.AuthorizationSummary;
import com.carddemo.batch.model.PurgeResult;
import com.carddemo.batch.repository.AuthorizationRepository;
import com.carddemo.batch.service.AuthorizationPurgeService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationPurgeServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2023, 6, 10);

    /** In-memory stand-in for the IMS auth database. */
    private static final class InMemoryAuthorizationRepository implements AuthorizationRepository {
        final List<AuthorizationSummary> summaries = new ArrayList<>();
        final Map<Long, List<AuthorizationDetail>> details = new HashMap<>();
        final List<String> deletedDetailIds = new ArrayList<>();
        final List<Long> deletedSummaryIds = new ArrayList<>();
        final List<Long> updatedSummaryIds = new ArrayList<>();
        int checkpoints = 0;

        @Override
        public List<AuthorizationSummary> findAllSummaries() {
            return new ArrayList<>(summaries);
        }

        @Override
        public List<AuthorizationDetail> findDetailsBySummary(long summaryId) {
            return new ArrayList<>(details.getOrDefault(summaryId, List.of()));
        }

        @Override
        public void deleteDetail(String detailId) {
            deletedDetailIds.add(detailId);
        }

        @Override
        public void deleteSummary(long summaryId) {
            deletedSummaryIds.add(summaryId);
        }

        @Override
        public void updateSummary(AuthorizationSummary summary) {
            updatedSummaryIds.add(summary.getAcctId());
        }

        @Override
        public void checkpoint() {
            checkpoints++;
        }
    }

    private static AuthorizationSummary summary(long acctId, int approvedCnt, int declinedCnt,
                                               String approvedAmt, String declinedAmt) {
        return new AuthorizationSummary(
                acctId, 999L, "A",
                new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                new BigDecimal("100.00"), new BigDecimal("0.00"),
                approvedCnt, declinedCnt,
                new BigDecimal(approvedAmt), new BigDecimal(declinedAmt));
    }

    private static AuthorizationDetail detail(String id, LocalDate authDate, String respCode,
                                             String transactionAmt, String approvedAmt) {
        return new AuthorizationDetail(
                authDate, LocalTime.NOON,
                "230601", "120000",
                "4111111111111111", "AUTH", "2512",
                "0100", "POS", "ABC123",
                respCode, "0000", 0,
                new BigDecimal(transactionAmt), new BigDecimal(approvedAmt),
                "5411", "840", 1,
                "MID", "Merchant", "City", "CA", "00000",
                id, "P", "N", "");
    }

    // --- Expiry calculation -------------------------------------------------

    @Test
    void authYoungerThanThresholdIsNotExpired() {
        AuthorizationPurgeService service =
                new AuthorizationPurgeService(new InMemoryAuthorizationRepository(), 5, 5);

        AuthorizationDetail threeDaysOld = detail("D1", TODAY.minusDays(3), "00", "10.00", "10.00");
        assertFalse(service.isExpired(threeDaysOld, TODAY));
    }

    @Test
    void authAtOrBeyondThresholdIsExpired() {
        AuthorizationPurgeService service =
                new AuthorizationPurgeService(new InMemoryAuthorizationRepository(), 5, 5);

        assertTrue(service.isExpired(detail("D1", TODAY.minusDays(6), "00", "10.00", "10.00"), TODAY));
        // Exactly expiryDays old qualifies (>= comparison).
        assertTrue(service.isExpired(detail("D2", TODAY.minusDays(5), "00", "10.00", "10.00"), TODAY));
    }

    // --- Counter adjustment -------------------------------------------------

    @Test
    void deletingApprovedAuthDecrementsApprovedCounters() {
        AuthorizationPurgeService service =
                new AuthorizationPurgeService(new InMemoryAuthorizationRepository(), 5, 5);
        AuthorizationSummary s = summary(1L, 2, 1, "100.00", "50.00");

        service.adjustSummaryCounters(s, detail("D1", TODAY, "00", "999.00", "40.00"));

        assertEquals(1, s.getApprovedAuthCnt());
        assertEquals(0, new BigDecimal("60.00").compareTo(s.getApprovedAuthAmt()));
        // Declined counters untouched.
        assertEquals(1, s.getDeclinedAuthCnt());
        assertEquals(0, new BigDecimal("50.00").compareTo(s.getDeclinedAuthAmt()));
    }

    @Test
    void deletingDeclinedAuthDecrementsDeclinedCounters() {
        AuthorizationPurgeService service =
                new AuthorizationPurgeService(new InMemoryAuthorizationRepository(), 5, 5);
        AuthorizationSummary s = summary(1L, 2, 1, "100.00", "50.00");

        service.adjustSummaryCounters(s, detail("D1", TODAY, "05", "20.00", "0.00"));

        assertEquals(0, s.getDeclinedAuthCnt());
        assertEquals(0, new BigDecimal("30.00").compareTo(s.getDeclinedAuthAmt()));
        // Approved counters untouched.
        assertEquals(2, s.getApprovedAuthCnt());
        assertEquals(0, new BigDecimal("100.00").compareTo(s.getApprovedAuthAmt()));
    }

    // --- Summary deletion ---------------------------------------------------

    @Test
    void summaryWithAllAuthsPurgedIsDeleted() {
        InMemoryAuthorizationRepository repo = new InMemoryAuthorizationRepository();
        AuthorizationSummary s = summary(1L, 1, 0, "40.00", "0.00");
        repo.summaries.add(s);
        repo.details.put(1L, List.of(detail("D1", TODAY.minusDays(10), "00", "40.00", "40.00")));

        PurgeResult result = new AuthorizationPurgeService(repo, 5, 5).purge(TODAY);

        assertEquals(List.of("D1"), repo.deletedDetailIds);
        assertEquals(List.of(1L), repo.deletedSummaryIds);
        assertFalse(repo.updatedSummaryIds.contains(1L), "deleted summary should not also be updated");
        assertEquals(1, result.summariesDeleted());
        assertEquals(1, result.detailsDeleted());
    }

    @Test
    void summaryWithRemainingAuthsIsUpdatedNotDeleted() {
        InMemoryAuthorizationRepository repo = new InMemoryAuthorizationRepository();
        AuthorizationSummary s = summary(2L, 2, 0, "100.00", "0.00");
        repo.summaries.add(s);
        repo.details.put(2L, List.of(
                detail("D1", TODAY.minusDays(10), "00", "40.00", "40.00"), // expired
                detail("D2", TODAY.minusDays(1), "00", "60.00", "60.00")));  // fresh

        PurgeResult result = new AuthorizationPurgeService(repo, 5, 5).purge(TODAY);

        assertEquals(List.of("D1"), repo.deletedDetailIds);
        assertTrue(repo.deletedSummaryIds.isEmpty());
        assertEquals(List.of(2L), repo.updatedSummaryIds);
        assertEquals(1, s.getApprovedAuthCnt());
        assertEquals(0, new BigDecimal("60.00").compareTo(s.getApprovedAuthAmt()));
        assertEquals(0, result.summariesDeleted());
    }

    @Test
    void freshAuthsLeaveSummaryUntouched() {
        InMemoryAuthorizationRepository repo = new InMemoryAuthorizationRepository();
        AuthorizationSummary s = summary(3L, 1, 1, "40.00", "10.00");
        repo.summaries.add(s);
        repo.details.put(3L, List.of(detail("D1", TODAY.minusDays(1), "00", "40.00", "40.00")));

        new AuthorizationPurgeService(repo, 5, 5).purge(TODAY);

        assertTrue(repo.deletedDetailIds.isEmpty());
        assertTrue(repo.deletedSummaryIds.isEmpty());
        assertTrue(repo.updatedSummaryIds.isEmpty());
    }

    // --- Default parameter handling ----------------------------------------

    @Test
    void nonNumericExpiryDaysDefaultsToFive() {
        assertEquals(5, AuthorizationPurgeService.resolveExpiryDays("ABC"));
        assertEquals(5, AuthorizationPurgeService.resolveExpiryDays(null));
        assertEquals(5, AuthorizationPurgeService.resolveExpiryDays("  "));
        // A valid numeric value is honoured.
        assertEquals(7, AuthorizationPurgeService.resolveExpiryDays("07"));
    }

    @Test
    void checkpointFrequencyDefaultsAndIsRespected() {
        assertEquals(5, AuthorizationPurgeService.resolveCheckpointFrequency("xx"));

        InMemoryAuthorizationRepository repo = new InMemoryAuthorizationRepository();
        // 3 summaries, checkpointFrequency 1 -> checkpoint after each, plus final.
        for (long i = 1; i <= 3; i++) {
            repo.summaries.add(summary(i, 1, 0, "10.00", "0.00"));
            repo.details.put(i, List.of(detail("D" + i, TODAY.minusDays(1), "00", "10.00", "10.00")));
        }

        PurgeResult result = new AuthorizationPurgeService(repo, 5, 1).purge(TODAY);

        assertEquals(3, result.summariesRead());
        assertTrue(result.checkpoints() >= 1, "at least the final checkpoint is taken");
        assertEquals(repo.checkpoints, result.checkpoints());
    }
}
