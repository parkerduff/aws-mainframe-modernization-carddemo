package com.carddemo.authorization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pending authorization detail — child record (BR-07, BR-08).
 *
 * <p>Replaces IMS segment {@code PAUTDTL1} / copybook {@code CIPAUDTY}. The legacy
 * IMS child key {@code PA-AUTHORIZATION-KEY} stores complemented date/time values so
 * that segments read most-recent-first; here that ordering is expressed directly by
 * {@link #authTs} descending, and a surrogate id is used as the stable selector for
 * the REST layer.
 */
@Entity
@Table(name = "PENDING_AUTH_DETAIL",
        indexes = @Index(name = "IX_PAUTDTL_ACCT_TS", columnList = "ACCT_ID, AUTH_TS DESC"))
public class PendingAuthDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ACCT_ID", nullable = false)
    private PendingAuthSummary summary;

    /** Authorization timestamp — drives ordering and the {@code AUTHFRDS.AUTH_TS} key. */
    @Column(name = "AUTH_TS", nullable = false)
    private LocalDateTime authTs;

    /** {@code PA-AUTH-ORIG-DATE} (YYMMDD). */
    @Column(name = "AUTH_ORIG_DATE", length = 6)
    private String authOrigDate;

    /** {@code PA-AUTH-ORIG-TIME} (HHMMSS). */
    @Column(name = "AUTH_ORIG_TIME", length = 6)
    private String authOrigTime;

    @Column(name = "CARD_NUM", length = 16, nullable = false)
    private String cardNum;

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
    private BigDecimal transactionAmt = BigDecimal.ZERO;

    @Column(name = "APPROVED_AMT", precision = 12, scale = 2)
    private BigDecimal approvedAmt = BigDecimal.ZERO;

    @Column(name = "MERCHANT_CATEGORY_CODE", length = 4)
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

    /** {@code PA-MATCH-STATUS}. */
    @Column(name = "MATCH_STATUS", length = 1)
    private String matchStatus;

    /** {@code PA-AUTH-FRAUD} — fraud-confirmed flag ({@link FraudFlag}). */
    @Column(name = "AUTH_FRAUD", length = 1)
    private String authFraud = FraudFlag.NONE;

    /** {@code PA-FRAUD-RPT-DATE}. */
    @Column(name = "FRAUD_RPT_DATE", length = 10)
    private String fraudRptDate;

    public boolean isFraudConfirmed() {
        return FraudFlag.isConfirmed(authFraud);
    }

    public boolean isApproved() {
        return AuthResponseCode.APPROVED.equals(authRespCode);
    }

    public Long getId() {
        return id;
    }

    public PendingAuthSummary getSummary() {
        return summary;
    }

    public void setSummary(PendingAuthSummary summary) {
        this.summary = summary;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
    }

    public void setAuthTs(LocalDateTime authTs) {
        this.authTs = authTs;
    }

    public String getAuthOrigDate() {
        return authOrigDate;
    }

    public void setAuthOrigDate(String authOrigDate) {
        this.authOrigDate = authOrigDate;
    }

    public String getAuthOrigTime() {
        return authOrigTime;
    }

    public void setAuthOrigTime(String authOrigTime) {
        this.authOrigTime = authOrigTime;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
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

    public String getAuthFraud() {
        return authFraud;
    }

    public void setAuthFraud(String authFraud) {
        this.authFraud = authFraud;
    }

    public String getFraudRptDate() {
        return fraudRptDate;
    }

    public void setFraudRptDate(String fraudRptDate) {
        this.fraudRptDate = fraudRptDate;
    }
}

