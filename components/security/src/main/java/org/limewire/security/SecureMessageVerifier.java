package org.limewire.security;

import java.security.PublicKey;

/**
 * Defines the interface to handle message verification.
 */
public interface SecureMessageVerifier {

    /** Queues this <code>SecureMessage</code> for verification. The callback 
     * will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc);

    /** 
     * Queues this <code>SecureMessage</code> for verification. The callback 
     * will be notified of success or failure.
     */
    public void verify(PublicKey pubKey, String algorithm, SecureMessage sm,
            SecureMessageCallback smc);

    /**
     * Enqueues a custom <code>Verifier</code>.
     */
    public void verify(Verifier verifier);

}