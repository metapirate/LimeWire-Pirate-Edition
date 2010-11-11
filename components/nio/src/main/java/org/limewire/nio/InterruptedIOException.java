package org.limewire.nio;

class InterruptedIOException extends java.io.InterruptedIOException {
    

    InterruptedIOException(InterruptedException ix) {
        super();
        initCause(ix);
    }
    
}    