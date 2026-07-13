package com.carddemo.authorization.service;

/**
 * Raised when a referenced authorization detail or summary does not exist.
 */
public class AuthorizationNotFoundException extends RuntimeException {

    public AuthorizationNotFoundException(String message) {
        super(message);
    }
}

