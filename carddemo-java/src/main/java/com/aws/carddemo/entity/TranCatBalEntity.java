package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction category balance entity, migrated from COBOL copybook
 * {@code app/cpy/CVTRA01Y.cpy} (TRAN-CAT-BAL-RECORD).
 *
 * <pre>
 *   05 TRAN-CAT-KEY.                          -> id (composite)
 *   05 TRAN-CAT-BAL PIC S9(09)V99.            -> balance
 * </pre>
 *
 * Maintained by the batch poster (CBTRN02C 2700-UPDATE-TCATBAL) which accumulates
 * posted amounts per (account, transaction type, transaction category).
 */
@Entity
@Table(name = "tran_cat_balance")
@Getter
@Setter
@NoArgsConstructor
public class TranCatBalEntity {

    @EmbeddedId
    private TranCatBalId id;

    @Column(name = "balance", precision = 11, scale = 2)
    private BigDecimal balance;

    public TranCatBalEntity(TranCatBalId id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }
}
