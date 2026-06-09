package com.aws.carddemo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sign-on request, replacing the COSGN00C map input fields (USERIDI / PASSWDI).
 */
public record LoginRequest(
        @NotBlank(message = "Please enter User ID ...") String userId,
        @NotBlank(message = "Please enter Password ...") String password) {
}
