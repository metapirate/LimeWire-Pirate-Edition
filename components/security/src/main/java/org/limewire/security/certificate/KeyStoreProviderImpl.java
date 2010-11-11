package org.limewire.security.certificate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.limewire.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Initially loads the key store from the network, and then caches it to disk in
 * the preferences directory.
 */
@Singleton
class KeyStoreProviderImpl implements KeyStoreProvider {
    private volatile KeyStore keyStore = null;

    private final Provider<HttpClient> httpClient;
    private volatile File keyStoreLocation;
    private volatile char[] keyStorePassword;

    private static final Log LOG = LogFactory.getLog(KeyStoreProviderImpl.class);

    @Inject
    KeyStoreProviderImpl(Provider<HttpClient> httpClient) {
        this.httpClient = httpClient;
        this.keyStoreLocation = CertificateTools.getKeyStoreLocation();
        this.keyStorePassword = CertificateTools.getKeyStorePassword();
    }
    
    void setKeyStoreLocation(File location) {
        this.keyStoreLocation = location;
    }
    
    void setKeyStorePassword(char[] password) {
        this.keyStorePassword = password;
    }

    public KeyStore getKeyStore() throws IOException {
        KeyStore ks = keyStore;
        
        if (isValid(ks)) {
            return ks;
        }
        
        // Not cached, try to load into memory from disk...
        try {
            ks = getKeyStoreFromDisk();
            keyStore = ks;
            return ks;
        } catch (IOException ex) {
            // Failed to load from disk, ignore and try to pull from network...
            LOG.debug("IOException trying to load keystore from disk.", ex);
        }

        // Not on disk, try to load from network, load into memory and save to
        // disk.
        OutputStream out = null;
        try {
            ks = getKeyStoreFromNetwork();
            keyStore = ks;
            keyStoreLocation.getParentFile().mkdirs();
            out = new FileOutputStream(keyStoreLocation);
            ks.store(out, keyStorePassword);
            return ks;
        } catch (KeyStoreException ex) {
            throw IOUtils.getIOException("KeyStoreException while saving keystore: ", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException while saving keystore: ", ex);
        } catch (CertificateException ex) {
            throw IOUtils.getIOException("CertificateException while saving keystore: ", ex);
        } finally {
            IOUtils.close(out);
        }
    }

    KeyStore getKeyStoreFromNetwork() throws IOException {
        HttpClient client = httpClient.get();
        HttpGet get = new HttpGet(CertificateTools.getKeyStoreURI());
        

        KeyStore newKeyStore;
        try {
            newKeyStore = KeyStore.getInstance("jks");
        } catch (KeyStoreException ex) {
            throw new IOException("KeyStoreException instantiating keystore: " + ex.getMessage());
        }
        HttpResponse response = null;
        try {
            response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                try {
                    newKeyStore.load(response.getEntity().getContent(), keyStorePassword);
                } catch (NoSuchAlgorithmException ex) {
                    throw IOUtils.getIOException(
                            "NoSuchAlgorithmException while parsing keystore: ", ex);
                } catch (CertificateException ex) {
                    throw IOUtils.getIOException("CertificateException while parsing keystore: ",
                            ex);
                }
                return newKeyStore;
            } else {
                throw new IOException("Failed to download new keystore ("
                        + CertificateTools.getKeyStoreURI().toString() + "): "
                        + response.getStatusLine());
            }
        } finally {
            if(response != null && response.getEntity() != null) {
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    LOG.error(e);
                }            
            }
        }
    }

    /**
     * @return the loaded and initialized key store, or throws an exception
     * @throws IOException if loading fails for any reason (missing file,
     *         certificate issues, etc)
     */
    KeyStore getKeyStoreFromDisk() throws IOException {
        InputStream in = null;
        try {
            KeyStore newKeyStore = KeyStore.getInstance("jks");
            in = new BufferedInputStream(new FileInputStream(keyStoreLocation));
            newKeyStore.load(in, keyStorePassword);
            return newKeyStore;
        } catch (KeyStoreException ex) {
            throw IOUtils.getIOException("KeyStoreException while creating keystore in memory: ",
                    ex);
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException while parsing keystore: ", ex);
        } catch (CertificateException ex) {
            throw IOUtils.getIOException("CertificateException while parsing keystore: ", ex);
        } finally {
            IOUtils.close(in);
        }
    }

    /*
     * Invalidates by dereferencing our in-memory copy as well as deleting the
     * on-disk version.
     */
    public void invalidateKeyStore() {
        keyStore = null;
        keyStoreLocation.delete();
    }

    private boolean isValid(KeyStore ks) {
        if (ks == null)
            return false;
        try {
            if (ks.size() > 0)
                return true;
        } catch (KeyStoreException ex) {
            // keyStore is non-null but not initialized, treat it as null
        }
        return false;
    }

    public boolean isCached() {
        if (isValid(keyStore))
            return true;
        if (keyStoreLocation.exists()) {
            try {
                if (isValid(getKeyStoreFromDisk()))
                    return true;
            } catch (IOException ignored) {
                // File exists but doesn't look to be valid...
            }
        }
        return false;
    }

}
