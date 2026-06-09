package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Composite key for {@link TranCatBalEntity}, migrated from the COBOL group
 * {@code TRAN-CAT-KEY} in {@code app/cpy/CVTRA01Y.cpy}:
 *
 * <pre>
 *   10 TRANCAT-ACCT-ID PIC 9(11). -> acctId
 *   10 TRANCAT-TYPE-CD PIC X(02). -> typeCd
 *   10 TRANCAT-CD      PIC 9(04). -> catCd
 * </pre>
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TranCatBalId implements Serializable {

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "type_cd", length = 2, nullable = false)
    private String typeCd;

    @Column(name = "cat_cd", nullable = false)
    private Integer catCd;

    public TranCatBalId(Long acctId, String typeCd, Integer catCd) {
        this.acctId = acctId;
        this.typeCd = typeCd;
        this.catCd = catCd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranCatBalId that)) {
            return false;
        }
        return Objects.equals(acctId, that.acctId)
                && Objects.equals(typeCd, that.typeCd)
                && Objects.equals(catCd, that.catCd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acctId, typeCd, catCd);
    }
}
