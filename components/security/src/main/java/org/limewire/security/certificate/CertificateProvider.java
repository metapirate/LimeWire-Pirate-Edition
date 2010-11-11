package org.limewire.security.certificate;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Provides a wrapper around the {@link KeyStoreProvider} and
 * {@link CertificateVerifier} classes to allow easy loading of certificates.
 */
public interface CertificateProvider {

    /**
     * @return the certificate from the default keystore with the given alias.
     *         This certificate will have been verified by the CA prior to this
     *         method returning, so the certificate can be trusted.
     * @throws CertificateException if there is a problem retrieving the given
     *         certificate.
     */
    Certificate getCertificate(String alias) throws CertificateException;
}
