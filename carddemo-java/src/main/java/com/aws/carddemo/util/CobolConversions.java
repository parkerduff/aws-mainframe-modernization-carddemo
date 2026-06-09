package com.aws.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper conversions for COBOL data idioms.
 *
 * <p>The legacy code frequently used the {@code REDEFINES} pattern to view the same storage
 * both as display text and as a number, e.g. in {@code app/cpy/CVCRD01Y.cpy}:
 * <pre>
 *   10 CC-ACCT-ID                        PIC X(11) VALUE SPACES.
 *   10 CC-ACCT-ID-N REDEFINES CC-ACCT-ID PIC 9(11).
 * </pre>
 * and in {@code app/cbl/COACTVWC.cbl} where the entered account id (text) is validated and
 * reinterpreted as numeric. These helpers centralize that text&lt;-&gt;numeric conversion.</p>
 */
public final class CobolConversions {

    private CobolConversions() {
    }

    /**
     * Parse a fixed-width numeric display field (e.g. {@code PIC X(11)} viewed as {@code 9(11)})
     * into a Long. Spaces are treated as zero, mirroring the COBOL default for an unset field.
     *
     * @param value the display string (may contain leading zeros or spaces)
     * @return the numeric value, or {@code null} if the input is null
     */
    public static Long picXToNumber(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(trimmed);
    }

    /**
     * Format a Long as a zero-padded fixed-width display field, the inverse of
     * {@link #picXToNumber(String)}.
     *
     * @param value the numeric value
     * @param width the picture width (e.g. 11 for {@code PIC 9(11)})
     * @return the zero-padded string, or all spaces if value is null
     */
    public static String numberToPicX(Long value, int width) {
        if (value == null) {
            return " ".repeat(width);
        }
        return String.format("%0" + width + "d", value);
    }

    /**
     * Normalize a monetary amount to scale 2 (HALF_UP), preserving the fixed-point
     * semantics of COBOL {@code PIC S9(n)V99} fields.
     */
    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
