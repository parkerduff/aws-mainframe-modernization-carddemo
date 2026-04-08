package com.cardemo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard error response")
public record ErrorResponse(
    @Schema(description = "HTTP status code") int status,
    @Schema(description = "Error message") String message,
    @Schema(description = "Timestamp") LocalDateTime timestamp,
    @Schema(description = "Field-level errors") List<FieldError> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now(), null);
    }

    public record FieldError(String field, String message) {}
}
