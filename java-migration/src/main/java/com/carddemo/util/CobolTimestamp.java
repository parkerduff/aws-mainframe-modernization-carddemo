package com.carddemo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converts between the raw 26-character COBOL timestamp representation used by
 * the transaction records (copybooks CVTRA05Y / CVTRA06Y, fields
 * {@code TRAN-ORIG-TS}, {@code TRAN-PROC-TS}) and {@link LocalDateTime}.
 * <p>
 * The on-disk format is {@code yyyy-MM-dd HH:mm:ss.SSSSSS} (26 chars, microsecond
 * precision), for example {@code "2022-06-10 19:27:53.000000"}. Entities store
 * the raw 26-char string to guarantee lossless round-tripping; use this helper
 * when a typed {@link LocalDateTime} is needed.
 */
public final class CobolTimestamp {

    /** The fixed on-disk width of the timestamp field. */
    public static final int WIDTH = 26;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private CobolTimestamp() {
    }

    /**
     * Parses a 26-char COBOL timestamp string into a {@link LocalDateTime},
     * or returns {@code null} if the field is blank/empty.
     */
    public static LocalDateTime toLocalDateTime(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value, FORMATTER);
    }

    /**
     * Formats a {@link LocalDateTime} back into the 26-char COBOL representation.
     */
    public static String toCobol(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return FORMATTER.format(value);
    }
}
