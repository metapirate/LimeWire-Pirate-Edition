package com.limegroup.gnutella.security;

import java.io.File;
import java.io.IOException;

/**
 * Reads a certificate from a given file. Also writes a certificate to a file.
 */
public interface FileCertificateReader {

    /**
     * Reads a certificate from <code>file</code>
     * @throws IOException if the file could not be read or the certificate
     * could not be parsed
     */
    Certificate read(File file) throws IOException;

    /**
     * Writes {@link Certificate#getCertificateString()} to <code>file</code>
     * @return true if successful
     */
    boolean write(Certificate certificate, File file);
}