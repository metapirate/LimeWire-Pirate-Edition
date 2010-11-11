package org.limewire.rest.oauth;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.limewire.rest.RestUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Implementation of OAuthValidator used to validate requests using the OAuth
 * protocol.  At present, only the HMAC-SHA1 signature method is supported. 
 */
public class OAuthValidatorImpl implements OAuthValidator {

    private static final String VERSION = "1.0";
    private static final String SIG_METHOD = "HMAC-SHA1";
    private static final String MAC_NAME = "HmacSHA1";
    /** Maximum nonce age is 2 minutes. */
    private static final long NONCE_AGE = 2 * 60 * 1000L;

    private final String baseUrl;
    private final String consumerSecret;
    private final String tokenSecret;
    private final Map<String, Long> timestamps;
    private final NonceTracker nonceTracker;

    /**
     * Constructs an OAuthValidator with the specified base URL, port number,
     * and consumer secret.  By default, the token secret is an empty string
     * for use with two-legged OAuth.
     */
    @Inject
    public OAuthValidatorImpl(
            @Assisted("baseUrl") String baseUrl,
            @Assisted int port,
            @Assisted("secret") String secret) {
        this.baseUrl = createBaseUrl(baseUrl, port);
        this.consumerSecret = secret;
        this.tokenSecret = "";
        this.timestamps = new ConcurrentHashMap<String, Long>();
        this.nonceTracker = new NonceTracker();
    }
    
    /**
     * Creates the base URL using the specified URL and port number.  The
     * default port numbers 80 (http) or 443 (https) are ignored because 
     * OAuth specifies that these must be excluded from the signature base 
     * string.
     */
    private String createBaseUrl(String baseUrl, int port) {
        // Split protocol and domain in URL string.
        int pos = baseUrl.indexOf("//");
        String protocol = (pos < 0) ? "" : baseUrl.substring(0, pos + 2);
        String domain = (pos < 0) ? baseUrl : baseUrl.substring(pos + 2);
        
        // Split uri from domain.
        int uriPos = domain.indexOf('/');
        String uri = (uriPos < 0) ? "" : domain.substring(uriPos);
        domain = (uriPos < 0) ? domain : domain.substring(0, uriPos);
        
        // Remove old port number.
        int portPos = domain.indexOf(':');
        domain = (portPos < 0) ? domain : domain.substring(0, portPos);
        
        // Add port number to domain.
        if ((port != 80) && (port != 443)) {
            domain = domain + ':' + port;
        }
        
        // Recreate url string.
        return protocol + domain + uri;
    }
    
    @Override
    public void validateRequest(OAuthRequest request) throws OAuthException {
        long currentMsec = System.currentTimeMillis();
        validateParameters(request);
        validateVersion(request);
        validateTimestamp(request, currentMsec);
        validateNonce(request, currentMsec);
        validateSignatureMethod(request);
        validateSignature(request);
    }
    
    /**
     * Validates the required OAuth parameters in the specified request.
     */
    private void validateParameters(OAuthRequest request) throws OAuthException {
        if (request.getParameter(OAuthRequest.OAUTH_CONSUMER_KEY) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_CONSUMER_KEY);
        }
        if (request.getParameter(OAuthRequest.OAUTH_SIGNATURE_METHOD) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_SIGNATURE_METHOD);
        }
        if (request.getParameter(OAuthRequest.OAUTH_SIGNATURE) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_SIGNATURE);
        }
        if (request.getParameter(OAuthRequest.OAUTH_TIMESTAMP) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_TIMESTAMP);
        }
        if (request.getParameter(OAuthRequest.OAUTH_NONCE) == null) {
            throw new OAuthException("Missing " + OAuthRequest.OAUTH_NONCE);
        }
    }
    
    /**
     * Validates the OAuth version in the specified request.  The version is
     * an optional parameter.
     */
    private void validateVersion(OAuthRequest request) throws OAuthException {
        String version = request.getParameter(OAuthRequest.OAUTH_VERSION);
        if ((version != null) && !VERSION.equalsIgnoreCase(version)) {
            throw new OAuthException("Invalid OAuth version");
        }
    }
    
    /**
     * Validates the timestamp in the specified request.  According to OAuth 
     * Core 1.0 Revision A, Section 8, the timestamp is in seconds, and must
     * be equal to or greater than the timestamp in previous requests.
     */
    private void validateTimestamp(OAuthRequest request, long currentMsec) throws OAuthException {
        // Get timestamp in seconds.
        long timestamp = request.getParameter(OAuthRequest.OAUTH_TIMESTAMP, 0);
        if (timestamp <= 0) {
            throw new OAuthException("Invalid OAuth timestamp");
        }
        
        // Get previous timestamp.
        String consumerKey = request.getParameter(OAuthRequest.OAUTH_CONSUMER_KEY);
        Long prevTime = timestamps.get(consumerKey); 
        
        // Timestamp cannot be earlier than last request. 
        if ((prevTime != null) && (prevTime.longValue() > timestamp)) {
            throw new OAuthException("OAuth timestamp earlier than previous");
        }
        
        timestamps.put(consumerKey, timestamp);
    }
    
    /**
     * Validates the nonce in the specified request.  According to OAuth 
     * Core 1.0 Revision A, Section 8, the nonce must be unique for all 
     * requests with the same timestamp.
     */
    private void validateNonce(OAuthRequest request, long currentMsec) throws OAuthException {
        // Get request parameters.
        long timestamp = request.getParameter(OAuthRequest.OAUTH_TIMESTAMP, 0);
        String consumerKey = request.getParameter(OAuthRequest.OAUTH_CONSUMER_KEY);
        String nonceStr = request.getParameter(OAuthRequest.OAUTH_NONCE);
        
        // Nonce must be unique.
        Nonce nonce = new Nonce(timestamp, consumerKey, nonceStr);
        boolean valid = nonceTracker.add(nonce);
        if (!valid) {
            throw new OAuthException("OAuth nonce already used");
        }
        
        // Remove old nonces.
        nonceTracker.removeOldNonces(currentMsec);
    }
    
    /**
     * Validates the OAuth signature method in the specified request.  Only
     * HMAC-SHA1 is supported.
     */
    private void validateSignatureMethod(OAuthRequest request) throws OAuthException {
        String sigMethod = request.getParameter(OAuthRequest.OAUTH_SIGNATURE_METHOD);
        if (!SIG_METHOD.equalsIgnoreCase(sigMethod)) {
            throw new OAuthException("Unsupported OAuth signature method");
        }
    }
    
    /**
     * Validates the OAuth signature in the specified request.
     */
    private void validateSignature(OAuthRequest request) throws OAuthException {
        // Retrieve request signature.
        String oauthSignature = request.getParameter(OAuthRequest.OAUTH_SIGNATURE);
        byte[] oauthBytes = Base64.decodeBase64(oauthSignature.getBytes());
        
        try {
            // Create base string and compute signature.
            String baseString = OAuthUtils.createSignatureBaseString(request, baseUrl);
            byte[] signatureBytes = computeSignature(baseString);
            
            // Compare signatures.
            if (!Arrays.equals(oauthBytes, signatureBytes)) {
                throw new OAuthException("Invalid OAuth signature");
            }
            
        } catch (GeneralSecurityException ex) {
            throw new OAuthException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new OAuthException(ex); 
        }
    }
    
    /**
     * Computes the signature for the specified base string.  The HMAC-SHA1 
     * signature method is used.
     */
    private byte[] computeSignature(String baseString)
        throws GeneralSecurityException, UnsupportedEncodingException {
        
        // Create key.
        String keyString = RestUtils.percentEncode(consumerSecret) + '&' + RestUtils.percentEncode(tokenSecret);
        byte[] keyBytes = StringUtils.toUTF8Bytes(keyString);
        SecretKey key = new SecretKeySpec(keyBytes, MAC_NAME);

        // Compute signature using HmacSHA1. 
        Mac mac = Mac.getInstance(MAC_NAME);
        mac.init(key);
        byte[] text = StringUtils.toUTF8Bytes(baseString);
        return mac.doFinal(text);
    }
    
    /**
     * Representation of a nonce.  Each timestamp/consumer key must use a 
     * unique nonce string.
     */
    private static class Nonce {
        private final long creationTime;
        private final long timestamp;
        private final String consumerKey;
        private final String nonce;
        
        public Nonce(long timestamp, String consumerKey, String nonce) {
            this.creationTime = System.currentTimeMillis();
            this.timestamp = timestamp;
            this.consumerKey = consumerKey;
            this.nonce = nonce;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Nonce) {
                Nonce n2 = (Nonce) obj;
                return (timestamp == n2.timestamp) && 
                    consumerKey.equals(n2.consumerKey) &&
                    nonce.equals(n2.nonce);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + consumerKey.hashCode();
            result = 31 * result + nonce.hashCode();
            return result;
        }
    }
    
    /**
     * Tracker to maintain Nonce values.  Each Nonce must be unique.  We also
     * order nonces by their creation time so we can easily remove old values.
     */
    private static class NonceTracker {
        private final Set<Nonce> nonces;
        private final Set<Nonce> orderedNonces;
        
        public NonceTracker() {
            nonces = new HashSet<Nonce>();
            orderedNonces = new TreeSet<Nonce>(new Comparator<Nonce>() {
                @Override
                public int compare(Nonce o1, Nonce o2) {
                    long time1 = o1.getCreationTime();
                    long time2 = o2.getCreationTime();
                    int result = (time1 < time2) ? -1 : ((time1 > time2) ? 1 : 0);
                    return result;
                }
            });
        }
        
        public boolean add(Nonce nonce) {
            synchronized(nonces) {
                boolean valid = nonces.add(nonce);
                if (valid) {
                    orderedNonces.add(nonce);
                }
                return valid;
            }
        }
        
        public void removeOldNonces(long currentMsec) {
            // Calculate oldest creation time.
            long minMsec = currentMsec - NONCE_AGE;

            synchronized(nonces) {
                // Remove old nonces from cache.  Nonces are stored in order of
                // creation time so we can easily remove items that are too old.
                for (Iterator<Nonce> iter = orderedNonces.iterator(); iter.hasNext();) {
                    Nonce nonce = iter.next();
                    if (minMsec < nonce.getCreationTime()) {
                        break;
                    }
                    nonces.remove(nonce);
                    iter.remove();
                }
            }
        }
    }
}
