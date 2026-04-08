package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Card cross-reference entity - migrated from COBOL copybook CVACT03Y.cpy (CARDXREF VSAM file).
 * Maps the CARD-XREF-RECORD layout linking cards to customers and accounts.
 */
@Entity
@Table(name = "card_xrefs")
public class CardXref {

    @Id
    @Column(name = "card_num", length = 16)
    private String cardNum;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CardXref() {}

    public String getCardNum() { return cardNum; }
    public void setCardNum(String cardNum) { this.cardNum = cardNum; }

    public Long getCustId() { return custId; }
    public void setCustId(Long custId) { this.custId = custId; }

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
