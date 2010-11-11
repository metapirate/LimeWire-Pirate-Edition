package org.limewire.libtorrent;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the torrent_status.state_t enum from the native libtorrent code.
 */
public enum LibTorrentState {
    /**
     * These state align with the torrent_status.state_t enum. We need to
     * maintain in the indexes in order to properly map from the int jna type to
     * the enum.
     */
    QUEUED_FOR_CHECKING(0), CHECKING_FILES(1), DOWNLOADING_METADATA(2),
    DOWNLOADING(3), FINISHED(4), SEEDING(5), ALLOCATING(6);

    private static final Map<Integer, LibTorrentState> map;

    static {
        Map<Integer, LibTorrentState> builder = new HashMap<Integer, LibTorrentState>();
        for ( LibTorrentState state : LibTorrentState.values() ) {
            builder.put(state.id, state);
        }
        map = builder;
    }

    private final int id;

    private LibTorrentState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LibTorrentState forId(int id) {
        return map.get(id);
    }

}
