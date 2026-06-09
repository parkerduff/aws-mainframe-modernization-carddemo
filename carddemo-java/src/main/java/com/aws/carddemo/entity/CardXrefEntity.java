package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Card cross-reference entity, migrated from COBOL copybook {@code app/cpy/CVACT03Y.cpy}
 * (CARD-XREF-RECORD).
 *
 * <pre>
 *   05 XREF-CARD-NUM PIC X(16). -> cardNum (PK)
 *   05 XREF-CUST-ID  PIC 9(09). -> custId
 *   05 XREF-ACCT-ID  PIC 9(11). -> acctId
 * </pre>
 *
 * In the legacy VSAM design this KSDS (with alternate indexes) linked a card number to its
 * owning customer and account. The online programs (e.g. COACTVWC) and the batch poster
 * (CBTRN02C) read this file to resolve a card number into an account id.
 */
@Entity
@Table(name = "card_xref")
@Getter
@Setter
@NoArgsConstructor
public class CardXrefEntity {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "cust_id")
    private Long custId;

    @Column(name = "acct_id")
    private Long acctId;
}
