package org.limewire.bittorrent;

public interface TorrentAlert {

	// TODO: These names are copied directly from the c code,
	//         might want to use Java conventions
    public static final int error_notification = 0x1;
    public static final int peer_notification = 0x2;
    public static final int port_mapping_notification = 0x4;
    /** Alerts related to the fast resume file **/
    public static final int storage_notification = 0x8;
    public static final int tracker_notification = 0x10;
    public static final int debug_notification = 0x20;
    public static final int status_notification = 0x40;
    public static final int progress_notification = 0x80;
    public static final int ip_block_notification = 0x100;
    public static final int performance_warning = 0x200;
    public static final int all_categories = 0xffffffff;

    /**
     * Returns the category for this alert.
     */
    public int getCategory();

    /**
     * Returns the sha1 associated with this alert. will return null or empty
     * string if the alert is not associated with a torrent. but is a more
     * general alert.
     */
    public String getSha1();

    /**
     * Returns a message describing what this alert relates to.
     */
    public String getMessage();

}