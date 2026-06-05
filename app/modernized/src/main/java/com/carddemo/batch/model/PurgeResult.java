package com.carddemo.batch.model;

/**
 * Run statistics returned by
 * {@link com.carddemo.batch.service.AuthorizationPurgeService}, mirroring the
 * end-of-job counters displayed by {@code CBPAUP0C.cbl} (lines 173-176) plus the
 * number of checkpoints taken.
 */
public record PurgeResult(
        int summariesRead,
        int summariesDeleted,
        int detailsRead,
        int detailsDeleted,
        int checkpoints
) {
}
