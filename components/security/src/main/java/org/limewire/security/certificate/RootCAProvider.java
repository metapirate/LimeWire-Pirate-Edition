package org.limewire.security.certificate;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public interface RootCAProvider {
    /**
     * This method should do a lookup of the root certificate, validate that it
     * is truly the right certificate (checking hashes, signatures, etc) and
     * return. If it cannot validate the certificate, it should throw an
     * exception, never return null.
     * 
     * @return the root CA certificate that all LW certificates depend on.
     * @throws CertificateException if there is any unresolvable issue in
     *         finding the root CA, for instance a certificate coming out of the
     *         keystore that does not match our expected signature.
     */
    X509Certificate getCertificate() throws CertificateException;
}
