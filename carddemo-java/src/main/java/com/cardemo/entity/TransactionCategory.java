package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Transaction category entity - migrated from COBOL copybook CVTRA04Y.cpy (TRANCATG VSAM file).
 * Maps the TRAN-CAT-RECORD layout defining transaction category codes within types.
 */
@Entity
@Table(name = "transaction_categories")
@IdClass(TransactionCategoryId.class)
public class TransactionCategory {

    @Id
    @Column(name = "type_cd", length = 2)
    @NotBlank
    private String typeCd;

    @Id
    @Column(name = "cat_cd")
    private Integer catCd;

    @Column(name = "cat_desc", length = 50, nullable = false)
    @NotBlank
    private String catDesc;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TransactionCategory() {}

    public String getTypeCd() { return typeCd; }
    public void setTypeCd(String typeCd) { this.typeCd = typeCd; }

    public Integer getCatCd() { return catCd; }
    public void setCatCd(Integer catCd) { this.catCd = catCd; }

    public String getCatDesc() { return catDesc; }
    public void setCatDesc(String catDesc) { this.catDesc = catDesc; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
