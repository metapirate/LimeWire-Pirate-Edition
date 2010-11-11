package org.limewire.core.impl;

import org.limewire.bittorrent.Torrent;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public interface TorrentFactory {

    /**
     * Returns a Torrent file if one exists or null if one does not.
     * Attempts to return the valid Torrent file if it is currently
     * uploading or downloading first, then attempts to reconstruct
     * one through XML data if one does not exist.
     */
    public Torrent createTorrentFromXML(LimeXMLDocument document);
}
