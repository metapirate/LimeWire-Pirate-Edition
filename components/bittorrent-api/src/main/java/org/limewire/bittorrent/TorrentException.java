package org.limewire.bittorrent;

/**
 * TorrentException is used to wrap a caught exception from the native code.
 */
public class TorrentException extends RuntimeException {
    private final int type;

    public static final int LOAD_EXCEPTION = -100000;

    public static final int DISABLED_EXCEPTION = -100001;

    public static final int INITIALIZATION_EXCEPTION = -100002;

    public TorrentException(String message, int type) {
        super(message);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
