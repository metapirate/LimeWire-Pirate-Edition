package org.limewire.security.certificate;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import com.google.inject.Inject;

public class CertificateProviderImpl implements CertificateProvider {
    private KeyStoreProvider keyStoreProvider;

    private CertificateVerifier certificateVerifier;

    @Inject
    public CertificateProviderImpl(KeyStoreProvider keyStoreProvider,
            CertificateVerifier certificateVerifier) {
        this.keyStoreProvider = keyStoreProvider;
        this.certificateVerifier = certificateVerifier;
    }

    public Certificate getCertificate(String alias) throws CertificateException {
        try {
            KeyStore ks = keyStoreProvider.getKeyStore();
            Certificate certificate = ks.getCertificate(alias);
            if (certificate == null)
                throw new CertificateException("Unable to locate certificate '" + alias + "'");
            if (certificateVerifier.isValid(certificate))
                return certificate;
            throw new CertificateException("Certificate failed validation.");
        } catch (KeyStoreException ex) {
            throw new CertificateException("KeyStoreException getting certificate.", ex);
        } catch (IOException ex) {
            throw new CertificateException("IOException while getting certificate.", ex);
        }
    }
}
