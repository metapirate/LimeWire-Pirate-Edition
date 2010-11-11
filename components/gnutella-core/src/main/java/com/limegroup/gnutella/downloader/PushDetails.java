package com.limegroup.gnutella.downloader;

import java.util.Arrays;

import org.limewire.io.GUID;


class PushDetails {
    
    private final byte[] clientGUID;
    private final String address;
    private final GUID uniqueID;

    PushDetails(byte[] clientGUID, String address) {
        this.clientGUID = clientGUID;
        this.address = address;
        this.uniqueID = new GUID();
    }

    String getAddress() {
        return address;
    }

    byte[] getClientGUID() {
        return clientGUID;
    }

    GUID getUniqueID() {
        return uniqueID;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof PushDetails) {
            PushDetails other = (PushDetails)o;
            return uniqueID.equals(other.uniqueID)
                && Arrays.equals(clientGUID, other.clientGUID)
                && address.equals(other.address);
        } else {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return "clientGUID: " + new GUID(clientGUID) + ", address: " + address + ", uniqueID: " + uniqueID;
    }

}
