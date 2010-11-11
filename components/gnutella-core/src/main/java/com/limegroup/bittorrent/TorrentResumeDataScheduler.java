package com.limegroup.bittorrent;

import java.util.Iterator;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;

/**
 * Iterates through the torrents periodically saving a fastresume file for each
 * file.
 */
public class TorrentResumeDataScheduler implements Runnable {

    private final TorrentManager torrentManager;
    private Iterator<Torrent> torrentIterator;

    public TorrentResumeDataScheduler(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        torrentIterator = torrentManager.getTorrents().iterator();
    }

    @Override
    public void run() {
        torrentManager.getLock().lock();
        try {
            if (!torrentIterator.hasNext()) {
                torrentIterator = torrentManager.getTorrents().iterator();
                if (!torrentIterator.hasNext()) {
                    return;
                }
            }

            Torrent torrent = torrentIterator.next();

            if (torrent.isValid() && torrent.hasMetaData()) {
                torrent.saveFastResumeData();
            }
        } finally {
            torrentManager.getLock().unlock();
        }
    }
}
