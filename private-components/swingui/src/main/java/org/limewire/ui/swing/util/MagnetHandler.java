package org.limewire.ui.swing.util;

import org.limewire.core.api.magnet.MagnetLink;

/**
 * When given a magnet file the MagnetHandler will take the appropriate action
 * of downloading or starting a search.
 */
public interface MagnetHandler {
    /**
     * Handles the given magnet file by either starting a search or starting to
     * download the file specified in the magnet.
     */
    public void handleMagnet(final MagnetLink magnet);
}
