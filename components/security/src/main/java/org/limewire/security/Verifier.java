package org.limewire.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SecureMessage.Status;

/**
 * An abstract base class to verify a {@link SecureMessage}. The
 * callback, a {@link SecureMessageCallback} receives notification if the 
 * message is verified.
 */
public abstract class Verifier implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(Verifier.class);
    
    private SecureMessage message;
    
    private SecureMessageCallback callback;
    
    public Verifier(SecureMessage message, SecureMessageCallback callback) {
        this.message = message;
        this.callback = callback;
    }
    
    /**
     * Returns the <code>PublicKey</code> that is used to verify the signature.
     */
    public abstract PublicKey getPublicKey();
    
    /**
     * Returns the algorithm of the signature.
     */
    public abstract String getAlgorithm();
    
    /**
     * Returns the <code>SecureMessage</code>.
     */
    public SecureMessage getSecureMessage() {
        return message;
    }
    
    /**
     * Returns the <code>SecureMessageCallback</code>.
     */
    public SecureMessageCallback getSecureMessageCallback() {
        return callback;
    }
    
    /** Does the verification. */
    public void run() {
        
        SecureMessage message = getSecureMessage();
        SecureMessageCallback callback = getSecureMessageCallback();
        
        PublicKey pubKey = getPublicKey();
        String algorithm = getAlgorithm();
        
        if(pubKey == null) {
            LOG.warn("Cannot verify message without a public key.");
            message.setSecureStatus(Status.INSECURE);
            callback.handleSecureMessage(message, false);
            return;
        }
        
        byte[] signature = message.getSecureSignature();
        if(signature == null) {
            LOG.warn("Cannot verify message without a signature.");
            message.setSecureStatus(Status.INSECURE);
            callback.handleSecureMessage(message, false);
            return;
        }
        
        try {
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(pubKey);
            message.updateSignatureWithSecuredBytes(verifier);
            if(verifier.verify(signature)) {
                message.setSecureStatus(Status.SECURE);
                callback.handleSecureMessage(message, true);
                return;
            }
            // fallthrough on not secure & failures to set failed.
        } catch (NoSuchAlgorithmException nsax) {
            LOG.error("No alg.", nsax);
        } catch (InvalidKeyException ikx) {
            LOG.error("Invalid key", ikx);
        } catch (SignatureException sx) {
            LOG.error("Bad sig", sx);
        } catch (ClassCastException ccx) {
            LOG.error("bad cast", ccx);
        }
        
        message.setSecureStatus(Status.FAILED);
        callback.handleSecureMessage(message, false);
    }
}
