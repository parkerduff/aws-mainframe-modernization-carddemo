package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Disclosure group entity - migrated from COBOL copybook CVTRA02Y.cpy (DISCGRP VSAM file).
 * Maps the DISC-GRP-RECORD layout defining interest rates per account group/transaction type/category.
 */
@Entity
@Table(name = "disclosure_groups")
@IdClass(DisclosureGroupId.class)
public class DisclosureGroup {

    @Id
    @Column(name = "acct_group_id", length = 10)
    private String acctGroupId;

    @Id
    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    @Id
    @Column(name = "tran_cat_cd")
    private Integer tranCatCd;

    @Column(name = "int_rate", precision = 6, scale = 2)
    private BigDecimal intRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DisclosureGroup() {}

    public String getAcctGroupId() { return acctGroupId; }
    public void setAcctGroupId(String acctGroupId) { this.acctGroupId = acctGroupId; }

    public String getTranTypeCd() { return tranTypeCd; }
    public void setTranTypeCd(String tranTypeCd) { this.tranTypeCd = tranTypeCd; }

    public Integer getTranCatCd() { return tranCatCd; }
    public void setTranCatCd(Integer tranCatCd) { this.tranCatCd = tranCatCd; }

    public BigDecimal getIntRate() { return intRate; }
    public void setIntRate(BigDecimal intRate) { this.intRate = intRate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
