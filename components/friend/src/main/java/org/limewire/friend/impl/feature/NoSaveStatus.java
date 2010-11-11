package org.limewire.friend.impl.feature;

import org.limewire.friend.api.FriendException;

/**
 * Used to access and set the nosave statuses for users
 */
public interface NoSaveStatus {


    /**
     * Get the nosave status as an enum
     *
     * @return nosave status
     */
    public NoSave getStatus();

    /**
     * Modifies the nosave status by sending a
     * packet to the server.  If the current nosave
     * status is enabled, this method attempts to disable it.
     * If disabled, this method attempts to enable it.
     *
     * This method is asynch, and simply sends the nosave.
     * Notifications come in thru the iq listener
     *
     * @throws FriendException when a problem occurs in sending
     * the nosave set packet
     */
    public void toggleStatus() throws FriendException;
}

