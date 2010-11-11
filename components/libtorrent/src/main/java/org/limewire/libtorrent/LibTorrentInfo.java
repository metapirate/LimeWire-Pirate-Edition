package org.limewire.libtorrent;

import java.util.ArrayList;
import java.util.List;

import org.limewire.bittorrent.TorrentTracker;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;

public class LibTorrentInfo extends Structure {
    public String sha1;

    public WString name;
    
    public long total_size;

    public int piece_length;

    public Pointer trackers;

    public int num_trackers;

    public Pointer seeds;

    public int num_seeds;

    public String created_by;

    public String comment;

    private List<TorrentTracker> trackers_internal = new ArrayList<TorrentTracker>();

    private List<String> seeds_internal = new ArrayList<String>();

    @Override
    public void read() {
        super.read();
        if (num_trackers > 0) {
            LibTorrentAnnounceEntry[] entries = (LibTorrentAnnounceEntry[]) new LibTorrentAnnounceEntry(
                    trackers).toArray(num_trackers);
            for (int i = 0; i < entries.length; i++) {
                LibTorrentAnnounceEntry entry = entries[i];
                entry.read();
                trackers_internal.add(entry);
            }
        }

        if (num_seeds > 0) {
            LibTorrentAnnounceEntry[] entries = (LibTorrentAnnounceEntry[]) new LibTorrentAnnounceEntry(
                    seeds).toArray(num_seeds);
            for (int i = 0; i < entries.length; i++) {
                LibTorrentAnnounceEntry entry = entries[i];
                entry.read();
                seeds_internal.add(entry.uri);
            }
        }
    }
    
    
    public List<TorrentTracker> getTrackers() {
        return trackers_internal;
    }
    
    public List<String> getSeeds() {
        return seeds_internal;
    }
}
