package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentFileEntry;

import com.sun.jna.Structure;
import com.sun.jna.WString;

/**
 * Represents a file in the torrent.
 */
public class LibTorrentFileEntry extends Structure implements Structure.ByReference,
        TorrentFileEntry {

    /**
     * Index of file within the torrent.
     */
    public int index;

    /**
     * Relative path of file within the torrent.
     */
    public WString path;

    /**
     * Total size of the file.
     */
    public long size;

    /**
     * The total amount of the file downloaded so far.
     */
    public long total_done;

    /**
     * The priority for downloading this file in the torrent.
     */
    public int priority;

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getTotalDone() {
        return total_done;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public float getProgress() {
        if (getSize() == 0) {
            return 1;
        }

        return getTotalDone() / (float) getSize();
    }

    @Override
    public String toString() {
        return getPath();
    }
}
