package org.limewire.libtorrent.callback;

import org.limewire.libtorrent.LibTorrentAlert;

import com.sun.jna.Callback;

/**
 * AlertCallback interface sent to the libtorrentwrapper native code to handle
 * processing various kinds of alerts.
 */
public interface AlertCallback extends Callback {
    public void callback(LibTorrentAlert alert);
}
