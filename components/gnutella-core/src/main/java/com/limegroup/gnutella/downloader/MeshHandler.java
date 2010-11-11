package com.limegroup.gnutella.downloader;

import java.util.Collection;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * Defines an interface to inform the mesh network about 
 * the quality and location of <code>RemoteFileDesc</code>s. 
 */

public interface MeshHandler {
    void informMesh(RemoteFileDesc rfd, boolean good);
    void addPossibleSources(Collection<? extends RemoteFileDesc> hosts);
}