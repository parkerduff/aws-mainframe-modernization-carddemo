package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Card entity - migrated from COBOL copybook CVACT02Y.cpy (CARDDATA VSAM file).
 * Maps the CARD-RECORD layout containing credit card details.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_num", length = 16)
    @NotBlank
    @Size(max = 16)
    private String cardNum;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "cvv_cd", length = 3, nullable = false)
    @NotBlank
    @Size(max = 3)
    private String cvvCd;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus = "Y";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Card() {}

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }

    public String getCvvCd() { return cvvCd; }
    public void setCvvCd(String cvvCd) { this.cvvCd = cvvCd; }

    public String getEmbossedName() { return embossedName; }
    public void setEmbossedName(String embossedName) { this.embossedName = embossedName; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return "Y".equals(activeStatus);
    }
}
