package org.limewire.friend.impl.feature;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

/**
 * Default implementation for {@link AuthToken}.
 */
public class AuthTokenImpl implements AuthToken {

    private final byte[] token;

    public AuthTokenImpl(byte[] token) {
        this.token = Objects.nonNull(token, "token");
    }
    
    public AuthTokenImpl(String base64Encoded) {
        this.token = Base64.decodeBase64(StringUtils.toAsciiBytes(base64Encoded));
    }
    
    @Override
    public byte[] getToken() {
        return token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthToken) {
            return Arrays.equals(token, ((AuthToken)obj).getToken());
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(token);
    }

    @Override
    public String getBase64() {
        return StringUtils.getASCIIString(Base64.encodeBase64(token));
    }
    
    @Override
    public String toString() {
        return getBase64();
    }
}
