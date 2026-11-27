package com.classroombooking.application.booking;

/** Indicates that the selected classroom is already reserved during the requested time. */
public final class BookingConflictException extends IllegalArgumentException {
    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
