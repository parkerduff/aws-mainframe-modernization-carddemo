package com.aws.carddemo.exception;

/**
 * Thrown on a failed sign-on. Corresponds to the COSGN00C error handling (WS-ERR-FLG)
 * for "User not found" (RESP=13) and "Wrong Password" cases.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
