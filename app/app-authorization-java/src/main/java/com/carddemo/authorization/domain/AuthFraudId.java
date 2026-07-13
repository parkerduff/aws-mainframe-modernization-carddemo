package com.carddemo.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Composite primary key for {@link AuthFraud} — {@code (CARD_NUM, AUTH_TS)}
 * (BR-04.2), matching the DB2 {@code AUTHFRDS} primary key.
 */
@Embeddable
public class AuthFraudId implements Serializable {

    @Column(name = "CARD_NUM", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "AUTH_TS", nullable = false)
    private LocalDateTime authTs;

    protected AuthFraudId() {
    }

    public AuthFraudId(String cardNum, LocalDateTime authTs) {
        this.cardNum = cardNum;
        this.authTs = authTs;
    }

    public String getCardNum() {
        return cardNum;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
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

