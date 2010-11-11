package org.limewire.friend.api.feature;

/**
 * A per session authentication token.
 * 
 * Implementations should be expected to be immutable and hence threadsafe, and
 * follow value semantics for {@link #equals(Object)} and {@link #hashCode()}.
 */
public interface AuthToken {
    byte [] getToken();
    /**
     * @return token as a base64 encoded string
     */
    String getBase64();
}
