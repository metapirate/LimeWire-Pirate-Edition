package org.limewire.security.certificate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import org.limewire.util.CommonUtils;

public class CertificateTools {
    /**
     * Takes a byte array and returns a hex string encoded two chars per byte
     * (00-FF), all uppercase.
     */
    public static String encodeBytesToString(byte bytes[]) {
        StringBuilder string = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // look up high nibble char
            string.append(HEX_CHARS[(b & 0xf0) >>> 4]);
            // look up low nibble char
            string.append(HEX_CHARS[b & 0x0f]);
        }
        return string.toString();
    }

    public static String getCertificateHash(Certificate certificate, HashCalculator hashCalculator)
            throws CertificateEncodingException {
        return encodeBytesToString(hashCalculator.calculate(certificate.getEncoded()));
    }

    // table to convert a nibble to a hex char.
    private final static char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * @return the full path to the limewire.keystore file, within the
     *         certificate settings directory.
     * @see #getCertificateSettingsDirectory()
     */
    public static File getKeyStoreLocation() {
        return new File(getCertificateSettingsDirectory(), "limewire.keystore");
    }

    /**
     * @return the directory within the user settings directory to store
     *         certificate info
     * @see CommonUtils#getUserSettingsDir()
     */
    public static File getCertificateSettingsDirectory() {
        return new File(CommonUtils.getUserSettingsDir(), "certificate/");
    }

    public static URI getKeyStoreURI() {
        try {
            return new URI(CertificateProps.getKeyStoreURLString());
        } catch (URISyntaxException ex) {
            throw new RuntimeException("MalformedURL '" + CertificateProps.getKeyStoreURLString()
                    + "'", ex);
        }
    }

    public static char[] getKeyStorePassword() {
        return "".toCharArray();
    }

}
