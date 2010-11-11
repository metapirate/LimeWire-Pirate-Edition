package com.limegroup.gnutella.http;

import java.io.IOException;

/**
 * Thrown in replace of IndexOutOfBoundsException or NumberFormatException.
 */
public class ProblemReadingHeaderException extends IOException {

    /**
     * Root cause.
     */
    private final Throwable cause;

    public ProblemReadingHeaderException() {
        super("Problem Reading Header");
        cause = null;
    }

    public ProblemReadingHeaderException(String msg) {
        super(msg);
        cause = null;
    }

    public ProblemReadingHeaderException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    @Override
    public void printStackTrace() {
        super.printStackTrace();
        if (cause != null) {
            System.err.println("Parent Cause:");
            cause.printStackTrace();
        }
    }
}
