package com.aws.carddemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transaction entity, migrated from COBOL copybook {@code app/cpy/CVTRA05Y.cpy} (TRAN-RECORD).
 *
 * <pre>
 *   05 TRAN-ID            PIC X(16).      -> tranId (PK)
 *   05 TRAN-TYPE-CD       PIC X(02).      -> tranTypeCd
 *   05 TRAN-CAT-CD        PIC 9(04).      -> tranCatCd
 *   05 TRAN-SOURCE        PIC X(10).      -> tranSource
 *   05 TRAN-DESC          PIC X(100).     -> tranDesc
 *   05 TRAN-AMT           PIC S9(09)V99.  -> tranAmt
 *   05 TRAN-MERCHANT-ID   PIC 9(09).      -> tranMerchantId
 *   05 TRAN-MERCHANT-NAME PIC X(50).      -> tranMerchantName
 *   05 TRAN-MERCHANT-CITY PIC X(50).      -> tranMerchantCity
 *   05 TRAN-MERCHANT-ZIP  PIC X(10).      -> tranMerchantZip
 *   05 TRAN-CARD-NUM      PIC X(16).      -> tranCardNum
 *   05 TRAN-ORIG-TS       PIC X(26).      -> tranOrigTs
 *   05 TRAN-PROC-TS       PIC X(26).      -> tranProcTs
 * </pre>
 *
 * The COBOL timestamps are kept as Strings (PIC X(26), format
 * {@code yyyy-MM-dd-HH.mm.ss.ffffff}) to preserve the legacy representation byte-for-byte.
 */
@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @Column(name = "tran_id", length = 16, nullable = false)
    private String tranId;

    @Column(name = "tran_type_cd", length = 2)
    private String tranTypeCd;

    @Column(name = "tran_cat_cd")
    private Integer tranCatCd;

    @Column(name = "tran_source", length = 10)
    private String tranSource;

    @Column(name = "tran_desc", length = 100)
    private String tranDesc;

    @Column(name = "tran_amt", precision = 11, scale = 2)
    private BigDecimal tranAmt;

    @Column(name = "tran_merchant_id")
    private Long tranMerchantId;

    @Column(name = "tran_merchant_name", length = 50)
    private String tranMerchantName;

    @Column(name = "tran_merchant_city", length = 50)
    private String tranMerchantCity;

    @Column(name = "tran_merchant_zip", length = 10)
    private String tranMerchantZip;

    @Column(name = "tran_card_num", length = 16)
    private String tranCardNum;

    @Column(name = "tran_orig_ts", length = 26)
    private String tranOrigTs;

    @Column(name = "tran_proc_ts", length = 26)
    private String tranProcTs;
}
