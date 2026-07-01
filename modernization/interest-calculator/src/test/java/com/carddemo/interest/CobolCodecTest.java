package com.carddemo.interest;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.interest.domain.CobolCodec;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Verifies zoned-decimal / fixed-width encoding is byte-faithful to the CardDemo files.
 */
class CobolCodecTest {

    @Test
    void parsesPositiveOverpunchZonedDecimal() {
        // '{' overpunch on the trailing byte = digit 0, positive. 00000000000{ -> 0000000194 0(+0) => 194.00
        assertThat(CobolCodec.parseZonedDecimal("00000001940{", 2)).isEqualByComparingTo("194.00");
        assertThat(CobolCodec.parseZonedDecimal("0000000000{", 2)).isEqualByComparingTo("0.00");
    }

    @Test
    void parsesNegativeOverpunchZonedDecimal() {
        // trailing 'K' = negative 2 -> ...2 negative
        assertThat(CobolCodec.parseZonedDecimal("0000000075K", 2)).isEqualByComparingTo("-7.52");
    }

    @Test
    void formatZonedDecimalRoundTripsThroughParse() {
        BigDecimal value = new BigDecimal("12.50");
        String encoded = CobolCodec.formatZonedDecimal(value, 11, 2);
        assertThat(encoded).hasSize(11);
        assertThat(CobolCodec.parseZonedDecimal(encoded, 2)).isEqualByComparingTo(value);
    }

    @Test
    void formatZonedDecimalEncodesNegativeWithOverpunch() {
        String encoded = CobolCodec.formatZonedDecimal(new BigDecimal("-7.50"), 11, 2);
        assertThat(CobolCodec.parseZonedDecimal(encoded, 2)).isEqualByComparingTo("-7.50");
        // trailing byte carries the negative sign overpunch, not a plain digit
        assertThat("}JKLMNOPQR").contains(encoded.substring(encoded.length() - 1));
    }

    @Test
    void numericAndTextFieldsPadAndParse() {
        assertThat(CobolCodec.formatNumeric(5, 4)).isEqualTo("0005");
        assertThat(CobolCodec.parseNumeric("00000000042")).isEqualTo(42L);
        assertThat(CobolCodec.padRight("System", 10)).isEqualTo("System    ");
        assertThat(CobolCodec.parseText("System    ")).isEqualTo("System");
    }
}
