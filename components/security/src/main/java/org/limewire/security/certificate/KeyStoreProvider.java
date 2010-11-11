package org.limewire.security.certificate;

import java.io.IOException;
import java.security.KeyStore;

/**
 * Provides a wrapper to access the base LimeWire key store, which should
 * contain the ca.limewire.com root certificate as well as any other important
 * sub certificates. Implementations should NOT automatically check the validity
 * of the certificates stored within the key store, instead relying on consumer
 * classes to validate and call {@link #invalidateKeyStore()} if there is a
 * problem.
 */
public interface KeyStoreProvider {
    /**
     * @return the current instance of the LimeWire key store, which will be
     *         cached at the first call to this method.
     * @see #invalidateKeyStore()
     * @throws IOException if there is a problem loading or parsing the key
     *         store (file/network io, invalid certificates, or invalid
     *         algorithms)
     */
    KeyStore getKeyStore() throws IOException;

    /**
     * Causes this provider to delete any cached key store information, to be
     * re-retrieved and re-cached on the next call to {@link #getKeyStore()}.
     */
    void invalidateKeyStore();

    /**
     * @return true if the key store is cached in memory OR ANY OTHER LOCATION
     *         closer than the most-distant (most authoritative) source. For
     *         example, the most likely implementation will pull from a remote
     *         web server, but store the file on local disk for future access.
     *         So even though the system might not have the file in memory, this
     *         would count as cached.
     */
    boolean isCached();
}
