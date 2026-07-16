package com.carddemo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Account master record. Mirrors copybook CVACT01Y (ACCOUNT-RECORD, RECLN 300).
 * Signed decimal amount fields (PIC S9(10)V99) map to BigDecimal with scale 2.
 */
@Entity
@Table(name = "ACCOUNT")
public class Account {

    @Id
    @Column(name = "ACCT_ID")
    private Long acctId; // PIC 9(11)

    @Column(name = "ACCT_ACTIVE_STATUS")
    private String acctActiveStatus; // PIC X(01)

    @Column(name = "ACCT_CURR_BAL")
    private BigDecimal acctCurrBal; // PIC S9(10)V99

    @Column(name = "ACCT_CREDIT_LIMIT")
    private BigDecimal acctCreditLimit; // PIC S9(10)V99

    @Column(name = "ACCT_CASH_CREDIT_LIMIT")
    private BigDecimal acctCashCreditLimit; // PIC S9(10)V99

    @Column(name = "ACCT_OPEN_DATE")
    private String acctOpenDate; // PIC X(10)

    @Column(name = "ACCT_EXPIRATION_DATE")
    private String acctExpirationDate; // PIC X(10) (copybook: ACCT-EXPIRAION-DATE)

    @Column(name = "ACCT_REISSUE_DATE")
    private String acctReissueDate; // PIC X(10)

    @Column(name = "ACCT_CURR_CYC_CREDIT")
    private BigDecimal acctCurrCycCredit; // PIC S9(10)V99

    @Column(name = "ACCT_CURR_CYC_DEBIT")
    private BigDecimal acctCurrCycDebit; // PIC S9(10)V99

    @Column(name = "ACCT_ADDR_ZIP")
    private String acctAddrZip; // PIC X(10)

    @Column(name = "ACCT_GROUP_ID")
    private String acctGroupId; // PIC X(10)

    public Long getAcctId() {
        return acctId;
    }

    public void setAcctId(Long acctId) {
        this.acctId = acctId;
    }

    public String getAcctActiveStatus() {
        return acctActiveStatus;
    }

    public void setAcctActiveStatus(String acctActiveStatus) {
        this.acctActiveStatus = acctActiveStatus;
    }

    public BigDecimal getAcctCurrBal() {
        return acctCurrBal;
    }

    public void setAcctCurrBal(BigDecimal acctCurrBal) {
        this.acctCurrBal = acctCurrBal;
    }

    public BigDecimal getAcctCreditLimit() {
        return acctCreditLimit;
    }

    public void setAcctCreditLimit(BigDecimal acctCreditLimit) {
        this.acctCreditLimit = acctCreditLimit;
    }

    public BigDecimal getAcctCashCreditLimit() {
        return acctCashCreditLimit;
    }

    public void setAcctCashCreditLimit(BigDecimal acctCashCreditLimit) {
        this.acctCashCreditLimit = acctCashCreditLimit;
    }

    public String getAcctOpenDate() {
        return acctOpenDate;
    }

    public void setAcctOpenDate(String acctOpenDate) {
        this.acctOpenDate = acctOpenDate;
    }

    public String getAcctExpirationDate() {
        return acctExpirationDate;
    }

    public void setAcctExpirationDate(String acctExpirationDate) {
        this.acctExpirationDate = acctExpirationDate;
    }

    public String getAcctReissueDate() {
        return acctReissueDate;
    }

    public void setAcctReissueDate(String acctReissueDate) {
        this.acctReissueDate = acctReissueDate;
    }

    public BigDecimal getAcctCurrCycCredit() {
        return acctCurrCycCredit;
    }

    public void setAcctCurrCycCredit(BigDecimal acctCurrCycCredit) {
        this.acctCurrCycCredit = acctCurrCycCredit;
    }

    public BigDecimal getAcctCurrCycDebit() {
        return acctCurrCycDebit;
    }

    public void setAcctCurrCycDebit(BigDecimal acctCurrCycDebit) {
        this.acctCurrCycDebit = acctCurrCycDebit;
    }

    public String getAcctAddrZip() {
        return acctAddrZip;
    }

    public void setAcctAddrZip(String acctAddrZip) {
        this.acctAddrZip = acctAddrZip;
    }

    public String getAcctGroupId() {
        return acctGroupId;
    }

    public void setAcctGroupId(String acctGroupId) {
        this.acctGroupId = acctGroupId;
    }
}
