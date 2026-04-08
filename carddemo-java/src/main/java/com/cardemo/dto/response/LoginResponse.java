package com.cardemo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response with JWT token")
public record LoginResponse(
    @Schema(description = "User ID") String userId,
    @Schema(description = "User type: A=Admin, U=User") String userType,
    @Schema(description = "First name") String firstName,
    @Schema(description = "Last name") String lastName,
    @Schema(description = "JWT access token") String token
) {}
