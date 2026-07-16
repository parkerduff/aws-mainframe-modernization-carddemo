package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Posted transaction record. Mirrors copybook CVTRA05Y (TRAN-RECORD, RECLN 350).
 * TRAN-AMT (PIC S9(09)V99) maps to BigDecimal with scale 2. Timestamps are kept
 * as the raw 26-char COBOL format; use {@code com.carddemo.util.CobolTimestamp}
 * to convert to/from {@link java.time.LocalDateTime}.
 */
@Entity
@Table(name = "TRANSACTION")
public class Transaction {

    @Id
    @Column(name = "TRAN_ID")
    private String tranId; // PIC X(16)

    @Column(name = "TRAN_TYPE_CD")
    private String tranTypeCd; // PIC X(02)

    @Column(name = "TRAN_CAT_CD")
    private Integer tranCatCd; // PIC 9(04)

    @Column(name = "TRAN_SOURCE")
    private String tranSource; // PIC X(10)

    @Column(name = "TRAN_DESC")
    private String tranDesc; // PIC X(100)

    @Column(name = "TRAN_AMT")
    private BigDecimal tranAmt; // PIC S9(09)V99

    @Column(name = "TRAN_MERCHANT_ID")
    private Long tranMerchantId; // PIC 9(09)

    @Column(name = "TRAN_MERCHANT_NAME")
    private String tranMerchantName; // PIC X(50)

    @Column(name = "TRAN_MERCHANT_CITY")
    private String tranMerchantCity; // PIC X(50)

    @Column(name = "TRAN_MERCHANT_ZIP")
    private String tranMerchantZip; // PIC X(10)

    @Column(name = "TRAN_CARD_NUM")
    private String tranCardNum; // PIC X(16)

    @Column(name = "TRAN_ORIG_TS")
    private String tranOrigTs; // PIC X(26)

    @Column(name = "TRAN_PROC_TS")
    private String tranProcTs; // PIC X(26)

    public String getTranId() {
        return tranId;
    }

    public void setTranId(String tranId) {
        this.tranId = tranId;
    }

    public String getTranTypeCd() {
        return tranTypeCd;
    }

    public void setTranTypeCd(String tranTypeCd) {
        this.tranTypeCd = tranTypeCd;
    }

    public Integer getTranCatCd() {
        return tranCatCd;
    }

    public void setTranCatCd(Integer tranCatCd) {
        this.tranCatCd = tranCatCd;
    }

    public String getTranSource() {
        return tranSource;
    }

    public void setTranSource(String tranSource) {
        this.tranSource = tranSource;
    }

    public String getTranDesc() {
        return tranDesc;
    }

    public void setTranDesc(String tranDesc) {
        this.tranDesc = tranDesc;
    }

    public BigDecimal getTranAmt() {
        return tranAmt;
    }

    public void setTranAmt(BigDecimal tranAmt) {
        this.tranAmt = tranAmt;
    }

    public Long getTranMerchantId() {
        return tranMerchantId;
    }

    public void setTranMerchantId(Long tranMerchantId) {
        this.tranMerchantId = tranMerchantId;
    }

    public String getTranMerchantName() {
        return tranMerchantName;
    }

    public void setTranMerchantName(String tranMerchantName) {
        this.tranMerchantName = tranMerchantName;
    }

    public String getTranMerchantCity() {
        return tranMerchantCity;
    }

    public void setTranMerchantCity(String tranMerchantCity) {
        this.tranMerchantCity = tranMerchantCity;
    }

    public String getTranMerchantZip() {
        return tranMerchantZip;
    }

    public void setTranMerchantZip(String tranMerchantZip) {
        this.tranMerchantZip = tranMerchantZip;
    }

    public String getTranCardNum() {
        return tranCardNum;
    }

    public void setTranCardNum(String tranCardNum) {
        this.tranCardNum = tranCardNum;
    }

    public String getTranOrigTs() {
        return tranOrigTs;
    }

    public void setTranOrigTs(String tranOrigTs) {
        this.tranOrigTs = tranOrigTs;
    }

    public String getTranProcTs() {
        return tranProcTs;
    }

    public void setTranProcTs(String tranProcTs) {
        this.tranProcTs = tranProcTs;
    }
}
