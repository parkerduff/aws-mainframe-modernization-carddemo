package com.aws.carddemo.dto;

/**
 * Sign-on response. The token carries the equivalent of the COBOL COMMAREA general
 * info (userId, userType). {@code nextProgram} mirrors the COSGN00C routing decision:
 * COADM01C for admins, COMEN01C for regular users.
 */
public record LoginResponse(
        String token,
        String userId,
        String userType,
        String nextProgram) {
}
