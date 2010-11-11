package org.limewire.security.id;

import org.limewire.io.GUID;

/**
 * This is the storage of a SecureIdManager. It stores both the local identity
 * and the remoteIdKeys the local node shares with other nodes in the network.
 */
public interface SecureIdStore {
    /**
     * store identity information of the local node  
     * @param value is the identity information of the local node
     */
    void setLocalData(byte[] value);
    
    /** 
     * @return identity information of the local node 
     */
    byte[] getLocalData();
    
    /**
     * store shared keys etc of a remote node
     * @param key is the remote node's GUID
     * @param shared keys etc of the remote node
     */
    public void put(GUID key, byte[] value);
    
    /**
     * get shared keys etc of a remote node
     * @param key is the remote node's GUID
     * @return shared keys etc of the remote node, null is returned if the GUID key is unknown.
     */
    byte[] get(GUID key);
}
