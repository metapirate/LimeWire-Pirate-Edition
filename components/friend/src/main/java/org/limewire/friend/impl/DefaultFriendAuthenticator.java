package org.limewire.friend.impl;

import org.apache.http.auth.Credentials;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.impl.feature.AuthTokenImpl;
import org.limewire.friend.impl.feature.AuthTokenRegistry;
import org.limewire.http.auth.Authenticator;
import org.limewire.http.auth.AuthenticatorRegistry;
import org.limewire.inject.EagerSingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.SHA1;
import org.limewire.security.SecurityUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

/**
 * Handles authentication and creation of user passwords for the friend component.
 * 
 * The class is inherently stateless except for a per session random seed.
 */
@EagerSingleton
class DefaultFriendAuthenticator implements Authenticator, AuthTokenRegistry {

    private final static Log LOG = LogFactory.getLog(DefaultFriendAuthenticator.class);
    
    /**
     * Per session random seed.
     */
    private final byte[] seed = new byte[SHA1.HASH_LENGTH];
    
    { 
        SecurityUtils.createSecureRandomNoBlock().nextBytes(seed);
    }
    
    @Override
    @Inject
    public void register(AuthenticatorRegistry registry) {
        registry.register(this);
    }

    /**
     * Returns auth token for <code>userId</code>.
     */
    public AuthToken getAuthToken(String userId) {
        SHA1 sha1 = new SHA1();
        byte[] hash = sha1.digest(StringUtils.toUTF8Bytes(userId));
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte)(hash[i] ^ seed[i]);
        }
        sha1.reset();
        // digest again, to make the seed irreconstructible
        return new AuthTokenImpl(sha1.digest(hash));
    }

    @Override
    public boolean authenticate(Credentials credentials) {
        // password can be null
        String password = credentials.getPassword();
        if (password == null) {
            return false;
        }
        AuthToken received = new AuthTokenImpl(password);
        AuthToken local = getAuthToken(credentials.getUserPrincipal().getName());
        if (!received.equals(local)) {
            if (LOG.isDebugEnabled()) {
                LOG.debugf("passwords did not match: {0}", password);
                LOG.debugf("received: {0}, local: {1}", received, local);
            }
            return false;
        }
        return true;
    }
}
