package org.limewire.friend.api.feature;

import java.net.URI;
import java.net.URISyntaxException;


public class LimewireFeature extends Feature<String> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public LimewireFeature() {
        super(ID.toASCIIString(), ID);
    }
}
