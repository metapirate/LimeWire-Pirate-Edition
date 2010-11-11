package com.limegroup.mozilla;

import java.io.File;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

/**
 * Interface to allow access into the state of the nsIDownloadListener.
 * 
 */
public interface MozillaDownload extends BandwidthTracker {

    /**
     * Returns the download id this listener is tracking.
     */
    long getDownloadId();

    /**
     * Returns the pending amount of bytes to be downloaded.
     */
    long getAmountPending();

    /**
     * Returns the amount downloaded so far.
     */
    long getAmountDownloaded();

    /**
     * Returns the total length of the download.
     */
    long getContentLength();

    /**
     * Returns the target save file for the download.
     */
    File getIncompleteFile();

    /**
     * Indicator if the download is complete or not.
     */
    boolean isCompleted();

    /**
     * Indicator if the downloader is currently active.
     */
    boolean isInactive();

    /**
     * Indicator if the downloader is in a paused state.
     */
    boolean isPaused();

    @Override
    float getAverageBandwidth();

    @Override
    float getMeasuredBandwidth();

    @Override
    void measureBandwidth();

    /**
     * Returns the download status for this download.
     */
    DownloadState getDownloadStatus();

    /**
     * Cancels the current download.
     */
    void cancelDownload();

    /**
     * Removes the current download.
     */
    void removeDownload();

    /**
     * Pauses the current download.
     */
    void pauseDownload();

    /**
     * Resumes the current download.
     */
    void resumeDownload();

    /**
     * Adds listener for this download.
     */
    void addListener(EventListener<DownloadStateEvent> listener);

    /**
     * Removes listener from this download.
     */
    boolean removeListener(EventListener<DownloadStateEvent> listener);

    /**
     * Returns indicator that the download is queued.
     */
    boolean isQueued();

    /**
     * Returns indicator that the download is CANCELLED.
     */
    boolean isCancelled();

    void setDiskError();
}
