package org.limewire.friend.api.feature;

import java.net.URI;
import java.net.URISyntaxException;

public class URIFeature extends Feature<URI> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/uri/2010-05-07");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URIFeature(URI feature) {
        super(feature, ID);
    }
}
