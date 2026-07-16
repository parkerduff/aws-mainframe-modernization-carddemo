package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link TranCatBalance}. Mirrors the TRAN-CAT-KEY group in
 * copybook CVTRA01Y (TRANCAT-ACCT-ID + TRANCAT-TYPE-CD + TRANCAT-CD).
 */
@Embeddable
public class TranCatBalanceId implements Serializable {

    @Column(name = "TRANCAT_ACCT_ID")
    private Long trancatAcctId; // PIC 9(11)

    @Column(name = "TRANCAT_TYPE_CD")
    private String trancatTypeCd; // PIC X(02)

    @Column(name = "TRANCAT_CD")
    private Integer trancatCd; // PIC 9(04)

    public TranCatBalanceId() {
    }

    public TranCatBalanceId(Long trancatAcctId, String trancatTypeCd, Integer trancatCd) {
        this.trancatAcctId = trancatAcctId;
        this.trancatTypeCd = trancatTypeCd;
        this.trancatCd = trancatCd;
    }

    public Long getTrancatAcctId() {
        return trancatAcctId;
    }

    public void setTrancatAcctId(Long trancatAcctId) {
        this.trancatAcctId = trancatAcctId;
    }

    public String getTrancatTypeCd() {
        return trancatTypeCd;
    }

    public void setTrancatTypeCd(String trancatTypeCd) {
        this.trancatTypeCd = trancatTypeCd;
    }

    public Integer getTrancatCd() {
        return trancatCd;
    }

    public void setTrancatCd(Integer trancatCd) {
        this.trancatCd = trancatCd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranCatBalanceId that)) {
            return false;
        }
        return Objects.equals(trancatAcctId, that.trancatAcctId)
                && Objects.equals(trancatTypeCd, that.trancatTypeCd)
                && Objects.equals(trancatCd, that.trancatCd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trancatAcctId, trancatTypeCd, trancatCd);
    }
}
