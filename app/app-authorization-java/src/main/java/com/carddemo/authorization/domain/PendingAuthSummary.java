package com.carddemo.authorization.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pending authorization summary — root record (BR-07, BR-08).
 *
 * <p>Replaces IMS segment {@code PAUTSUM0} / copybook {@code CIPAUSMY}. The account
 * id is the natural key (the legacy IMS root key {@code ACCNTID}). Holds the
 * per-account credit fields and running approved/declined counters maintained by
 * {@code COPAUA0C} paragraph {@code 8400-UPDATE-SUMMARY}.
 */
@Entity
@Table(name = "PENDING_AUTH_SUMMARY")
public class PendingAuthSummary {

    /** {@code PA-ACCT-ID} — account id, IMS root key. */
    @Id
    @Column(name = "ACCT_ID")
    private Long acctId;

    /** {@code PA-CUST-ID}. */
    @Column(name = "CUST_ID")
    private Long custId;

    /** {@code PA-AUTH-STATUS}. */
    @Column(name = "AUTH_STATUS", length = 1)
    private String authStatus;

    /** {@code PA-CREDIT-LIMIT}. */
    @Column(name = "CREDIT_LIMIT", precision = 11, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    /** {@code PA-CASH-LIMIT}. */
    @Column(name = "CASH_LIMIT", precision = 11, scale = 2)
    private BigDecimal cashLimit = BigDecimal.ZERO;

    /** {@code PA-CREDIT-BALANCE} — held/pending authorization amount. */
    @Column(name = "CREDIT_BALANCE", precision = 11, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;

    /** {@code PA-CASH-BALANCE}. */
    @Column(name = "CASH_BALANCE", precision = 11, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    /** {@code PA-APPROVED-AUTH-CNT}. */
    @Column(name = "APPROVED_AUTH_CNT")
    private int approvedAuthCount;

    /** {@code PA-DECLINED-AUTH-CNT}. */
    @Column(name = "DECLINED_AUTH_CNT")
    private int declinedAuthCount;

    /** {@code PA-APPROVED-AUTH-AMT}. */
    @Column(name = "APPROVED_AUTH_AMT", precision = 11, scale = 2)
    private BigDecimal approvedAuthAmount = BigDecimal.ZERO;

    /** {@code PA-DECLINED-AUTH-AMT}. */
    @Column(name = "DECLINED_AUTH_AMT", precision = 11, scale = 2)
    private BigDecimal declinedAuthAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("authTs DESC")
    private List<PendingAuthDetail> details = new ArrayList<>();

    protected PendingAuthSummary() {
    }

    public PendingAuthSummary(Long acctId, Long custId) {
        this.acctId = acctId;
        this.custId = custId;
    }

    /**
     * Available amount = credit limit − credit balance (BR-01.2).
     */
    public BigDecimal availableAmount() {
        return creditLimit.subtract(creditBalance);
    }

    public void addDetail(PendingAuthDetail detail) {
        detail.setSummary(this);
        this.details.add(detail);
    }

    public Long getAcctId() {
        return acctId;
    }

    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCashLimit() {
        return cashLimit;
    }

    public void setCashLimit(BigDecimal cashLimit) {
        this.cashLimit = cashLimit;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public void setCreditBalance(BigDecimal creditBalance) {
        this.creditBalance = creditBalance;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public int getApprovedAuthCount() {
        return approvedAuthCount;
    }

    public void setApprovedAuthCount(int approvedAuthCount) {
        this.approvedAuthCount = approvedAuthCount;
    }

    public int getDeclinedAuthCount() {
        return declinedAuthCount;
    }

    public void setDeclinedAuthCount(int declinedAuthCount) {
        this.declinedAuthCount = declinedAuthCount;
    }

    public BigDecimal getApprovedAuthAmount() {
        return approvedAuthAmount;
    }

    public void setApprovedAuthAmount(BigDecimal approvedAuthAmount) {
        this.approvedAuthAmount = approvedAuthAmount;
    }

    public BigDecimal getDeclinedAuthAmount() {
        return declinedAuthAmount;
    }

    public void setDeclinedAuthAmount(BigDecimal declinedAuthAmount) {
        this.declinedAuthAmount = declinedAuthAmount;
    }

    public List<PendingAuthDetail> getDetails() {
        return details;
    }
}

