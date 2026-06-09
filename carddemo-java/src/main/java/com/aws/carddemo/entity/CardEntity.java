package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card entity, migrated from COBOL copybook {@code app/cpy/CVACT02Y.cpy} (CARD-RECORD).
 *
 * <pre>
 *   05 CARD-NUM            PIC X(16).  -> cardNum (PK)
 *   05 CARD-ACCT-ID        PIC 9(11).  -> acctId
 *   05 CARD-CVV-CD         PIC 9(03).  -> cvvCd
 *   05 CARD-EMBOSSED-NAME  PIC X(50).  -> embossedName
 *   05 CARD-EXPIRAION-DATE PIC X(10).  -> expirationDate
 *   05 CARD-ACTIVE-STATUS  PIC X(01).  -> activeStatus
 * </pre>
 *
 * Note: the legacy CVCRD01Y.cpy customer-id field (CC-CUST-ID PIC X(09)) is not part of
 * the card record itself; the card-to-customer relationship is held in {@link CardXrefEntity}.
 */
@Entity
@Table(name = "card")
@Getter
@Setter
@NoArgsConstructor
public class CardEntity {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cvv_cd", length = 3)
    private String cvvCd;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date", length = 10)
    private String expirationDate;

    @Column(name = "active_status", length = 1)
    private String activeStatus;
}
