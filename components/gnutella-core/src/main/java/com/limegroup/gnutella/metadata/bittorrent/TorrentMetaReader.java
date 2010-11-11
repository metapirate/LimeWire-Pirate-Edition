package com.limegroup.gnutella.metadata.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.BTDataValueException;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaReader;

/**
 * Converts a torrent file into a MetaData object. 
 */
public class TorrentMetaReader implements MetaReader {

    @Override
    public MetaData parse(File torrentFile) throws IOException {
        FileInputStream torrentInputStream = null;
        try {
            torrentInputStream = new FileInputStream(torrentFile);
            Object obj = Token.parse(torrentInputStream.getChannel());
            if (!(obj instanceof Map)) {
                throw new BTDataValueException("expected map");
            }
            
            BTData btData = new BTDataImpl((Map)obj);
            btData.clearPieces(); // save memory
            return new TorrentMetaData(btData);
        } finally {
            IOUtils.close(torrentInputStream);
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "torrent" };
    }
}
