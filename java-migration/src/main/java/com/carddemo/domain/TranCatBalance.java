package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Transaction category balance. Mirrors copybook CVTRA01Y (TRAN-CAT-BAL-RECORD, RECLN 50).
 * Balance (PIC S9(09)V99) maps to BigDecimal with scale 2.
 */
@Entity
@Table(name = "TRAN_CAT_BALANCE")
public class TranCatBalance {

    @EmbeddedId
    private TranCatBalanceId id;

    @Column(name = "TRAN_CAT_BAL")
    private BigDecimal tranCatBal; // PIC S9(09)V99

    public TranCatBalanceId getId() {
        return id;
    }

    public void setId(TranCatBalanceId id) {
        this.id = id;
    }

    public BigDecimal getTranCatBal() {
        return tranCatBal;
    }

    public void setTranCatBal(BigDecimal tranCatBal) {
        this.tranCatBal = tranCatBal;
    }
}
