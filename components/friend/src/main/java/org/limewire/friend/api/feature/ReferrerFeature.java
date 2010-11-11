package org.limewire.friend.api.feature;

import java.net.URI;
import java.net.URISyntaxException;

public class ReferrerFeature extends Feature<URI> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/referrer/2010-05-07");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ReferrerFeature(URI feature) {
        super(feature, ID);
    }
}
