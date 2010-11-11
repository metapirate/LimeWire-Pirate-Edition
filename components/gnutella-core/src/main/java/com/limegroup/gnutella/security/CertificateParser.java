package com.limegroup.gnutella.security;

import java.io.IOException;

/**
 * Parses a string into a certificate. The string should correspond to
 * {@link Certificate#getCertificateString()}.
 * <p>
 * There is no assumption as to whether the parsed certificate is verified 
 * or not.
 */
public interface CertificateParser {

    /**
     *  Parses a string into a certificate. The string should correspond to
     * {@link Certificate#getCertificateString()}.
     * <p>
     * There is no assumption as to whether the parsed certificate is verified 
     * or not.
     * @throws IOException if the data is invalid for some reason
     */
    Certificate parseCertificate(String data) throws IOException;
}
