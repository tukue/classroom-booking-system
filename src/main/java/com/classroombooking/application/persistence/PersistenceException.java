package com.classroombooking.application.persistence;

/** Application-level, user-safe error for a persistence operation. */
public final class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
