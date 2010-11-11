package org.limewire.security;

import java.io.IOException;

/**
 * Thrown by a {@link SecurityToken} when a security token is 
 * created with invalid data. For example, throw 
 * <code>InvalidSecurityTokenException</code> upon invalid data from the 
 * network.
 */
public class InvalidSecurityTokenException extends IOException {

    public InvalidSecurityTokenException(String message) {
        super(message);
    }
    
}
