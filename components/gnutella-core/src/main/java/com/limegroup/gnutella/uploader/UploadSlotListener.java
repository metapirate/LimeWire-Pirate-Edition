package com.limegroup.gnutella.uploader;

/**
 * Something that listens for the availability of UploadSlots.
 */
public interface UploadSlotListener extends UploadSlotUser {

    /**
     * Notification that an upload slot has become available.
     */
    public void slotAvailable();
}
