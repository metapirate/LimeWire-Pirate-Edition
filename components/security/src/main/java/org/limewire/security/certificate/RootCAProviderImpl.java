package org.limewire.security.certificate;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RootCAProviderImpl implements RootCAProvider {
    KeyStoreProvider keyStoreProvider;

    HashCalculator hashCalculator;

    HashLookupProvider hashLookupProvider;

    @Inject
    public RootCAProviderImpl(KeyStoreProvider keyStoreProvider, HashCalculator hashCalculator,
            HashLookupProvider hashLookupProvider) {
        this.keyStoreProvider = keyStoreProvider;
        this.hashCalculator = hashCalculator;
        this.hashLookupProvider = hashLookupProvider;
    }

    public X509Certificate getCertificate() throws CertificateException {
        try {
            return getCertificateNoRetry();
        } catch (CertificateException ex) {
            // Failed, try invalidating the keystore and trying again...
            keyStoreProvider.invalidateKeyStore();
            return getCertificateNoRetry();
        }
    }

    /**
     * Same contract as {@link #getCertificate()}, but will not try to
     * invalidate and reacquire a keystore.
     * 
     * @see #getCertificate()
     */
    private X509Certificate getCertificateNoRetry() throws CertificateException {
        final String CA_ALIAS = CertificateProps.getCACertAlias();
        try {
            KeyStore ks = keyStoreProvider.getKeyStore();
            X509Certificate certificate = (X509Certificate) ks.getCertificate(CA_ALIAS);
            String expectedHash = hashLookupProvider.lookup(CertificateProps.getCAHashLookupKey());
            // If we can't get the hash, we assume the certificate is ok...
            if (expectedHash != null) {
                String actualHash = CertificateTools
                        .getCertificateHash(certificate, hashCalculator);
                if (!expectedHash.equalsIgnoreCase(actualHash))
                    throw new CertificateException("CA-hash does not match expected. actual->'"
                            + actualHash + "'!='" + expectedHash + "'");
            }
            return certificate;
        } catch (IOException ex) {
            throw new CertificateException("IOException getting certificate", ex);
        } catch (KeyStoreException ex) {
            throw new CertificateException("KeyStoreException getting certificate", ex);
        }
    }

}
