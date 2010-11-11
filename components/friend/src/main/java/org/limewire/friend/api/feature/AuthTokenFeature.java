package org.limewire.friend.api.feature;

import java.net.URI;
import java.net.URISyntaxException;


public class AuthTokenFeature extends Feature<AuthToken> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/auth-token/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public AuthTokenFeature(AuthToken feature) {
        super(feature, ID);
    }
}
