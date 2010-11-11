package org.limewire.core.impl.search.torrentweb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;

public class TorrentWebSearchUtils {

    /**
     * @return null if there was an error parsing
     */
    public static BTData parseTorrentFile(File torrentFile) {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Object obj = Token.parse(fileChannel);
            if (obj instanceof Map) {
                BTDataImpl torrentData = new BTDataImpl((Map)obj);
                torrentData.clearPieces();
                return torrentData;
            }
        } catch (IOException ie) {
            // TODO log
        } finally {
            IOUtils.close(fis);
            IOUtils.close(fileChannel);
        }
        return null;
    }
    
}
