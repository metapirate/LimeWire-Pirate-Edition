package com.limegroup.gnutella.dime;

import java.io.IOException;

public class DIMEException extends IOException {

    public DIMEException() {
    }

    public DIMEException(String s) {
        super(s);
    }
    
    public DIMEException(Throwable cause) {
        super(cause.getMessage());
        initCause(cause);
    }

}
