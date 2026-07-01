package com.carddemo.interest.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * Utility for reading and writing the fixed-width, zoned-decimal field
 * representations used by the CardDemo VSAM/flat files that CBACT04C processes.
 *
 * <p>All monetary fields in the copybooks are {@code PIC S9(n)V99} zoned decimal:
 * the value is stored as ASCII digits with an implied decimal point and the sign
 * carried as an "overpunch" on the trailing byte. This codec decodes/encodes that
 * representation exactly so the Java module is byte-faithful to the mainframe data.
 */
public final class CobolCodec {

    private CobolCodec() {
    }

    /** Trailing overpunch characters representing a positive digit 0-9. */
    private static final Map<Character, Character> POSITIVE_OVERPUNCH = Map.ofEntries(
            Map.entry('{', '0'), Map.entry('A', '1'), Map.entry('B', '2'), Map.entry('C', '3'),
            Map.entry('D', '4'), Map.entry('E', '5'), Map.entry('F', '6'), Map.entry('G', '7'),
            Map.entry('H', '8'), Map.entry('I', '9'));

    /** Trailing overpunch characters representing a negative digit 0-9. */
    private static final Map<Character, Character> NEGATIVE_OVERPUNCH = Map.ofEntries(
            Map.entry('}', '0'), Map.entry('J', '1'), Map.entry('K', '2'), Map.entry('L', '3'),
            Map.entry('M', '4'), Map.entry('N', '5'), Map.entry('O', '6'), Map.entry('P', '7'),
            Map.entry('Q', '8'), Map.entry('R', '9'));

    /**
     * Decode a zoned-decimal field ({@code PIC S9(m)V(scale)}) into a {@link BigDecimal}.
     *
     * @param raw   the raw field text (integer + fractional digits, sign overpunched on last byte)
     * @param scale number of implied decimal places (e.g. 2 for V99)
     */
    public static BigDecimal parseZonedDecimal(String raw, int scale) {
        String digits = raw;
        int sign = 1;
        char last = raw.charAt(raw.length() - 1);
        if (POSITIVE_OVERPUNCH.containsKey(last)) {
            digits = raw.substring(0, raw.length() - 1) + POSITIVE_OVERPUNCH.get(last);
        } else if (NEGATIVE_OVERPUNCH.containsKey(last)) {
            digits = raw.substring(0, raw.length() - 1) + NEGATIVE_OVERPUNCH.get(last);
            sign = -1;
        }
        BigInteger unscaled = new BigInteger(digits.trim().isEmpty() ? "0" : digits);
        if (sign < 0) {
            unscaled = unscaled.negate();
        }
        return new BigDecimal(unscaled, scale);
    }

    /**
     * Encode a {@link BigDecimal} back to zoned-decimal text of {@code totalDigits} length
     * (integer + fractional digits) with the sign overpunched on the trailing byte.
     *
     * @param value       the value to encode
     * @param totalDigits total number of digit positions (integer digits + scale)
     * @param scale       number of implied decimal places
     */
    public static String formatZonedDecimal(BigDecimal value, int totalDigits, int scale) {
        BigDecimal scaled = value.setScale(scale, java.math.RoundingMode.DOWN);
        boolean negative = scaled.signum() < 0;
        String digits = scaled.abs().movePointRight(scale).toBigInteger().toString();
        digits = padLeft(digits, totalDigits, '0');
        char lastDigit = digits.charAt(digits.length() - 1);
        char overpunch = overpunchFor(lastDigit, negative);
        return digits.substring(0, digits.length() - 1) + overpunch;
    }

    private static char overpunchFor(char digit, boolean negative) {
        Map<Character, Character> table = negative ? NEGATIVE_OVERPUNCH : POSITIVE_OVERPUNCH;
        for (Map.Entry<Character, Character> e : table.entrySet()) {
            if (e.getValue() == digit) {
                return e.getKey();
            }
        }
        throw new IllegalArgumentException("Not a digit: " + digit);
    }

    /** Parse a {@code PIC 9(n)} unsigned numeric field to a long. */
    public static long parseNumeric(String raw) {
        String t = raw.trim();
        return t.isEmpty() ? 0L : Long.parseLong(t);
    }

    /** Trim trailing spaces from a {@code PIC X(n)} field, preserving embedded/leading content. */
    public static String parseText(String raw) {
        int end = raw.length();
        while (end > 0 && raw.charAt(end - 1) == ' ') {
            end--;
        }
        return raw.substring(0, end);
    }

    /** Right-pad a string to {@code width} with spaces (COBOL {@code PIC X} display convention). */
    public static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Left-pad a string to {@code width} with the given pad character. */
    public static String padLeft(String s, int width, char pad) {
        if (s.length() >= width) {
            return s.substring(s.length() - width);
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < width - s.length()) {
            sb.append(pad);
        }
        sb.append(s);
        return sb.toString();
    }

    /** Format a long as an unsigned zero-padded {@code PIC 9(width)} field. */
    public static String formatNumeric(long value, int width) {
        return padLeft(Long.toString(value), width, '0');
    }
}
