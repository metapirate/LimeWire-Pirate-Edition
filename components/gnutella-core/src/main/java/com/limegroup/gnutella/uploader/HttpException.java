package com.limegroup.gnutella.uploader;

import java.io.IOException;

/**
 * HttpException that carries an HTTP error code.
 */
public class HttpException extends IOException {

    private final int errorCode;

    public HttpException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
}
