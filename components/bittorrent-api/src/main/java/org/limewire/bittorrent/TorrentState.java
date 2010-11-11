package org.limewire.bittorrent;

/**
 * Represents the Torrents State
 */
public enum TorrentState {
    QUEUED_FOR_CHECKING, CHECKING_FILES, DOWNLOADING_METADATA, DOWNLOADING, FINISHED, SEEDING, ALLOCATING;
}
