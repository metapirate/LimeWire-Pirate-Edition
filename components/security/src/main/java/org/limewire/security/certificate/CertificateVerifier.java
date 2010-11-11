package org.limewire.security.certificate;

import java.security.cert.Certificate;

public interface CertificateVerifier {
    /**
     * Takes the given certificate, then loads the keystore and tries to
     * validate the chain of the certificate up to our ca.
     * 
     * @return true if the certificate is valid and within its valid date range.
     */
    boolean isValid(Certificate certificate) ;

}
