package org.limewire.libtorrent;


import org.limewire.bittorrent.TorrentAlert;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Structure mapping to the wrapper_alert_info structure in the
 * libtorrentwrapper library.
 */
public class LibTorrentAlert extends Structure implements TorrentAlert {

    /**
     * Category of this alert
     */
    public int category;

    /**
     * Sha1 of this alert, null or empty if not an alert on a specific torrent.
     */
    public String sha1;

    /**
     * Message associated with this alert.
     */
    public String message;
    
    /**
     * Boolean whether or not the resume data exists.
     */
    public int has_data;
    
    /**
     * Pointer to the resume data in memory.
     */
    public Pointer resume_data;
    
    @Override
    public int getCategory() {
        return category;
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return sha1 + " " + message + " [" + category + "] ";
    }
}
