package com.aws.carddemo.exception;

/**
 * Thrown when a requested record does not exist. Corresponds to the COBOL "record not
 * found" / INVALID KEY conditions (e.g. RESP=13 NOTFND in the online programs).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
