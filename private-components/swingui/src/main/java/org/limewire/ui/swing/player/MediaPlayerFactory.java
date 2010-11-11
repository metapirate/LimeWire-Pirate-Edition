package org.limewire.ui.swing.player;

import java.awt.Container;
import java.io.File;

import javax.media.IncompatibleSourceException;
import javax.media.Player;

/**
 * Creates a media player for playing audio and video.
 */
public interface MediaPlayerFactory {

    /**
     * Returns a Player for the given file. The Player is initialized and ready for starting
     * upon return. In some rare instances the Player returned can be null though this should
     * not happen in practice. If there was a problem creating the player an IncompatibleSourceException
     * should be thrown instead.
     */
    public Player createMediaPlayer(File file, final Container parentComponent) throws IncompatibleSourceException;
}
