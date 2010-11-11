package org.limewire.bittorrent;

/**
 * Enum representing various events types that a Torrent can send out to listeners.
 */
public enum TorrentEventType {
    STATUS_CHANGED, STOPPED, COMPLETED, FAST_RESUME_FILE_SAVED, STARTED, META_DATA_RECEIVED;
}
