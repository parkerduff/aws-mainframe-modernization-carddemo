package com.aws.carddemo.dto;

/**
 * Update-user request, mirroring COUSR02C (update user). Null fields are left unchanged;
 * a non-blank password is re-hashed.
 */
public record UserUpdateDto(
        String firstName,
        String lastName,
        String password,
        String userType) {
}
