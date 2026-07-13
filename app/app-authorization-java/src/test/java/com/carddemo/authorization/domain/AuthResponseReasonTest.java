package com.carddemo.authorization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the reason-code catalogue.
 *
 * <p>Traceability: BR-01.7 (reason catalogue), BR-02 (fraud reason codes
 * 5100/5200/5300).
 */
class AuthResponseReasonTest {

    @Test
    void fraudReasonCodesMatchLegacyTable() {
        assertThat(AuthResponseReason.CARD_FRAUD.code()).isEqualTo("5100");
        assertThat(AuthResponseReason.CARD_FRAUD.description()).isEqualTo("CARD FRAUD");
        assertThat(AuthResponseReason.MERCHANT_FRAUD.code()).isEqualTo("5200");
        assertThat(AuthResponseReason.MERCHANT_FRAUD.description()).isEqualTo("MERCHANT FRAUD");
        assertThat(AuthResponseReason.LOST_CARD.code()).isEqualTo("5300");
        assertThat(AuthResponseReason.LOST_CARD.description()).isEqualTo("LOST CARD");
    }

    @Test
    void coreReasonCodesMatchLegacyTable() {
        assertThat(AuthResponseReason.APPROVED.code()).isEqualTo("0000");
        assertThat(AuthResponseReason.INVALID_CARD.code()).isEqualTo("3100");
        assertThat(AuthResponseReason.INSUFFICIENT_FUND.code()).isEqualTo("4100");
    }

    @Test
    void fromCodeResolvesKnownAndFallsBackToUnknown() {
        assertThat(AuthResponseReason.fromCode("5100")).isEqualTo(AuthResponseReason.CARD_FRAUD);
        assertThat(AuthResponseReason.fromCode("9999")).isEqualTo(AuthResponseReason.UNKNOWN);
        assertThat(AuthResponseReason.fromCode(null)).isEqualTo(AuthResponseReason.UNKNOWN);
    }
}

