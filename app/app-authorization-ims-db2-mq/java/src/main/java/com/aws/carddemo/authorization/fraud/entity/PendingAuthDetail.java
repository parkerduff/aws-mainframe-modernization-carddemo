package com.aws.carddemo.authorization.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relational stand-in for the IMS PAUTDTL1 (pending authorization details) segment,
 * described by copybook CIPAUDTY. In the COBOL flow (COPAUS1C) this record lives in the
 * IMS hierarchical database; here it is a provider-agnostic JPA entity.
 *
 * <p>The {@code fraudFlag} column corresponds to PA-AUTH-FRAUD ('F' confirmed / 'R' removed)
 * and {@code fraudRptDate} to PA-FRAUD-RPT-DATE. The remaining columns carry the full
 * authorization detail needed to build an {@link AuthFraud} row on the DB2 side.
 *
 * <p>Part of the Phase 0 foundation. The service (Sub-session 3) reads and updates this
 * entity via {@code PendingAuthDetailRepository}.
 */
@Entity
@Table(name = "PENDING_AUTH_DETAIL")
public class PendingAuthDetail {

    @Id
    @Column(name = "AUTH_KEY", length = 64, nullable = false)
    private String authKey;

    @Column(name = "CARD_NUM", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "AUTH_TS", nullable = false)
    private LocalDateTime authTs;

    @Column(name = "AUTH_TYPE", length = 4)
    private String authType;

    @Column(name = "CARD_EXPIRY_DATE", length = 4)
    private String cardExpiryDate;

    @Column(name = "MESSAGE_TYPE", length = 6)
    private String messageType;

    @Column(name = "MESSAGE_SOURCE", length = 6)
    private String messageSource;

    @Column(name = "AUTH_ID_CODE", length = 6)
    private String authIdCode;

    @Column(name = "AUTH_RESP_CODE", length = 2)
    private String authRespCode;

    @Column(name = "AUTH_RESP_REASON", length = 4)
    private String authRespReason;

    @Column(name = "PROCESSING_CODE", length = 6)
    private String processingCode;

    @Column(name = "TRANSACTION_AMT", precision = 12, scale = 2)
    private BigDecimal transactionAmt;

    @Column(name = "APPROVED_AMT", precision = 12, scale = 2)
    private BigDecimal approvedAmt;

    @Column(name = "MERCHANT_CATAGORY_CODE", length = 4)
    private String merchantCategoryCode;

    @Column(name = "ACQR_COUNTRY_CODE", length = 3)
    private String acqrCountryCode;

    @Column(name = "POS_ENTRY_MODE")
    private Integer posEntryMode;

    @Column(name = "MERCHANT_ID", length = 15)
    private String merchantId;

    @Column(name = "MERCHANT_NAME", length = 22)
    private String merchantName;

    @Column(name = "MERCHANT_CITY", length = 13)
    private String merchantCity;

    @Column(name = "MERCHANT_STATE", length = 2)
    private String merchantState;

    @Column(name = "MERCHANT_ZIP", length = 9)
    private String merchantZip;

    @Column(name = "TRANSACTION_ID", length = 15)
    private String transactionId;

    @Column(name = "MATCH_STATUS", length = 1)
    private String matchStatus;

    @Column(name = "ACCT_ID")
    private Long acctId;

    @Column(name = "CUST_ID")
    private Long custId;

    @Column(name = "AUTH_FRAUD", length = 1)
    private String fraudFlag;

    @Column(name = "FRAUD_RPT_DATE")
    private LocalDate fraudRptDate;

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
    }

    public void setAuthTs(LocalDateTime authTs) {
        this.authTs = authTs;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getCardExpiryDate() {
        return cardExpiryDate;
    }

    public void setCardExpiryDate(String cardExpiryDate) {
        this.cardExpiryDate = cardExpiryDate;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(String messageSource) {
        this.messageSource = messageSource;
    }

    public String getAuthIdCode() {
        return authIdCode;
    }

    public void setAuthIdCode(String authIdCode) {
        this.authIdCode = authIdCode;
    }

    public String getAuthRespCode() {
        return authRespCode;
    }

    public void setAuthRespCode(String authRespCode) {
        this.authRespCode = authRespCode;
    }

    public String getAuthRespReason() {
        return authRespReason;
    }

    public void setAuthRespReason(String authRespReason) {
        this.authRespReason = authRespReason;
    }

    public String getProcessingCode() {
        return processingCode;
    }

    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }

    public BigDecimal getTransactionAmt() {
        return transactionAmt;
    }

    public void setTransactionAmt(BigDecimal transactionAmt) {
        this.transactionAmt = transactionAmt;
    }

    public BigDecimal getApprovedAmt() {
        return approvedAmt;
    }

    public void setApprovedAmt(BigDecimal approvedAmt) {
        this.approvedAmt = approvedAmt;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public void setMerchantCategoryCode(String merchantCategoryCode) {
        this.merchantCategoryCode = merchantCategoryCode;
    }

    public String getAcqrCountryCode() {
        return acqrCountryCode;
    }

    public void setAcqrCountryCode(String acqrCountryCode) {
        this.acqrCountryCode = acqrCountryCode;
    }

    public Integer getPosEntryMode() {
        return posEntryMode;
    }

    public void setPosEntryMode(Integer posEntryMode) {
        this.posEntryMode = posEntryMode;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantCity() {
        return merchantCity;
    }

    public void setMerchantCity(String merchantCity) {
        this.merchantCity = merchantCity;
    }

    public String getMerchantState() {
        return merchantState;
    }

    public void setMerchantState(String merchantState) {
        this.merchantState = merchantState;
    }

    public String getMerchantZip() {
        return merchantZip;
    }

    public void setMerchantZip(String merchantZip) {
        this.merchantZip = merchantZip;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public String getFraudFlag() {
        return fraudFlag;
    }

    public void setFraudFlag(String fraudFlag) {
        this.fraudFlag = fraudFlag;
    }

    public LocalDate getFraudRptDate() {
        return fraudRptDate;
    }

    public void setFraudRptDate(LocalDate fraudRptDate) {
        this.fraudRptDate = fraudRptDate;
    }
}
