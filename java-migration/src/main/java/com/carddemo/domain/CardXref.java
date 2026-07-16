package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Card-to-account cross reference. Mirrors copybook CVACT03Y (CARD-XREF-RECORD, RECLN 50).
 */
@Entity
@Table(name = "CARD_XREF")
public class CardXref {

    @Id
    @Column(name = "XREF_CARD_NUM")
    private String xrefCardNum; // PIC X(16)

    @Column(name = "XREF_CUST_ID")
    private Long xrefCustId; // PIC 9(09)

    @Column(name = "XREF_ACCT_ID")
    private Long xrefAcctId; // PIC 9(11)

    public String getXrefCardNum() {
        return xrefCardNum;
    }

    public void setXrefCardNum(String xrefCardNum) {
        this.xrefCardNum = xrefCardNum;
    }

    public Long getXrefCustId() {
        return xrefCustId;
    }

    public void setXrefCustId(Long xrefCustId) {
        this.xrefCustId = xrefCustId;
    }

    public Long getXrefAcctId() {
        return xrefAcctId;
    }

    public void setXrefAcctId(Long xrefAcctId) {
        this.xrefAcctId = xrefAcctId;
    }
}
