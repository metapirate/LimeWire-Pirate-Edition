package org.limewire.libtorrent;

import com.sun.jna.Structure;

/**
 * Basic mapping for returning pieces info from JNA.
 */
public class LibTorrentPiecesInfoContainer extends Structure {
    public int numPiecesCompleted;
    public String stateInfo;
    
    public String getStateInfo() {
        return stateInfo;
    }
    
    public int getNumPiecesCompleted() {
        return numPiecesCompleted;
    }
}
