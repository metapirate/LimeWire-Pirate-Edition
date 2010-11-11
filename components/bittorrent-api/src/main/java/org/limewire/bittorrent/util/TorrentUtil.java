package org.limewire.bittorrent.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;

public class TorrentUtil {
    
    /**
     * Returns a list of files in the given torrent.
     */
    public static List<File> buildTorrentFiles(Torrent torrent, File root) {
        List<File> files = new ArrayList<File>();
        for (TorrentFileEntry torrentFileEntry : torrent.getTorrentFileEntries()) {
            files.add(new File(root, torrentFileEntry.getPath()));
        }
        return files;
    }
    
}
