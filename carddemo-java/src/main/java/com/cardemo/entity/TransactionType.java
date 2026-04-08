package com.cardemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Transaction type entity - migrated from COBOL copybook CVTRA03Y.cpy (TRANTYPE VSAM file).
 * Maps the TRAN-TYPE-RECORD layout defining transaction type codes.
 */
@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "type_cd", length = 2)
    @NotBlank
    private String typeCd;

    @Column(name = "type_desc", length = 50, nullable = false)
    @NotBlank
    private String typeDesc;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TransactionType() {}

    public String getTypeCd() { return typeCd; }
    public void setTypeCd(String typeCd) { this.typeCd = typeCd; }

    public String getTypeDesc() { return typeDesc; }
    public void setTypeDesc(String typeDesc) { this.typeDesc = typeDesc; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
