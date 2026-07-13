package com.carddemo.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fraud record — maps DB2 table {@code CARDDEMO.AUTHFRDS} exactly (BR-04, BR-10.3).
 *
 * <p>Columns and types follow {@code ddl/AUTHFRDS.ddl} / {@code dcl/AUTHFRDS.dcl}.
 * The primary key is the composite {@code (CARD_NUM, AUTH_TS)} and the unique index
 * {@code XAUTHFRD} is declared over {@code (CARD_NUM ASC, AUTH_TS DESC)}.
 */
@Entity
@Table(name = "AUTHFRDS",
        indexes = @Index(name = "XAUTHFRD", columnList = "CARD_NUM ASC, AUTH_TS DESC", unique = true))
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
    private Short posEntryMode;

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

    @Column(name = "ACCT_ID", precision = 11)
    private Long acctId;

    @Column(name = "CUST_ID", precision = 9)
    private Long custId;

    protected AuthFraud() {
    }

    public AuthFraud(String cardNum, LocalDateTime authTs) {
        this.id = new AuthFraudId(cardNum, authTs);
    }

    public AuthFraudId getId() {
        return id;
    }

    public String getCardNum() {
        return id.getCardNum();
    }

    public LocalDateTime getAuthTs() {
        return id.getAuthTs();
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

    public Short getPosEntryMode() {
        return posEntryMode;
    }

    public void setPosEntryMode(Short posEntryMode) {
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

