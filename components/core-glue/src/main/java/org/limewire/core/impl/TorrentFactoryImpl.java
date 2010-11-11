package org.limewire.core.impl;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

class TorrentFactoryImpl implements TorrentFactory {
    
    private static final Log LOG = LogFactory.getLog(TorrentFactoryImpl.class);
    private final TorrentManager torrentManager;
    
    @Inject
    public TorrentFactoryImpl(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    @Override
    public Torrent createTorrentFromXML(LimeXMLDocument xmlDocument) {
        // if this isn't a torrent xml file then return null
        if(xmlDocument == null || !xmlDocument.getSchemaURI().equals(LimeXMLNames.TORRENT_SCHEMA)) {
            return null;
        }

        try {
            XMLTorrent xmlTorrent = new XMLTorrent(xmlDocument);
            Torrent torrent = torrentManager.getTorrent(xmlTorrent.getSha1());
            return torrent != null ? torrent : xmlTorrent;
        } catch (InvalidDataException ive) {
            LOG.infof(ive, "error parsing torrent xml: {0}", xmlDocument);
            return null;
        }
    }
}
