package com.carddemo.interest.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Produces the 26-character DB2-format timestamp
 * ({@code YYYY-MM-DD-HH.MM.SS.mmm0000}) written to TRAN-ORIG-TS / TRAN-PROC-TS.
 * Mirrors paragraph Z-GET-DB2-FORMAT-TIMESTAMP. Abstracted so tests can supply a
 * deterministic value in place of {@code FUNCTION CURRENT-DATE}.
 */
@FunctionalInterface
public interface TimestampProvider {

    String currentDb2Timestamp();

    /** Default provider deriving the DB2 timestamp from the system clock. */
    static TimestampProvider systemClock() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SS");
        return () -> LocalDateTime.now().format(fmt) + "0000";
    }
}
