package org.limewire.core.api.library;

public class MetaDataException extends Exception {

    public MetaDataException(String message) {
        super(message);
    }
    
    public MetaDataException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MetaDataException(Throwable cause) {
        super(cause);
    }
}
