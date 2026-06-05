package com.carddemo.batch.repository;

import com.carddemo.batch.model.AuthorizationDetail;
import com.carddemo.batch.model.AuthorizationSummary;

import java.util.List;

/**
 * Data access abstraction for
 * {@link com.carddemo.batch.service.AuthorizationPurgeService}.
 *
 * <p>Hides the IMS DL/I navigation performed by {@code CBPAUP0C.cbl}
 * ({@code GN}/{@code GNP}/{@code DLET}/{@code CHKP}) behind a plain repository so
 * the purge logic can be exercised against any backing store.
 */
public interface AuthorizationRepository {

    /** All summary (root) segments, in iteration order ({@code GN PAUTSUM0}). */
    List<AuthorizationSummary> findAllSummaries();

    /**
     * Detail (child) segments under the given summary, identified by the
     * summary's account id ({@code GNP PAUTDTL1}).
     */
    List<AuthorizationDetail> findDetailsBySummary(long summaryId);

    /** Deletes a detail segment ({@code DLET PAUTDTL1}). */
    void deleteDetail(String detailId);

    /** Deletes a summary segment ({@code DLET PAUTSUM0}). */
    void deleteSummary(long summaryId);

    /** Persists adjusted counters/amounts back to a surviving summary segment. */
    void updateSummary(AuthorizationSummary summary);

    /** Commits work done so far ({@code CHKP}). */
    void checkpoint();
}
