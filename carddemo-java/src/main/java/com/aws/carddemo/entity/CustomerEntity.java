package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer entity, migrated from COBOL copybook {@code app/cpy/CVCUS01Y.cpy} (CUSTOMER-RECORD).
 *
 * <pre>
 *   05 CUST-ID                 PIC 9(09).  -> custId (PK)
 *   05 CUST-FIRST-NAME         PIC X(25).  -> firstName
 *   05 CUST-MIDDLE-NAME        PIC X(25).  -> middleName
 *   05 CUST-LAST-NAME          PIC X(25).  -> lastName
 *   05 CUST-ADDR-LINE-1        PIC X(50).  -> addrLine1
 *   05 CUST-ADDR-LINE-2        PIC X(50).  -> addrLine2
 *   05 CUST-ADDR-LINE-3        PIC X(50).  -> addrLine3
 *   05 CUST-ADDR-STATE-CD      PIC X(02).  -> addrStateCd
 *   05 CUST-ADDR-COUNTRY-CD    PIC X(03).  -> addrCountryCd
 *   05 CUST-ADDR-ZIP           PIC X(10).  -> addrZip
 *   05 CUST-PHONE-NUM-1        PIC X(15).  -> phoneNum1
 *   05 CUST-PHONE-NUM-2        PIC X(15).  -> phoneNum2
 *   05 CUST-SSN                PIC 9(09).  -> ssn
 *   05 CUST-GOVT-ISSUED-ID     PIC X(20).  -> govtIssuedId
 *   05 CUST-DOB-YYYY-MM-DD     PIC X(10).  -> dobYyyyMmDd
 *   05 CUST-EFT-ACCOUNT-ID     PIC X(10).  -> eftAccountId
 *   05 CUST-PRI-CARD-HOLDER-IND PIC X(01). -> priCardHolderInd
 *   05 CUST-FICO-CREDIT-SCORE  PIC 9(03).  -> ficoCreditScore
 * </pre>
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity {

    @Id
    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "first_name", length = 25)
    private String firstName;

    @Column(name = "middle_name", length = 25)
    private String middleName;

    @Column(name = "last_name", length = 25)
    private String lastName;

    @Column(name = "addr_line_1", length = 50)
    private String addrLine1;

    @Column(name = "addr_line_2", length = 50)
    private String addrLine2;

    @Column(name = "addr_line_3", length = 50)
    private String addrLine3;

    @Column(name = "addr_state_cd", length = 2)
    private String addrStateCd;

    @Column(name = "addr_country_cd", length = 3)
    private String addrCountryCd;

    @Column(name = "addr_zip", length = 10)
    private String addrZip;

    @Column(name = "phone_num_1", length = 15)
    private String phoneNum1;

    @Column(name = "phone_num_2", length = 15)
    private String phoneNum2;

    @Column(name = "ssn")
    private Long ssn;

    @Column(name = "govt_issued_id", length = 20)
    private String govtIssuedId;

    @Column(name = "dob_yyyy_mm_dd", length = 10)
    private String dobYyyyMmDd;

    @Column(name = "eft_account_id", length = 10)
    private String eftAccountId;

    @Column(name = "pri_card_holder_ind", length = 1)
    private String priCardHolderInd;

    @Column(name = "fico_credit_score")
    private Integer ficoCreditScore;
}
