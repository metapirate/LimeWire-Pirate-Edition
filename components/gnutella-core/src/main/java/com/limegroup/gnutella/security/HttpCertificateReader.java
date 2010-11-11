package com.limegroup.gnutella.security;

import java.io.IOException;
import java.net.URI;

import org.limewire.io.IpPort;

/**
 * Reads a certificate from a uri.
 */
public interface HttpCertificateReader {

    /**
     * Reads a certificate from <code>uri</code>.
     * @param messageSource message source that will be passed to the server,
     * can be null
     * @throws IOException if the certificate could not be downloaded or parsed
     */
    Certificate read(URI uri, IpPort messageSource) throws IOException;

}