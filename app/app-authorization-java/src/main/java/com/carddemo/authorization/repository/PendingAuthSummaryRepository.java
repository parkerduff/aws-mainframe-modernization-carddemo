package com.carddemo.authorization.repository;

import com.carddemo.authorization.domain.PendingAuthSummary;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link PendingAuthSummary} — replaces IMS root-segment access
 * ({@code GU/ISRT/REPL/DLET SEGMENT(PAUTSUM0)}).
 */
public interface PendingAuthSummaryRepository extends JpaRepository<PendingAuthSummary, Long> {
}

