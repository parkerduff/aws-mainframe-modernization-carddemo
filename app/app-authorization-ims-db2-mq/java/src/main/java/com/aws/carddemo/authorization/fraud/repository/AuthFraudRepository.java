package com.aws.carddemo.authorization.fraud.repository;

import com.aws.carddemo.authorization.fraud.entity.AuthFraud;
import com.aws.carddemo.authorization.fraud.entity.AuthFraudId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the DB2 {@code AUTHFRDS} table, replacing the embedded SQL
 * INSERT/UPDATE in COPAUS2C. The service relies on the inherited {@code findById(AuthFraudId)}
 * (upsert lookup, mirroring the SQLCODE -803 duplicate-key branch) and {@code save(AuthFraud)}.
 *
 * <p>FROZEN CONTRACT (Phase 0): the {@code JpaRepository<AuthFraud, AuthFraudId>} base type is
 * stable. Sub-session 1 owns the entity mapping and may add derived query methods here, but
 * must not change the base generics.
 */
@Repository
public interface AuthFraudRepository extends JpaRepository<AuthFraud, AuthFraudId> {
}
