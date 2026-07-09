package com.aws.carddemo.authorization.fraud.support;

import com.aws.carddemo.authorization.fraud.entity.PendingAuthDetail;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Builds a representative {@link PendingAuthDetail} for tests, populated much like the
 * IMS PAUTDTL1 record the COBOL flow reads before marking fraud.
 */
public final class PendingAuthDetailFixture {

    public static final String CARD_NUM = "4111111111111111";
    public static final LocalDateTime AUTH_TS = LocalDateTime.of(2024, 1, 31, 12, 34, 56);

    private PendingAuthDetailFixture() {
    }

    public static PendingAuthDetail newDetail() {
        return newDetail(CARD_NUM, AUTH_TS, null);
    }

    public static PendingAuthDetail newDetail(String cardNum, LocalDateTime authTs, String fraudFlag) {
        PendingAuthDetail detail = new PendingAuthDetail();
        detail.setAuthKey(cardNum + "|" + authTs);
        detail.setCardNum(cardNum);
        detail.setAuthTs(authTs);
        detail.setAuthType("0100");
        detail.setCardExpiryDate("2612");
        detail.setMessageType("0100");
        detail.setMessageSource("POS");
        detail.setAuthIdCode("A1B2C3");
        detail.setAuthRespCode("00");
        detail.setAuthRespReason("0000");
        detail.setProcessingCode("000000");
        detail.setTransactionAmt(new BigDecimal("123.45"));
        detail.setApprovedAmt(new BigDecimal("123.45"));
        detail.setMerchantCategoryCode("5411");
        detail.setAcqrCountryCode("840");
        detail.setPosEntryMode(90);
        detail.setMerchantId("MERCH000000001");
        detail.setMerchantName("ACME STORE");
        detail.setMerchantCity("SEATTLE");
        detail.setMerchantState("WA");
        detail.setMerchantZip("981010000");
        detail.setTransactionId("TXN000000000001");
        detail.setMatchStatus("P");
        detail.setAcctId(10000000001L);
        detail.setCustId(200000001L);
        detail.setFraudFlag(fraudFlag);
        return detail;
    }
}
