package com.limegroup.gnutella;

// TODO move to net
/**
 * Configuration for UPnPManager
 */
public interface UPnPManagerConfiguration {
    boolean isEnabled();
    void setEnabled(boolean enabled);
    String getClientID();
}
