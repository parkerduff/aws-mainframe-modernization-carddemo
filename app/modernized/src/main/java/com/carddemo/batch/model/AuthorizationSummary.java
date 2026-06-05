package com.carddemo.batch.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modern representation of the IMS pending-authorization <em>summary</em>
 * (root) segment, copybook {@code CIPAUSMY.cpy}.
 *
 * <p>The approved/declined counters and amounts are mutable because the purge
 * job adjusts them as expired detail records are removed (see
 * {@code 4000-CHECK-IF-EXPIRED} in {@code CBPAUP0C.cbl}). The remaining fields
 * are immutable identity/limit attributes.
 *
 * <p>{@code acctId} ({@code PA-ACCT-ID}) is the IMS root key and is used as the
 * summary identifier by the repository abstraction.
 */
public class AuthorizationSummary {

    private final long acctId;            // PA-ACCT-ID          PIC S9(11) COMP-3
    private final long custId;            // PA-CUST-ID          PIC 9(09)
    private final String authStatus;      // PA-AUTH-STATUS      PIC X(01)
    private final BigDecimal creditLimit; // PA-CREDIT-LIMIT     PIC S9(09)V99 COMP-3
    private final BigDecimal cashLimit;   // PA-CASH-LIMIT       PIC S9(09)V99 COMP-3
    private final BigDecimal creditBalance; // PA-CREDIT-BALANCE PIC S9(09)V99 COMP-3
    private final BigDecimal cashBalance; // PA-CASH-BALANCE     PIC S9(09)V99 COMP-3

    private int approvedAuthCnt;          // PA-APPROVED-AUTH-CNT PIC S9(04) COMP
    private int declinedAuthCnt;          // PA-DECLINED-AUTH-CNT PIC S9(04) COMP
    private BigDecimal approvedAuthAmt;   // PA-APPROVED-AUTH-AMT PIC S9(09)V99 COMP-3
    private BigDecimal declinedAuthAmt;   // PA-DECLINED-AUTH-AMT PIC S9(09)V99 COMP-3

    public AuthorizationSummary(long acctId,
                                long custId,
                                String authStatus,
                                BigDecimal creditLimit,
                                BigDecimal cashLimit,
                                BigDecimal creditBalance,
                                BigDecimal cashBalance,
                                int approvedAuthCnt,
                                int declinedAuthCnt,
                                BigDecimal approvedAuthAmt,
                                BigDecimal declinedAuthAmt) {
        this.acctId = acctId;
        this.custId = custId;
        this.authStatus = authStatus;
        this.creditLimit = creditLimit;
        this.cashLimit = cashLimit;
        this.creditBalance = creditBalance;
        this.cashBalance = cashBalance;
        this.approvedAuthCnt = approvedAuthCnt;
        this.declinedAuthCnt = declinedAuthCnt;
        this.approvedAuthAmt = approvedAuthAmt == null ? BigDecimal.ZERO : approvedAuthAmt;
        this.declinedAuthAmt = declinedAuthAmt == null ? BigDecimal.ZERO : declinedAuthAmt;
    }

    public long getAcctId() {
        return acctId;
    }

    public long getCustId() {
        return custId;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCashLimit() {
        return cashLimit;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public int getApprovedAuthCnt() {
        return approvedAuthCnt;
    }

    public void setApprovedAuthCnt(int approvedAuthCnt) {
        this.approvedAuthCnt = approvedAuthCnt;
    }

    public int getDeclinedAuthCnt() {
        return declinedAuthCnt;
    }

    public void setDeclinedAuthCnt(int declinedAuthCnt) {
        this.declinedAuthCnt = declinedAuthCnt;
    }

    public BigDecimal getApprovedAuthAmt() {
        return approvedAuthAmt;
    }

    public void setApprovedAuthAmt(BigDecimal approvedAuthAmt) {
        this.approvedAuthAmt = approvedAuthAmt == null ? BigDecimal.ZERO : approvedAuthAmt;
    }

    public BigDecimal getDeclinedAuthAmt() {
        return declinedAuthAmt;
    }

    public void setDeclinedAuthAmt(BigDecimal declinedAuthAmt) {
        this.declinedAuthAmt = declinedAuthAmt == null ? BigDecimal.ZERO : declinedAuthAmt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthorizationSummary that)) {
            return false;
        }
        return acctId == that.acctId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(acctId);
    }

    @Override
    public String toString() {
        return "AuthorizationSummary{acctId=" + acctId
                + ", approvedAuthCnt=" + approvedAuthCnt
                + ", declinedAuthCnt=" + declinedAuthCnt
                + ", approvedAuthAmt=" + approvedAuthAmt
                + ", declinedAuthAmt=" + declinedAuthAmt + '}';
    }
}
