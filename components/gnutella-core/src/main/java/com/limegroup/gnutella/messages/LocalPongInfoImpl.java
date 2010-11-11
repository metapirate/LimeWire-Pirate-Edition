package com.limegroup.gnutella.messages;

import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;

@Singleton
public class LocalPongInfoImpl implements LocalPongInfo {
    
    private final Provider<ConnectionManager> connectionManager;
    private final FileView gnutellaFileView;

    @Inject
    public LocalPongInfoImpl(Provider<ConnectionManager> connectionManager,
            @GnutellaFiles FileView gnutellaFileView) {
        this.connectionManager = connectionManager;
        this.gnutellaFileView = gnutellaFileView;
    }


    /**
     * @return the number of free non-leaf slots available for limewires.
     */
    public byte getNumFreeLimeWireNonLeafSlots() {
        return (byte)connectionManager.get().getNumFreeLimeWireNonLeafSlots();
    }

    /**
     * @return the number of free leaf slots available for limewires.
     */
    public byte getNumFreeLimeWireLeafSlots() {
        return (byte)connectionManager.get().getNumFreeLimeWireLeafSlots();
    }

    public long getNumSharedFiles() {
        return gnutellaFileView.size();
    }

    public int getSharedFileSize() {
        return ByteUtils.long2int(gnutellaFileView.getNumBytes());
    }

    public boolean isSupernode() {
        return connectionManager.get().isSupernode();
    }
}
