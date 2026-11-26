package com.classroombooking.infrastructure.persistence;

/** Reports a persistence operation failure without exposing JDBC details to the console layer. */
public final class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
