package org.limewire.ui.swing.player;

import org.limewire.player.api.PlayerState;

/**
 * Defines a listener to handle player mediator events.
 */
public interface PlayerMediatorListener {

    /**
     * Handles progress update to the specified value between 0.0 and 1.0.
     */
    void progressUpdated(float progress);
    
    /**
     * Handles song change to the specified song name.
     */
    void mediaChanged(String name);
    
    /**
     * Hanldes state change in the player to the specified state.
     */
    void stateChanged(PlayerState state);

}
