package org.limewire.net;

public interface TLSManager {
    boolean isTLSSupported();

    boolean isIncomingTLSEnabled();

    void setIncomingTLSEnabled(boolean enabled);

    boolean isOutgoingTLSEnabled();

    void setOutgoingTLSEnabled(boolean enabled);
}
