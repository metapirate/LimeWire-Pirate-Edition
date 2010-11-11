package org.limewire.friend.api.feature;

import java.net.URI;

import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

/**
 * Represents a custom capability that a FriendPresence supports.  A Feature is
 * used to layer custom communication on top of existing social networks, (i.e., jabber)
 * They may represent custom data that enables two friends to make a p2p connection,
 * or an action, such as recommending a file to them. 
 */
public class Feature<T> {
    private final T feature;
    private final URI id;

    public Feature(URI id) {
        this.feature = null;
        this.id = id;
    }

    public Feature(T feature, URI id) {
        this.feature = Objects.nonNull(feature, "feature");
        this.id = id;
    }

    public T getFeature() {
        return feature;
    }

    public URI getID() {
        return id;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this, id, feature);
    }
}
