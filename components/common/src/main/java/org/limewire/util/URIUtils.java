package org.limewire.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * Utilities for <code>URIs</code>.
 */
public class URIUtils {
    
//    private static final Log LOG = LogFactory.getLog(URIUtils.class);
    
    /**
     * Creates a <code>URI</code> from the input string.
     * The preferred way to invoke this method is with an URL-encoded string.
     * <p>
     * However, if the string has not been encoded, this method will encode it.
     * It is ambiguous whether a string has been encoded or not, which is why
     * it is preferred to pass in the string pre-encoded.
     * <p>
     * This method is useful when manipulating a URI and you don't know if it is 
     * encoded or not.
     * <p>
     * @param uriString the uri to be created
     * @throws URISyntaxException
     */
    public static URI toURI(final String uriString) throws URISyntaxException {
        URI uri;
        try {   
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            // the uriString was perhaps not encoded.
            // try to percent encode it.
            String encodedURIString = encodeUri(uriString);
            try {
                uri = new URI(encodedURIString);
            } catch (URISyntaxException e1) {
                // encoding the uriString didn't help.
                // this probably means there is something structurally
                // wrong with it.
                
                // NOTE: throwing the original exception.
                // initing with second Exception.  Not the normal
                // use case for initCause(), but this will at least capture both 
                // stack traces
                if(e.getCause() == null) {
                    e.initCause(e1);
                }
                throw e;
            }
        }
        return uri;
    }
    
    /**
     * Returns the port for the given URI. If no port can be found, it checks the scheme.
     * If the scheme is http port 80 is returned, if https 443.
     * <p>
     * -1 is returned if no port can be found.
     */
    public static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }
        return port;
    }
    
    /**
     * Percent encodes <code>uri</code> leaving slashes and other reserved 
     * characters untouched. This is the same as the Javascript implementation
     * of encodURI.
     */
    public static String encodeUri(String uri) throws URISyntaxException {
        return encode(uri, true);
    }
    
    /**
     * Percent-encodes part of a uri, encoding all reserved characters. This
     * is the same as the Javascript implementation of encodeURIComponent.
     */
    public static String encodeUriComponent(String uriComponent) throws URISyntaxException {
        return encode(uriComponent, false);
    }
    
    /**
     * Decodes <code>uri</code> replacing '+' with ' ' and percent encoded characters
     * with their utf equivalents.
     * 
     * @return copy of original uri if there no characters had to be decoded
     */
    public static String decodeToUtf8(String uri) throws URISyntaxException {
        try {
            return URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException iae) {
            throw new URISyntaxException(uri, "invalid url");
        }
    }
    
    /**
     * @return the canonical lower case host or null if <code>uri</code> does
     * not have a host
     */
    public static String getCanonicalHost(URI uri) {
        String host = uri.getHost();
        return host != null ? host.toLowerCase(Locale.US) : null;
    }
    
    /**
     * Code taken from rhino-1.7R1/src/org/mozilla/javascript/NativeGlobal.java 
     * and slightly adapted. It is released under the MPL 1.1 and GPL 2.0.
     */
    private static String encode(String str, boolean fullUri) throws URISyntaxException {
        byte[] utf8buf = null;
        StringBuilder sb = null;

        for (int k = 0, length = str.length(); k != length; ++k) {
            char c = str.charAt(k);
            if (encodeUnescaped(c, fullUri)) {
                if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(length + 3);
                    sb.append(str);
                    sb.setLength(k);
                    utf8buf = new byte[6];
                }
                if (0xDC00 <= c && c <= 0xDFFF) {
                    throw new URISyntaxException(str, c + " outside of valid range");
                }
                int value;
                if (c < 0xD800 || 0xDBFF < c) {
                    value = c;
                } else {
                    k++;
                    if (k == length) {
                        throw new URISyntaxException(str, "out of chars");
                    }
                    char c2 = str.charAt(k);
                    if (!(0xDC00 <= c2 && c2 <= 0xDFFF)) {
                        throw new URISyntaxException(str, "outside of valid range");
                    }
                    value = ((c - 0xD800) << 10) + (c2 - 0xDC00) + 0x10000;
                }
                int L = oneUcs4ToUtf8Char(utf8buf, value);
                assert utf8buf != null;
                for (int j = 0; j < L; j++) {
                    int d = 0xff & utf8buf[j];
                    sb.append('%');
                    sb.append(toHexChar(d >>> 4));
                    sb.append(toHexChar(d & 0xf));
                }
            }
        }
        return (sb == null) ? str : sb.toString();
    }

    private static char toHexChar(int i) {
        if (i >> 4 != 0) throw new RuntimeException();
        return (char)((i < 10) ? i + '0' : i - 10 + 'a');
    }

    private static boolean encodeUnescaped(char c, boolean fullUri) {
        if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')
            || ('0' <= c && c <= '9'))
        {
            return true;
        }
        if ("-_.!~*'()".indexOf(c) >= 0)
            return true;
        if (fullUri) {
            return URI_DECODE_RESERVED.indexOf(c) >= 0;
        }
        return false;
    }

    private static final String URI_DECODE_RESERVED = ";/?:@&=+$,#";

    /* Convert one UCS-4 char and write it into a UTF-8 buffer, which must be
    * at least 6 bytes long.  Return the number of UTF-8 bytes of data written.
    */
    private static int oneUcs4ToUtf8Char(byte[] utf8Buffer, int ucs4Char) {
        int utf8Length = 1;

        //JS_ASSERT(ucs4Char <= 0x7FFFFFFF);
        if ((ucs4Char & ~0x7F) == 0)
            utf8Buffer[0] = (byte)ucs4Char;
        else {
            int i;
            int a = ucs4Char >>> 11;
            utf8Length = 2;
            while (a != 0) {
                a >>>= 5;
                utf8Length++;
            }
            i = utf8Length;
            while (--i > 0) {
                utf8Buffer[i] = (byte)((ucs4Char & 0x3F) | 0x80);
                ucs4Char >>>= 6;
            }
            utf8Buffer[0] = (byte)(0x100 - (1 << (8-utf8Length)) + ucs4Char);
        }
        return utf8Length;
    }

}
