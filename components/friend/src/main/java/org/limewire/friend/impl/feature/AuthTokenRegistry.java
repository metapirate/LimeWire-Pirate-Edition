package org.limewire.friend.impl.feature;

import org.limewire.friend.api.feature.AuthToken;

public interface AuthTokenRegistry {
    AuthToken getAuthToken(String presenceId);
}
