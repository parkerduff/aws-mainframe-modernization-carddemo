package com.carddemo.authorization.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the MQ message codec.
 *
 * <p>Traceability: BR-06.1 (request format/field order), BR-06.2 (reply format),
 * BR-06.3 (decimal handling).
 */
class AuthorizationMessageCodecTest {

    private final AuthorizationMessageCodec codec = new AuthorizationMessageCodec();

    @Test
    void parsesRequestInDocumentedFieldOrder() {
        String message = String.join(",",
                "240115", "120000", "4111111111111111", "0100", "2512", "0100", "0100",
                "000000", "150.75", "5411", "840", "90", "MERCH000000001", "TEST MERCHANT",
                "ANYTOWN", "NY", "10001", "TX0000000000001");

        AuthorizationRequest request = codec.parseRequest(message);

        assertThat(request.authDate()).isEqualTo("240115");
        assertThat(request.cardNum()).isEqualTo("4111111111111111");
        assertThat(request.transactionAmt()).isEqualByComparingTo("150.75");
        assertThat(request.posEntryMode()).isEqualTo(90);
        assertThat(request.transactionId()).isEqualTo("TX0000000000001");
    }

    @Test
    void parseRejectsMessageWithTooFewFields() {
        assertThatThrownBy(() -> codec.parseRequest("240115,120000,4111111111111111"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatsReplyInDocumentedFieldOrderWithSignedAmount() {
        AuthorizationReply reply = new AuthorizationReply(
                "4111111111111111", "TX0000000000001", "120000", "00", "0000",
                new BigDecimal("150.75"));

        String message = codec.formatReply(reply);

        assertThat(message).isEqualTo("4111111111111111,TX0000000000001,120000,00,0000,+150.75");
    }

    @Test
    void formatsDeclinedReplyWithZeroApprovedAmount() {
        AuthorizationReply reply = new AuthorizationReply(
                "4111111111111111", "TX0000000000001", "120000", "05", "4100", BigDecimal.ZERO);

        assertThat(codec.formatReply(reply)).endsWith(",05,4100,+0.00");
    }
}

