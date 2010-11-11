package com.limegroup.gnutella.downloader;

/** Allows a writer to delay its writing. */
interface DelayedWrite {
    
    /** Returns true if the write was performed, false otherwise. */
    boolean write();

}