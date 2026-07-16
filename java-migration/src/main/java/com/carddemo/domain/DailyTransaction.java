package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Daily transaction staging record read by the posting engine (CBTRN02C).
 * Mirrors copybook CVTRA06Y (DALYTRAN-RECORD, RECLN 350).
 * <p>
 * Uses a surrogate primary key because a daily file may repeat a DALYTRAN-ID
 * across runs; DALYTRAN-AMT (PIC S9(09)V99) maps to BigDecimal with scale 2.
 */
@Entity
@Table(name = "DAILY_TRANSACTION")
public class DailyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DALYTRAN_ID")
    private String dalytranId; // PIC X(16)

    @Column(name = "DALYTRAN_TYPE_CD")
    private String dalytranTypeCd; // PIC X(02)

    @Column(name = "DALYTRAN_CAT_CD")
    private Integer dalytranCatCd; // PIC 9(04)

    @Column(name = "DALYTRAN_SOURCE")
    private String dalytranSource; // PIC X(10)

    @Column(name = "DALYTRAN_DESC")
    private String dalytranDesc; // PIC X(100)

    @Column(name = "DALYTRAN_AMT")
    private BigDecimal dalytranAmt; // PIC S9(09)V99

    @Column(name = "DALYTRAN_MERCHANT_ID")
    private Long dalytranMerchantId; // PIC 9(09)

    @Column(name = "DALYTRAN_MERCHANT_NAME")
    private String dalytranMerchantName; // PIC X(50)

    @Column(name = "DALYTRAN_MERCHANT_CITY")
    private String dalytranMerchantCity; // PIC X(50)

    @Column(name = "DALYTRAN_MERCHANT_ZIP")
    private String dalytranMerchantZip; // PIC X(10)

    @Column(name = "DALYTRAN_CARD_NUM")
    private String dalytranCardNum; // PIC X(16)

    @Column(name = "DALYTRAN_ORIG_TS")
    private String dalytranOrigTs; // PIC X(26)

    @Column(name = "DALYTRAN_PROC_TS")
    private String dalytranProcTs; // PIC X(26)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDalytranId() {
        return dalytranId;
    }

    public void setDalytranId(String dalytranId) {
        this.dalytranId = dalytranId;
    }

    public String getDalytranTypeCd() {
        return dalytranTypeCd;
    }

    public void setDalytranTypeCd(String dalytranTypeCd) {
        this.dalytranTypeCd = dalytranTypeCd;
    }

    public Integer getDalytranCatCd() {
        return dalytranCatCd;
    }

    public void setDalytranCatCd(Integer dalytranCatCd) {
        this.dalytranCatCd = dalytranCatCd;
    }

    public String getDalytranSource() {
        return dalytranSource;
    }

    public void setDalytranSource(String dalytranSource) {
        this.dalytranSource = dalytranSource;
    }

    public String getDalytranDesc() {
        return dalytranDesc;
    }

    public void setDalytranDesc(String dalytranDesc) {
        this.dalytranDesc = dalytranDesc;
    }

    public BigDecimal getDalytranAmt() {
        return dalytranAmt;
    }

    public void setDalytranAmt(BigDecimal dalytranAmt) {
        this.dalytranAmt = dalytranAmt;
    }

    public Long getDalytranMerchantId() {
        return dalytranMerchantId;
    }

    public void setDalytranMerchantId(Long dalytranMerchantId) {
        this.dalytranMerchantId = dalytranMerchantId;
    }

    public String getDalytranMerchantName() {
        return dalytranMerchantName;
    }

    public void setDalytranMerchantName(String dalytranMerchantName) {
        this.dalytranMerchantName = dalytranMerchantName;
    }

    public String getDalytranMerchantCity() {
        return dalytranMerchantCity;
    }

    public void setDalytranMerchantCity(String dalytranMerchantCity) {
        this.dalytranMerchantCity = dalytranMerchantCity;
    }

    public String getDalytranMerchantZip() {
        return dalytranMerchantZip;
    }

    public void setDalytranMerchantZip(String dalytranMerchantZip) {
        this.dalytranMerchantZip = dalytranMerchantZip;
    }

    public String getDalytranCardNum() {
        return dalytranCardNum;
    }

    public void setDalytranCardNum(String dalytranCardNum) {
        this.dalytranCardNum = dalytranCardNum;
    }

    public String getDalytranOrigTs() {
        return dalytranOrigTs;
    }

    public void setDalytranOrigTs(String dalytranOrigTs) {
        this.dalytranOrigTs = dalytranOrigTs;
    }

    public String getDalytranProcTs() {
        return dalytranProcTs;
    }

    public void setDalytranProcTs(String dalytranProcTs) {
        this.dalytranProcTs = dalytranProcTs;
    }
}
