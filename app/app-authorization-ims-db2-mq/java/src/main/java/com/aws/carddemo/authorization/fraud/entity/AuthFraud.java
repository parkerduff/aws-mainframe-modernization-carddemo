package com.aws.carddemo.authorization.fraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity mapped to the DB2 table {@code CARDDEMO.AUTHFRDS} (see the DDL in the module
 * README). Mirrors the record inserted/updated by COPAUS2C.
 *
 * <p>Phase 0 provides a skeleton with the FROZEN method surface that the service
 * (Sub-session 3) depends on:
 * <ul>
 *   <li>{@link #fromPendingAuthDetail(PendingAuthDetail)} — build an insert-ready row</li>
 *   <li>{@link #getId()} / {@link #setId(AuthFraudId)}</li>
 *   <li>{@link #getAuthFraud()} / {@link #setAuthFraud(String)} — the AUTH_FRAUD flag</li>
 *   <li>{@link #getFraudRptDate()} / {@link #setFraudRptDate(LocalDate)}</li>
 * </ul>
 * Sub-session 1 maps ALL remaining AUTHFRDS columns and implements the factory body.
 */
@Entity
@Table(name = "AUTHFRDS")
public class AuthFraud {

    @EmbeddedId
    private AuthFraudId id;

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

    @Column(name = "AUTH_FRAUD", length = 1)
    private String authFraud;

    @Column(name = "FRAUD_RPT_DATE")
    private LocalDate fraudRptDate;

    @Column(name = "ACCT_ID")
    private Long acctId;

    @Column(name = "CUST_ID")
    private Long custId;

    public AuthFraud() {
    }

    public AuthFraud(AuthFraudId id) {
        this.id = id;
    }

    /**
     * Build a fully-populated (insert-ready) AuthFraud row from a pending auth detail,
     * mirroring the field-by-field MOVEs preceding the INSERT in COPAUS2C.
     *
     * <p>Sub-session 1 implements this by copying every AUTHFRDS column from the detail.
     */
    public static AuthFraud fromPendingAuthDetail(PendingAuthDetail detail) {
        AuthFraud authFraud = new AuthFraud(new AuthFraudId(detail.getCardNum(), detail.getAuthTs()));
        authFraud.setAuthType(detail.getAuthType());
        authFraud.setCardExpiryDate(detail.getCardExpiryDate());
        authFraud.setMessageType(detail.getMessageType());
        authFraud.setMessageSource(detail.getMessageSource());
        authFraud.setAuthIdCode(detail.getAuthIdCode());
        authFraud.setAuthRespCode(detail.getAuthRespCode());
        authFraud.setAuthRespReason(detail.getAuthRespReason());
        authFraud.setProcessingCode(detail.getProcessingCode());
        authFraud.setTransactionAmt(detail.getTransactionAmt());
        authFraud.setApprovedAmt(detail.getApprovedAmt());
        authFraud.setMerchantCategoryCode(detail.getMerchantCategoryCode());
        authFraud.setAcqrCountryCode(detail.getAcqrCountryCode());
        authFraud.setPosEntryMode(detail.getPosEntryMode());
        authFraud.setMerchantId(detail.getMerchantId());
        authFraud.setMerchantName(detail.getMerchantName());
        authFraud.setMerchantCity(detail.getMerchantCity());
        authFraud.setMerchantState(detail.getMerchantState());
        authFraud.setMerchantZip(detail.getMerchantZip());
        authFraud.setTransactionId(detail.getTransactionId());
        authFraud.setMatchStatus(detail.getMatchStatus());
        authFraud.setAcctId(detail.getAcctId());
        authFraud.setCustId(detail.getCustId());
        authFraud.setAuthFraud(detail.getFraudFlag());
        authFraud.setFraudRptDate(detail.getFraudRptDate());
        return authFraud;
    }

    public AuthFraudId getId() {
        return id;
    }

    public void setId(AuthFraudId id) {
        this.id = id;
    }

    public String getAuthFraud() {
        return authFraud;
    }

    public void setAuthFraud(String authFraud) {
        this.authFraud = authFraud;
    }

    public LocalDate getFraudRptDate() {
        return fraudRptDate;
    }

    public void setFraudRptDate(LocalDate fraudRptDate) {
        this.fraudRptDate = fraudRptDate;
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
}
