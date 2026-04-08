package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User update request - migrated from COUSR02 update user screen")
public record UserUpdateRequest(
    @Schema(description = "First name") String firstName,
    @Schema(description = "Last name") String lastName,
    @Schema(description = "New password") String password,
    @Schema(description = "User type: A=Admin, U=User") String userType
) {}
