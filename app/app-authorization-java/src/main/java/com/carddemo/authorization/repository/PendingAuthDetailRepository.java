package com.carddemo.authorization.repository;

import com.carddemo.authorization.domain.PendingAuthDetail;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link PendingAuthDetail} — replaces IMS child-segment access
 * ({@code GNP/ISRT/REPL/DLET SEGMENT(PAUTDTL1)}).
 */
public interface PendingAuthDetailRepository extends JpaRepository<PendingAuthDetail, Long> {

    /** List an account's authorizations most-recent-first, paginated (BR-08.1). */
    Page<PendingAuthDetail> findBySummaryAcctIdOrderByAuthTsDesc(Long acctId, Pageable pageable);

    Optional<PendingAuthDetail> findByCardNumAndAuthTs(String cardNum, LocalDateTime authTs);
}

