package com.aws.carddemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decoder/encoder for COBOL zoned-decimal numbers with a trailing sign overpunch, as used
 * in the ASCII sample data files (e.g. {@code app/data/ASCII/dailytran.txt}).
 *
 * <p>A field such as {@code PIC S9(09)V99} is stored as 11 characters; the final character
 * encodes both the last digit and the sign via the classic overpunch table:</p>
 * <pre>
 *   positive: 0='{' 1='A' 2='B' 3='C' 4='D' 5='E' 6='F' 7='G' 8='H' 9='I'
 *   negative: 0='}' 1='J' 2='K' 3='L' 4='M' 5='N' 6='O' 7='P' 8='Q' 9='R'
 * </pre>
 * The {@code V} indicates an implied decimal point; {@code scale} specifies how many of the
 * trailing digits are fractional (2 for {@code V99}).
 */
public final class ZonedDecimal {

    private ZonedDecimal() {
    }

    /**
     * Decode a trailing-sign zoned-decimal field into a {@link BigDecimal}.
     *
     * @param raw   the fixed-width field (digits with an overpunched last character)
     * @param scale number of implied decimal places (e.g. 2 for {@code V99})
     * @return the decoded value with the given scale
     */
    public static BigDecimal decode(String raw, int scale) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.UNNECESSARY);
        }
        String field = raw.trim();
        char last = field.charAt(field.length() - 1);
        StringBuilder digits = new StringBuilder(field.substring(0, field.length() - 1));

        boolean negative = false;
        int lastDigit;
        if (Character.isDigit(last)) {
            lastDigit = last - '0';
        } else {
            switch (last) {
                case '{' -> lastDigit = 0;
                case 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' -> lastDigit = last - 'A' + 1;
                case '}' -> { lastDigit = 0; negative = true; }
                case 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R' -> { lastDigit = last - 'J' + 1; negative = true; }
                default -> throw new IllegalArgumentException("Invalid zoned-decimal character: " + last);
            }
        }
        digits.append(lastDigit);

        BigDecimal unscaled = new BigDecimal(digits.toString());
        BigDecimal value = unscaled.movePointLeft(scale).setScale(scale, RoundingMode.UNNECESSARY);
        return negative ? value.negate() : value;
    }

    /**
     * Encode a {@link BigDecimal} as a fixed-width trailing-sign zoned-decimal field. Used by
     * tests to build sample input matching the COBOL format.
     *
     * @param value      the value to encode
     * @param totalDigits total number of digits in the field (e.g. 11 for {@code S9(09)V99})
     * @param scale      number of implied decimal places
     * @return the encoded fixed-width string
     */
    public static String encode(BigDecimal value, int totalDigits, int scale) {
        boolean negative = value.signum() < 0;
        BigDecimal abs = value.abs().setScale(scale, RoundingMode.HALF_UP);
        String unscaled = abs.movePointRight(scale).toBigInteger().toString();
        if (unscaled.length() > totalDigits) {
            throw new IllegalArgumentException("Value " + value + " does not fit in " + totalDigits + " digits");
        }
        String padded = "0".repeat(totalDigits - unscaled.length()) + unscaled;
        char lastDigit = padded.charAt(padded.length() - 1);
        int d = lastDigit - '0';
        char overpunch;
        if (negative) {
            overpunch = d == 0 ? '}' : (char) ('J' + d - 1);
        } else {
            overpunch = d == 0 ? '{' : (char) ('A' + d - 1);
        }
        return padded.substring(0, padded.length() - 1) + overpunch;
    }
}
