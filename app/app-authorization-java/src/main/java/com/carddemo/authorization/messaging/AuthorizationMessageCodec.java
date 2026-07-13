package com.carddemo.authorization.messaging;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Parses/formats the comma-separated MQ authorization messages (BR-06).
 *
 * <p>Replaces the {@code UNSTRING ... DELIMITED BY ','} in {@code COPAUA0C}
 * {@code 2100-EXTRACT-REQUEST-MSG} (request) and the reply {@code STRING} in
 * {@code 6000-MAKE-DECISION} (reply). Field order matches copybooks
 * {@code CCPAURQY}/{@code CCPAURLY} and the README message-format tables.
 */
@Component
public class AuthorizationMessageCodec {

    private static final int REQUEST_FIELD_COUNT = 18;

    /** Parse a request message (BR-06.1). */
    public AuthorizationRequest parseRequest(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Authorization request message is null");
        }
        // -1 limit keeps trailing empty fields, matching COBOL fixed positional UNSTRING.
        String[] f = message.split(",", -1);
        if (f.length < REQUEST_FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "Authorization request has " + f.length + " fields, expected " + REQUEST_FIELD_COUNT);
        }
        return new AuthorizationRequest(
                trim(f[0]),
                trim(f[1]),
                trim(f[2]),
                trim(f[3]),
                trim(f[4]),
                trim(f[5]),
                trim(f[6]),
                trim(f[7]),
                parseAmount(f[8]),
                trim(f[9]),
                trim(f[10]),
                parseInt(f[11]),
                trim(f[12]),
                trim(f[13]),
                trim(f[14]),
                trim(f[15]),
                trim(f[16]),
                trim(f[17]));
    }

    /** Format a reply message (BR-06.2). */
    public String formatReply(AuthorizationReply reply) {
        return String.join(",",
                nullToEmpty(reply.cardNum()),
                nullToEmpty(reply.transactionId()),
                nullToEmpty(reply.authIdCode()),
                nullToEmpty(reply.authRespCode()),
                nullToEmpty(reply.authRespReason()),
                formatAmount(reply.approvedAmt()));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static BigDecimal parseAmount(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // COBOL NUMVAL tolerates leading sign / whitespace (PIC +9(10).99).
        return new BigDecimal(trimmed.replace("+", "").trim());
    }

    private static Integer parseInt(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        return Integer.valueOf(trimmed);
    }

    /**
     * Format an amount the way COBOL {@code PIC +9(10).99} does: a mandatory sign
     * followed by the value scaled to 2 decimals (BR-06.3).
     */
    private static String formatAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        value = value.setScale(2, java.math.RoundingMode.HALF_UP);
        String sign = value.signum() < 0 ? "-" : "+";
        return sign + value.abs().toPlainString();
    }
}

