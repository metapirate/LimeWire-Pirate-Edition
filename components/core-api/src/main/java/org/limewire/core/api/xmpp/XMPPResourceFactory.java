package org.limewire.core.api.xmpp;

/**
 * Defines an interface for obtaining a unique client identifier to use as
 * an XMPP resource string.
 */
public interface XMPPResourceFactory {
    /**
     * Returns a string that uniquely identifies the LimeWire instance.
     */
    public String getResource();
}
