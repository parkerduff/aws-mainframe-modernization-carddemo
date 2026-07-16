package com.carddemo.dataload;

import java.math.BigDecimal;

/**
 * Parses COBOL zoned-decimal (DISPLAY) numeric fields as they appear in the
 * fixed-width ASCII sample files.
 * <p>
 * A signed field such as {@code PIC S9(09)V99} occupies one byte per digit. The
 * sign is "overpunched" onto the final byte, so the last character encodes both
 * the least-significant digit and the sign, using the standard IBM convention:
 * <pre>
 *   digit:      0    1  2  3  4  5  6  7  8  9
 *   positive:   {    A  B  C  D  E  F  G  H  I
 *   negative:   }    J  K  L  M  N  O  P  Q  R
 * </pre>
 * Unsigned fields simply contain the digits {@code 0-9}. The {@code V} in the
 * PIC clause is an implied decimal point (no byte on disk); the caller supplies
 * the number of implied decimal places as {@code scale}.
 * <p>
 * Parsing is exact: the digits are assembled as an integer and the decimal point
 * is shifted left by {@code scale}, so there is never any binary rounding drift.
 */
public final class ZonedDecimal {

    private ZonedDecimal() {
    }

    /**
     * Parses a zoned-decimal field into a {@link BigDecimal} with exactly
     * {@code scale} fractional digits.
     *
     * @param raw   the raw fixed-width field (e.g. {@code "0000005047G"})
     * @param scale the number of implied decimal places (2 for the CardDemo amounts)
     * @return the exact value with {@code setScale(scale)} applied
     */
    public static BigDecimal parse(String raw, int scale) {
        if (raw == null) {
            throw new IllegalArgumentException("zoned-decimal field is null");
        }
        String field = raw.trim();
        if (field.isEmpty()) {
            return BigDecimal.ZERO.setScale(scale);
        }

        char last = field.charAt(field.length() - 1);
        StringBuilder digits = new StringBuilder(field.length());
        digits.append(field, 0, field.length() - 1);

        boolean negative = false;
        int lastDigit;
        if (last >= '0' && last <= '9') {
            lastDigit = last - '0';
        } else if (last == '{') {
            lastDigit = 0;
        } else if (last == '}') {
            lastDigit = 0;
            negative = true;
        } else if (last >= 'A' && last <= 'I') {
            lastDigit = last - 'A' + 1;
        } else if (last >= 'J' && last <= 'R') {
            lastDigit = last - 'J' + 1;
            negative = true;
        } else {
            throw new IllegalArgumentException(
                    "Invalid zoned-decimal overpunch character '" + last + "' in field \"" + raw + "\"");
        }
        digits.append((char) ('0' + lastDigit));

        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(
                        "Non-numeric character '" + c + "' in zoned-decimal field \"" + raw + "\"");
            }
        }

        BigDecimal value = new BigDecimal(digits.toString()).movePointLeft(scale).setScale(scale);
        return negative ? value.negate() : value;
    }
}
