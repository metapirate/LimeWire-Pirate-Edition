package com.limegroup.gnutella.downloader;

import java.util.Set;

import org.limewire.io.Address;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Small factory interface that {@link RemoteFileDescFactory} delegates to
 * to allow different {@link RemoteFileDesc} implementations to be created
 * based on the type of address.
 */
public interface RemoteFileDescCreator {
    
    /**
     * Returns true if it can create a {@link RemoteFileDesc} for <code>address</code>. 
     */
    boolean canCreateFor(Address address);
    
    RemoteFileDesc create(Address address, long index, String filename,
            long size, byte[] clientGUID, int speed, int quality, boolean browseHost, LimeXMLDocument xmlDoc,
            Set<? extends URN> urns, boolean replyToMulticast, String vendor,
            long createTime, boolean http1);
}
