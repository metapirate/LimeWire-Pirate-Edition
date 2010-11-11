package org.limewire.friend.impl.feature;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.friend.api.feature.Feature;

/**
 * The nosave feature is associated with an XMPP extension
 * used by Google to enable/disable server side logging of a chat
 * with a specific contact.
 *
 */
public class NoSaveFeature extends Feature<NoSaveStatus> {

    public static final URI ID;

    static {
        try {
            ID = new URI("google:nosave");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public NoSaveFeature(NoSaveStatus feature) {
        super(feature, ID);
    }
}
