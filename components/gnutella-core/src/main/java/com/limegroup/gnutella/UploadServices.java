package com.limegroup.gnutella;

public interface UploadServices {

    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    public boolean hasActiveUploads();

    /**
     * @return the bandwidth for uploads in bytes per second
     */
    public float getRequestedUploadSpeed();

    /**
     * Returns the number of uploads in progress.
     */
    public int getNumUploads();

    /**
     * Returns the number of queued uploads.
     */
    public int getNumQueuedUploads();

}