package org.limewire.xmpp.client.impl.messages;

/**
 * Exception to be thrown if an invalid IQ was parsed. Is expected to be
 * caught.
 */
public class InvalidIQException extends Exception {

    public InvalidIQException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidIQException(String message) {
        super(message);
    }    
    
    
}
