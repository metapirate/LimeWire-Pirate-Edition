package com.limegroup.gnutella.downloader;

import java.util.Set;

import org.limewire.io.Address;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface RemoteFileDescDeserializer {

    void register(RemoteFileDescFactory remoteFileDescFactory);
    
    RemoteFileDesc createRemoteFileDesc(Address address, long index, String filename,
            long size, byte[] clientGUID, int speed, int quality, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, String vendor, long createTime);
        
}
