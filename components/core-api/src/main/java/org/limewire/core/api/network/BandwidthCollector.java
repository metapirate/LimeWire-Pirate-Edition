package org.limewire.core.api.network;

/**
 * Provides insight into current bandwidth usage per second, and the maximum
 * known bandwidth to be used for upload and downloads in the last two weeks.
 */
public interface BandwidthCollector {
    /**
     * Returns the maximum measured downstream bandwidth usage in kilobytes per
     * second.
     */
    public int getMaxMeasuredTotalDownloadBandwidth();

    /**
     * Returns the maximum measured upstream bandwidth usage in kilobytes per
     * second.
     */
    public int getMaxMeasuredTotalUploadBandwidth();

    /**
     * Returns the current downstream bandwidth usage in kilobytes per second.
     */
    public int getCurrentTotalDownloadBandwidth();

    /**
     * Returns the current upstream bandwidth usage in kilobytes per second.
     */
    public int getCurrentTotalUploadBandwidth();

    /**
     * Returns the current downstream bandwidth usage for all downloaders in
     * kilobytes per second.
     */
    public int getCurrentDownloaderBandwidth();

    /**
     * Returns the current upstream bandwidth usage for all uploaders in
     * kilobytes per second.
     */
    public int getCurrentUploaderBandwidth();

}
