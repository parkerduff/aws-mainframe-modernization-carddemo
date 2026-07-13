package com.carddemo.authorization;

import com.carddemo.authorization.messaging.AuthorizationRequest;
import java.math.BigDecimal;

/**
 * Shared builders for tests.
 */
public final class TestFixtures {

    public static final String CARD_NUM = "4111111111111111";

    private TestFixtures() {
    }

    public static AuthorizationRequest request(BigDecimal amount) {
        return request(CARD_NUM, amount, "TX0000000000001");
    }

    public static AuthorizationRequest request(String cardNum, BigDecimal amount, String transactionId) {
        return new AuthorizationRequest(
                "240115",
                "120000",
                cardNum,
                "0100",
                "2512",
                "0100",
                "POS",
                "000000",
                amount,
                "5411",
                "840",
                90,
                "MERCH000000001",
                "TEST MERCHANT",
                "ANYTOWN",
                "NY",
                "10001",
                transactionId);
    }
}

