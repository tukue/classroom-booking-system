package com.classroombooking.application.booking;

/** Indicates that the selected classroom is already reserved during the requested time. */
public final class BookingDuplicateException extends IllegalArgumentException {
    public BookingDuplicateException(String message) {
        super(message);
    }

    public BookingDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}
