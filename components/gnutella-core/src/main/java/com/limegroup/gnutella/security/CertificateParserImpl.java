package com.limegroup.gnutella.security;

import java.io.IOException;
import java.security.PublicKey;

import org.limewire.security.SignatureVerifier;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

/**
 * Parses certificate strings of the following format:
 * <pre>
 * base32(signature)|key version as integer literal|base32(X509 encoded DSA public key)
 * </pre>
 */
public class CertificateParserImpl implements CertificateParser {

    
    public Certificate parseCertificate(String contents) throws IOException {
        String[] parts = contents.split("\\|");
        if (parts.length != 3) {
            throw new IOException(parts.length + " invalid data format: " + contents);
        }
        byte[] signature = Base32.decode(parts[0]);
        byte[] signedPayload = extractSignedPayload(contents);
        int keyVersion;
        try { 
            keyVersion = Integer.parseInt(parts[1]);
        } catch (NumberFormatException nfe) {
            throw new IOException("Could not parse key version");
        }
        PublicKey publicKey = SignatureVerifier.readKey(parts[2], "DSA");
        if (publicKey == null) {
            throw new IOException("invalid public key");
        }
        return new CertificateImpl(signature, signedPayload, keyVersion, publicKey, contents);
    }

    static byte[] extractSignedPayload(String contents) throws IOException {
        int pipe = contents.indexOf('|');
        if (pipe < 0 || pipe == contents.length() - 1) {
            throw new IOException("invalid contents: " + contents);
        }
        return StringUtils.toUTF8Bytes(contents.substring(pipe + 1));
    }
}
