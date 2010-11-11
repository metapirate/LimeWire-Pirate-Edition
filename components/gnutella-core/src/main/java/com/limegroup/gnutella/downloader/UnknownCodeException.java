package com.limegroup.gnutella.downloader;

import java.io.IOException;

/**
 * Thrown when the response code is unknown.
 */
public class UnknownCodeException extends IOException {
    private int code;
    public UnknownCodeException() {
        super("unknown code");
    }

    public UnknownCodeException(int code) {
        super("unknown: " + code);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
