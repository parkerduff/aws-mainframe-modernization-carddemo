package com.carddemo.authorization.domain;

/**
 * Two-character authorization response codes (BR-01).
 *
 * <p>From {@code COPAUA0C} {@code 6000-MAKE-DECISION}: {@code '00'} approved,
 * {@code '05'} declined.
 */
public final class AuthResponseCode {

    public static final String APPROVED = "00";
    public static final String DECLINED = "05";

    private AuthResponseCode() {
    }
}

