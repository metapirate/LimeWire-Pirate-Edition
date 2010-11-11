package org.limewire.http.auth;

import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.message.BasicHeader;
import org.limewire.util.StringUtils;

/**
 * Implements basic http authentication.
 * 
 * Parses http request for basic auth scheme headers, creates 
 * {@link UsernamePasswordCredentials} and authenticates them against
 * the given {@link Authenticator}.
 * 
 * This class is not threadsafe.
 */
public class BasicServerAuthScheme implements ServerAuthScheme {
    
    private final Authenticator authenticator;
    private boolean complete;

    public BasicServerAuthScheme(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void setComplete() {
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public Credentials authenticate(HttpRequest request) {
        Header authHeader = request.getFirstHeader(AUTH.WWW_AUTH_RESP);
        if(authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader.getValue());
            if(st.hasMoreTokens()) {
                if(st.nextToken().trim().equalsIgnoreCase("Basic")) {
                    if(st.hasMoreTokens()) {
                        byte [] userNamePassword = Base64.decodeBase64(StringUtils.toUTF8Bytes(st.nextToken().trim()));
                        Credentials credentials = new UsernamePasswordCredentials(StringUtils.getUTF8String(userNamePassword));
                        if(authenticator.authenticate(credentials)) {
                            return credentials;
                        }
                    }
                }
            } 
        }
        return null;
    }

    /**
     * Creates basic auth header with realm "secure".
     * See {@link ServerAuthScheme#createChallenge()}.
     */
    public Header createChallenge() {
        return new BasicHeader(AUTH.WWW_AUTH, "Basic realm=\"secure\"");
    }
}
