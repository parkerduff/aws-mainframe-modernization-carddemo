package com.aws.carddemo.authorization.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for {@link AuthFraud}, mirroring the AUTHFRDS primary key
 * {@code PRIMARY KEY(CARD_NUM, AUTH_TS)} defined in the module README DDL.
 *
 * <p>FROZEN CONTRACT (Phase 0): the {@code cardNum}/{@code authTs} fields, the
 * {@code (String, LocalDateTime)} constructor and the accessors are stable — the
 * service and repository depend on them. Sub-session 1 refines the JPA column mapping.
 */
@Embeddable
public class AuthFraudId implements Serializable {

    @Column(name = "CARD_NUM", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "AUTH_TS", nullable = false)
    private LocalDateTime authTs;

    public AuthFraudId() {
    }

    public AuthFraudId(String cardNum, LocalDateTime authTs) {
        this.cardNum = cardNum;
        this.authTs = authTs;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
    }

    public void setAuthTs(LocalDateTime authTs) {
        this.authTs = authTs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthFraudId that)) {
            return false;
        }
        return Objects.equals(cardNum, that.cardNum) && Objects.equals(authTs, that.authTs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNum, authTs);
    }
}
