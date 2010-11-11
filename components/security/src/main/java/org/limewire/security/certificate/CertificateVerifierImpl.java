package org.limewire.security.certificate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CertificateVerifierImpl implements CertificateVerifier {
    
    private static final Log LOG = LogFactory.getLog(CertificateVerifierImpl.class);

    private KeyStoreProvider keyStoreProvider;

    private RootCAProvider rootCAProvider;

    @Inject
    public CertificateVerifierImpl(KeyStoreProvider keyStoreProvider, RootCAProvider rootCAProvider) {
        this.keyStoreProvider = keyStoreProvider;
        this.rootCAProvider = rootCAProvider;
    }

    public boolean isValid(Certificate certificate) {
        KeyStore ks;
        try {
            ks = keyStoreProvider.getKeyStore();
        } catch (IOException ex) {
            LOG.error("IOException getting keyStore", ex);
            return false;
        }
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");

            List<Certificate> certList = new ArrayList<Certificate>();
            certList.add(certificate);
            populateCertList(ks, certificate, certList);
            CertPath certPath = certFactory.generateCertPath(certList);
            // Set the Trust anchor
            TrustAnchor anchor = new TrustAnchor(rootCAProvider.getCertificate(), null);
            // Set the PKIX parameters
            PKIXParameters params = new PKIXParameters(Collections.singleton(anchor));
            params.setRevocationEnabled(false);
            // Validate and obtain results
            try {
                certPathValidator.validate(certPath, params);
            } catch (CertPathValidatorException ex) {
                LOG.error("Validation failure, cert[" + ex.getIndex() + "] :", ex);
                return false;
            }
        } catch (GeneralSecurityException ex) {
            LOG.error("CertificateException caught.", ex);
            return false;
        }
        return true;
    }

    /**
     * Looks at the certificate, looks up the issuer, and tries to find it in
     * the key store. When found, adds it to the end of the list. Then calls
     * itself recursively until it reaches the top of the signing chain.
     */
    private void populateCertList(KeyStore keyStore, Certificate certificate,
            List<Certificate> certs) {
        // We expect these to be x.509, otherwise we just return
        if (certificate instanceof X509Certificate) {
            X509Certificate x509 = (X509Certificate) certificate;
            String dn = x509.getIssuerDN().getName();
            // Find the 'CN='
            String cn = "";
            for (String token : dn.split(",")) {
                if (token.trim().startsWith("CN=")) {
                    cn = token.trim().substring(3);
                    break;
                }
            }
            // Break if this cert is self-signed, indicates it's the top of the
            // chain
            if (x509.getIssuerDN().getName().equals(x509.getSubjectDN().getName()))
                return;
            // System.out.println(cn);
            try {
                Certificate issuerCert = keyStore.getCertificate(cn);
                if (issuerCert == null) {
                    LOG.error("Could not find certificate alias '" + cn + "'");
                    return;
                }
                certs.add(issuerCert);
                populateCertList(keyStore, issuerCert, certs);
            } catch (KeyStoreException ex) {
                LOG.error("KeyStoreException caught while walking chain.", ex);
            }
        }
    }
}
