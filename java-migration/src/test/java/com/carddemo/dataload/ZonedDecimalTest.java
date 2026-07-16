package com.carddemo.dataload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ZonedDecimalTest {

    @Test
    void parsesPositiveZeroOverpunch() {
        BigDecimal v = ZonedDecimal.parse("0000000000{", 2);
        assertThat(v).isEqualByComparingTo("0.00");
        assertThat(v.scale()).isEqualTo(2);
    }

    @Test
    void parsesNegativeZeroOverpunch() {
        BigDecimal v = ZonedDecimal.parse("0000009190}", 2);
        assertThat(v).isEqualByComparingTo("-919.00");
        assertThat(v.scale()).isEqualTo(2);
    }

    @Test
    void parsesPositiveDigitOverpunch() {
        // 'G' encodes +7 -> ...047 & 7 -> 50477 -> 504.77
        assertThat(ZonedDecimal.parse("0000005047G", 2)).isEqualByComparingTo("504.77");
    }

    @Test
    void parsesEachPositiveOverpunchLetter() {
        String[] letters = {"{", "A", "B", "C", "D", "E", "F", "G", "H", "I"};
        for (int d = 0; d <= 9; d++) {
            BigDecimal v = ZonedDecimal.parse("0000000000" + letters[d], 2);
            assertThat(v).isEqualByComparingTo(BigDecimal.valueOf(d, 2));
        }
    }

    @Test
    void parsesEachNegativeOverpunchLetter() {
        String[] letters = {"}", "J", "K", "L", "M", "N", "O", "P", "Q", "R"};
        for (int d = 0; d <= 9; d++) {
            BigDecimal v = ZonedDecimal.parse("0000000000" + letters[d], 2);
            assertThat(v).isEqualByComparingTo(BigDecimal.valueOf(-d, 2));
        }
    }

    @Test
    void alwaysProducesScaleTwoWithNoRoundingDrift() {
        BigDecimal v = ZonedDecimal.parse("00012345678I", 2); // 1234567.89
        assertThat(v).isEqualTo(new BigDecimal("1234567.89"));
        assertThat(v.scale()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidOverpunch() {
        assertThatThrownBy(() -> ZonedDecimal.parse("000000000*", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
