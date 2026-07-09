package com.aws.carddemo.authorization.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * JPA entity mapped to the DB2 table {@code CARDDEMO.AUTHFRDS} (see the DDL in the module
 * README). Mirrors the record inserted/updated by COPAUS2C.
 *
 * <p>Phase 0 provides a skeleton with the FROZEN method surface that the service
 * (Sub-session 3) depends on:
 * <ul>
 *   <li>{@link #fromPendingAuthDetail(PendingAuthDetail)} — build an insert-ready row</li>
 *   <li>{@link #getId()} / {@link #setId(AuthFraudId)}</li>
 *   <li>{@link #getAuthFraud()} / {@link #setAuthFraud(String)} — the AUTH_FRAUD flag</li>
 *   <li>{@link #getFraudRptDate()} / {@link #setFraudRptDate(LocalDate)}</li>
 * </ul>
 * Sub-session 1 maps ALL remaining AUTHFRDS columns and implements the factory body.
 */
@Entity
@Table(name = "AUTHFRDS")
public class AuthFraud {

    @EmbeddedId
    private AuthFraudId id;

    @Column(name = "AUTH_FRAUD", length = 1)
    private String authFraud;

    @Column(name = "FRAUD_RPT_DATE")
    private LocalDate fraudRptDate;

    public AuthFraud() {
    }

    public AuthFraud(AuthFraudId id) {
        this.id = id;
    }

    /**
     * Build a fully-populated (insert-ready) AuthFraud row from a pending auth detail,
     * mirroring the field-by-field MOVEs preceding the INSERT in COPAUS2C.
     *
     * <p>Sub-session 1 implements this by copying every AUTHFRDS column from the detail.
     */
    public static AuthFraud fromPendingAuthDetail(PendingAuthDetail detail) {
        throw new UnsupportedOperationException(
                "fromPendingAuthDetail() to be implemented by Sub-session 1 (DB2 persistence layer)");
    }

    public AuthFraudId getId() {
        return id;
    }

    public void setId(AuthFraudId id) {
        this.id = id;
    }

    public String getAuthFraud() {
        return authFraud;
    }

    public void setAuthFraud(String authFraud) {
        this.authFraud = authFraud;
    }

    public LocalDate getFraudRptDate() {
        return fraudRptDate;
    }

    public void setFraudRptDate(LocalDate fraudRptDate) {
        this.fraudRptDate = fraudRptDate;
    }
}
