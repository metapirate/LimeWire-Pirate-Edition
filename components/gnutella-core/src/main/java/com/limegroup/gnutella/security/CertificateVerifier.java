package com.limegroup.gnutella.security;

import java.security.SignatureException;

/**
 * Verifies certificates.
 */
public interface CertificateVerifier {

    /**
     * This call is potentially blocking.
     * @return certificate if it is valid
     * @throws SignatureException if the certificate fails verification
     */
    Certificate verify(Certificate certificate) throws SignatureException;
    
}
