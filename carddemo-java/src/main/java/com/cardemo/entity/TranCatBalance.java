package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction category balance entity - migrated from COBOL copybook CVTRA01Y.cpy (TCATBALF VSAM file).
 * Maps the TCAT-BAL-RECORD layout tracking balances per account/transaction type/category.
 */
@Entity
@Table(name = "tran_cat_balances")
@IdClass(TranCatBalanceId.class)
public class TranCatBalance {

    @Id
    @Column(name = "acct_id")
    private Long acctId;

    @Id
    @Column(name = "type_cd", length = 2)
    private String typeCd;

    @Id
    @Column(name = "cat_cd")
    private Integer catCd;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TranCatBalance() {}

    public Long getAcctId() { return acctId; }
    public void setAcctId(Long acctId) { this.acctId = acctId; }

    public String getTypeCd() { return typeCd; }
    public void setTypeCd(String typeCd) { this.typeCd = typeCd; }

    public Integer getCatCd() { return catCd; }
    public void setCatCd(Integer catCd) { this.catCd = catCd; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
