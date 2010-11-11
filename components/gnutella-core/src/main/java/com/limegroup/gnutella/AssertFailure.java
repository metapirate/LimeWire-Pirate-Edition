package com.limegroup.gnutella;

public class AssertFailure extends RuntimeException {

    public AssertFailure(String message) {
        super(message);
    }

    public AssertFailure(Throwable cause) {
        super(cause);
    }

    public AssertFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
