package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Account entity, migrated from COBOL copybook {@code app/cpy/CVACT01Y.cpy} (ACCOUNT-RECORD).
 *
 * <pre>
 *   05 ACCT-ID                PIC 9(11).      -> acctId (PK)
 *   05 ACCT-ACTIVE-STATUS     PIC X(01).      -> activeStatus
 *   05 ACCT-CURR-BAL          PIC S9(10)V99.  -> currBal
 *   05 ACCT-CREDIT-LIMIT      PIC S9(10)V99.  -> creditLimit
 *   05 ACCT-CASH-CREDIT-LIMIT PIC S9(10)V99.  -> cashCreditLimit
 *   05 ACCT-OPEN-DATE         PIC X(10).      -> openDate
 *   05 ACCT-EXPIRAION-DATE    PIC X(10).      -> expirationDate
 *   05 ACCT-REISSUE-DATE      PIC X(10).      -> reissueDate
 *   05 ACCT-CURR-CYC-CREDIT   PIC S9(10)V99.  -> currCycCredit
 *   05 ACCT-CURR-CYC-DEBIT    PIC S9(10)V99.  -> currCycDebit
 *   05 ACCT-ADDR-ZIP          PIC X(10).      -> addrZip
 *   05 ACCT-GROUP-ID          PIC X(10).      -> groupId
 * </pre>
 *
 * All monetary fields use {@link BigDecimal} with scale 2 to preserve the COBOL
 * fixed-point decimal semantics of {@code PIC S9(10)V99}.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity {

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "active_status", length = 1)
    private String activeStatus;

    @Column(name = "curr_bal", precision = 12, scale = 2)
    private BigDecimal currBal;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "cash_credit_limit", precision = 12, scale = 2)
    private BigDecimal cashCreditLimit;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @Column(name = "curr_cyc_credit", precision = 12, scale = 2)
    private BigDecimal currCycCredit;

    @Column(name = "curr_cyc_debit", precision = 12, scale = 2)
    private BigDecimal currCycDebit;

    @Column(name = "addr_zip", length = 10)
    private String addrZip;

    @Column(name = "group_id", length = 10)
    private String groupId;
}
