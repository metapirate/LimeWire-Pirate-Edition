package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SignatureException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Reads valid certificates from file, http and stores them to the same file.
 */
public class CertificateProviderImpl implements CertificateProvider {

    private static final Log LOG = LogFactory.getLog(CertificateProviderImpl.class);
    
    private final FileCertificateReader fileCertificateReader;
    private final HttpCertificateReader httpCertificateReader;
    private final CertificateVerifier certificateVerifier;
    
    private AtomicReference<Certificate> validCertificate = new AtomicReference<Certificate>(null);

    private final File file;

    private final URI uri;
    
    private final AtomicBoolean httpDone = new AtomicBoolean(false);
    
    /**
     * @param fileCertificateReader the file certificate reader used for reading
     * certificates from disk and for storing them to disk.
     * @param httpCertificateReader the http certificate reader used for 
     * retrieving certificates from a trusted http server
     * @param certificateVerifier verifier to verify all read and set certificates
     * @param file the file to read certificates from and write them to
     * @param uri the uri certificates are downloaded from over http
     */
    public CertificateProviderImpl(FileCertificateReader fileCertificateReader,
            HttpCertificateReader httpCertificateReader, 
            CertificateVerifier certificateVerifier,
            File file, URI uri) {
        this.fileCertificateReader = fileCertificateReader;
        this.httpCertificateReader = httpCertificateReader;
        this.certificateVerifier = certificateVerifier;
        this.file = file;
        this.uri = uri;
    }
    
    private Certificate getFromFile() {
        try {
            return certificateVerifier.verify(fileCertificateReader.read(file));
        } catch (IOException e) {
            LOG.debugf(e, "certificate from invalid file: {0}", file);
        } catch (SignatureException e) {
            LOG.debugf(e, "certificate from file {0} invalid", file);
        }
        return null;
    }

    @Override
    public void set(Certificate certificate) {
        LOG.debugf("setting certificate: {0}", certificate);
        try { 
            Certificate localCopy = validCertificate.get();
            if (localCopy == null || certificate.getKeyVersion() > localCopy.getKeyVersion()) {
                validCertificate.set(certificateVerifier.verify(certificate));
                fileCertificateReader.write(certificate, file);
            } else {
                LOG.debugf("certificate version not greater than local one: {0}", certificate);
            }
        } catch (SignatureException se) {
            LOG.debugf(se, "certificate invalid {0} ", certificate);
        }
    }

    /**
     * Potentially blocking call, accessing the disk and making network connections.
     * <p>
     * If a valid certificate is loaded, it will return the valid certificate.
     * Otherwise it will try to read a certificate from disk. If this fails it
     * will resort to http.
     * 
     * @returns {@link NullCertificate} if no valid certificate could be retrieved
     * from any of the sources
     */
    @Override
    public Certificate get() {
        Certificate copy = validCertificate.get();
        if (copy != null) {
            return copy;
        }
        validCertificate.compareAndSet(null, getFromFile());
        copy = validCertificate.get();
        if (copy != null) {
            return copy;
        }
        return new NullCertificate();
    }
    
    @Override
    public Certificate get(int keyVersion, IpPort messageSource) {
        Certificate copy = validCertificate.get();
        if (copy == null) {
            validCertificate.compareAndSet(null, getFromFile());
        }
        copy = validCertificate.get();
        if (copy != null && copy.getKeyVersion() >= keyVersion) {
            return copy;
        }
        return getFromHttp(messageSource);
    }

    Certificate getFromHttp(IpPort messageSource) {
        if (httpDone.compareAndSet(false, true)) {
            try {
                LOG.debug("getting certifcate from http");
                return certificateVerifier.verify(httpCertificateReader.read(uri, messageSource));
            } catch (IOException ie) {
                LOG.debugf(ie, "certificate from invalid url: {0}", uri);
            } catch (SignatureException e) {
                LOG.debugf(e, "certificate from http invalid: {0}", uri);
            }
            return new NullCertificate();
        } else {
            Certificate copy = validCertificate.get();
            if (copy != null) {
                return copy;
            } else {
                return new NullCertificate();
            }
        }
    }
    
}
