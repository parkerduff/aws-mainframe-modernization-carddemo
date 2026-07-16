package com.carddemo.dataload;

import java.math.BigDecimal;

/**
 * A cursor over one fixed-width record line. Fields are consumed left-to-right
 * using the exact byte widths derived from the COBOL PIC clauses, so callers
 * declare widths in copybook order without tracking absolute offsets.
 * <p>
 * The backing line is right-padded with spaces to the declared record length so
 * that trailing FILLER (which is often trimmed in the ASCII sample files) never
 * causes an out-of-bounds read.
 */
public final class FixedWidthRecord {

    private final String line;
    private int pos;

    private FixedWidthRecord(String line) {
        this.line = line;
    }

    /**
     * Wraps a raw line, right-padding it with spaces to {@code recordLength}.
     */
    public static FixedWidthRecord of(String rawLine, int recordLength) {
        String stripped = stripLineEnding(rawLine);
        if (stripped.length() < recordLength) {
            StringBuilder sb = new StringBuilder(recordLength);
            sb.append(stripped);
            while (sb.length() < recordLength) {
                sb.append(' ');
            }
            stripped = sb.toString();
        }
        return new FixedWidthRecord(stripped);
    }

    private static String stripLineEnding(String s) {
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) {
            end--;
        }
        return s.substring(0, end);
    }

    /** Returns the next {@code width} characters verbatim (no trimming). */
    public String raw(int width) {
        String slice = line.substring(pos, pos + width);
        pos += width;
        return slice;
    }

    /** Returns the next {@code width} characters as an alphanumeric field, right-trimmed. */
    public String text(int width) {
        return stripTrailing(raw(width));
    }

    /** Returns the next {@code width} characters as an unsigned integer (PIC 9(n)) or null if blank. */
    public Long unsignedLong(int width) {
        String slice = raw(width).trim();
        if (slice.isEmpty()) {
            return null;
        }
        return Long.parseLong(slice);
    }

    /** Returns the next {@code width} characters as an unsigned integer (PIC 9(n)) or null if blank. */
    public Integer unsignedInt(int width) {
        Long value = unsignedLong(width);
        return value == null ? null : value.intValue();
    }

    /** Returns the next {@code width} characters as a signed zoned decimal (PIC S9..V99). */
    public BigDecimal amount(int width, int scale) {
        return ZonedDecimal.parse(raw(width), scale);
    }

    /** Number of characters consumed so far (useful for asserting record length). */
    public int position() {
        return pos;
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
