package com.carddemo.authorization.repository;

import com.carddemo.authorization.domain.AuthFraud;
import com.carddemo.authorization.domain.AuthFraudId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link AuthFraud} — replaces the DB2 {@code AUTHFRDS} access in
 * {@code COPAUS2C} (insert + {@code -803} upsert).
 */
public interface AuthFraudRepository extends JpaRepository<AuthFraud, AuthFraudId> {
}

