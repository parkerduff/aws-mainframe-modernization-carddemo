package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request - migrated from COSGN00 BMS signon screen")
public record LoginRequest(
    @NotBlank @Schema(description = "User ID (max 8 chars)", example = "USER0001") String userId,
    @NotBlank @Schema(description = "Password") String password
) {}
