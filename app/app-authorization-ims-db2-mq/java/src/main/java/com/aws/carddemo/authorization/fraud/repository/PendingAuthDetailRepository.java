package com.aws.carddemo.authorization.fraud.repository;

import com.aws.carddemo.authorization.fraud.entity.PendingAuthDetail;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the pending authorization detail (relational stand-in for the IMS PAUTDTL1
 * segment). Mirrors the DLI GU/REPL access in COPAUS1C: the service loads a detail, toggles
 * its fraud flag and persists it back within the same transactional boundary.
 *
 * <p>Part of the Phase 0 foundation (frozen contract).
 */
@Repository
public interface PendingAuthDetailRepository extends JpaRepository<PendingAuthDetail, String> {

    Optional<PendingAuthDetail> findByCardNumAndAuthTs(String cardNum, LocalDateTime authTs);
}
