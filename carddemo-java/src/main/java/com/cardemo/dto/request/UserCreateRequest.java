package com.cardemo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User creation request - migrated from COUSR01 add user screen")
public record UserCreateRequest(
    @NotBlank @Size(max = 8) @Schema(description = "User ID") String userId,
    @NotBlank @Size(max = 20) @Schema(description = "First name") String firstName,
    @NotBlank @Size(max = 20) @Schema(description = "Last name") String lastName,
    @NotBlank @Schema(description = "Password") String password,
    @Schema(description = "User type: A=Admin, U=User", example = "U") String userType
) {}
