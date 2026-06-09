package com.aws.carddemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create-user request, mirroring COUSR01C (add user). The user id matches the legacy
 * 8-character SEC-USR-ID key; the password is BCrypt-hashed before storage.
 */
public record UserCreateDto(
        @NotBlank @Size(max = 8) String userId,
        @NotBlank @Size(max = 20) String firstName,
        @NotBlank @Size(max = 20) String lastName,
        @NotBlank String password,
        @NotBlank @Size(min = 1, max = 1) String userType) {
}
