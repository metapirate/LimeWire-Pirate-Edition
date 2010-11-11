/**
 * 
 */
package org.limewire.ui.support;

import org.limewire.core.api.support.SessionInfo;

class NoSessionInfo implements SessionInfo {

    public boolean acceptedIncomingConnection() {
        return false;
    }

    public boolean canReceiveSolicited() {
        return false;
    }

    public long getByteBufferCacheSize() {
        return 0;
    }

    public long getContentResponsesSize() {
        return 0;
    }

    public long getCreationCacheSize() {
        return 0;
    }

    public long getCurrentUptime() {
        return 0;
    }

    public long getDiskControllerByteCacheSize() {
        return 0;
    }

    public int getDiskControllerQueueSize() {
        return 0;
    }

    public long getDiskControllerVerifyingCacheSize() {
        return 0;
    }

    public int getNumIndividualDownloaders() {
        return 0;
    }

    public int getNumLeafToUltrapeerConnections() {
        return 0;
    }

    public int getNumOldConnections() {
        return 0;
    }

    public int getNumUltrapeerToLeafConnections() {
        return 0;
    }

    public int getNumUltrapeerToUltrapeerConnections() {
        return 0;
    }

    public int getNumWaitingDownloads() {
        return 0;
    }

    public int getNumberOfPendingTimeouts() {
        return 0;
    }

    public int getNumberOfWaitingSockets() {
        return 0;
    }

    public int getPort() {
        return 0;
    }

    public boolean isGUESSCapable() {
        return false;
    }

    @Override
    public boolean canDoFWT() {
        return false;
    }

    @Override
    public int getNumActiveDownloads() {
        return 0;
    }

    @Override
    public int getNumActiveUploads() {
        return 0;
    }

    @Override
    public int getNumConnectionCheckerWorkarounds() {
        return 0;
    }

    @Override
    public int getNumQueuedUploads() {
        return 0;
    }

    @Override
    public long[] getSelectStats() {
        return null;
    }

    @Override
    public int getSharedFileListSize() {
        return 0;
    }

    @Override
    public int getManagedFileListSize() {
        return 0;
    }

    @Override
    public int getAllFriendsFileListSize() {
        return 0;
    }

    @Override
    public String getUploadSlotManagerInfo() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isLifecycleLoaded() {
        return false;
    }

    @Override
    public boolean isShieldedLeaf() {
        return false;
    }

    @Override
    public boolean isSupernode() {
        return false;
    }

    @Override
    public boolean isUdpPortStable() {
        return false;
    }

    @Override
    public int lastReportedUdpPort() {
        return 0;
    }

    @Override
    public int receivedIpPong() {
        return 0;
    }

    @Override
    public long getStartTime() {
        return 0;
    }
}