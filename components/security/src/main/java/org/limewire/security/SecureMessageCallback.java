package org.limewire.security;


/** Defines a callback interface that notifies an entity
 * that the security of the message has been processed.
 */
public interface SecureMessageCallback {
    public void handleSecureMessage(SecureMessage sm, boolean passed);
}
