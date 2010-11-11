package org.limewire.core.api.transfer;

/**
 * Stores details for an active peer.
 */
public interface SourceInfo {
    
    /**
     * Returns this peers ip address.
     */
    String getIPAddress();
    
    /**
     * Whether the peer connection is encypted or not.
     */
    boolean isEncyrpted();
    
    /**
     * The name of the peer's client.
     */
    String getClientName();
    
    /**
     * Returns the current total upload speed to this source in bytes/sec.
     */
    float getUploadSpeed();
    
    /**
     * Returns the current total download speed from this source in bytes/sec.
     */
    float getDownloadSpeed();
}
