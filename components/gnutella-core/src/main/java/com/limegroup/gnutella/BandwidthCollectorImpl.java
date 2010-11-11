package com.limegroup.gnutella;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.statistic.BasicKilobytesStatistic;
import org.limewire.statistic.Statistic;
import org.limewire.statistic.StatisticAccumulator;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

@EagerSingleton
public class BandwidthCollectorImpl implements BandwidthCollectorDriver {

    private final Provider<ConnectionManager> connectionManager;
    private final Provider<BandwidthTracker> uploadTracker;
    private final Provider<BandwidthTracker> downloadTracker;
    private final Provider<TorrentManager> torrentManager;

    // these inspections include:
    // gnutella downloads and uploads, torrents,
    // gnutella messaging and mozilla downloads.
    // missing gnutella dht, missing torrent dht, missing other connection types

    /**
     * 200 measurements are saved, 1 per second.
     */
    private final Statistic uploadStat; //

    /**
     * 200 measurements are saved, 1 per second.
     */
    private final Statistic downloadStat;

    private final AtomicInteger currentTotalUploadBandwidthKiloBytes = new AtomicInteger(0);
    private final AtomicInteger currentTotalDownloadBandwidthKiloBytes = new AtomicInteger(0);
    private final AtomicInteger currentUploaderPayloadBandwidthKiloBytes = new AtomicInteger(0);
    private final AtomicInteger currentDownloaderPayloadBandwidthKiloBytes = new AtomicInteger(0);

    @Inject
    public BandwidthCollectorImpl(@Named("uploadTracker") Provider<BandwidthTracker> uploadTracker,
            @Named("downloadTracker") Provider<BandwidthTracker> downloadTracker,
            Provider<ConnectionManager> connectionManager,
            StatisticAccumulator statisticAccumulator, Provider<TorrentManager> torrentManager) {
        this.uploadTracker = uploadTracker;
        this.downloadTracker = downloadTracker;
        this.connectionManager = connectionManager;
        this.torrentManager = torrentManager;
        this.uploadStat = new BandwidthStat(statisticAccumulator);
        this.downloadStat = new BandwidthStat(statisticAccumulator);
    }

    @Override
    public int getMaxMeasuredTotalDownloadBandwidth() {
        return DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.getValue();
    }

    @Override
    public int getMaxMeasuredTotalUploadBandwidth() {
        return UploadSettings.MAX_MEASURED_UPLOAD_KBPS.getValue();
    }

    @Override
    public int getCurrentTotalDownloadBandwidth() {
        return currentTotalDownloadBandwidthKiloBytes.get();
    }

    @Override
    public int getCurrentTotalUploadBandwidth() {
        return currentTotalUploadBandwidthKiloBytes.get();
    }

    @Override
    public int getCurrentDownloaderBandwidth() {
        return currentDownloaderPayloadBandwidthKiloBytes.get();
    }

    @Override
    public int getCurrentUploaderBandwidth() {
        return currentUploaderPayloadBandwidthKiloBytes.get();
    }

    /**
     * Collects data on the bandwidth that has been used for file uploads and
     * downloads.
     */
    @Override
    public void collectBandwidthData() {
        uploadTracker.get().measureBandwidth();
        downloadTracker.get().measureBandwidth();
        connectionManager.get().measureBandwidth();

        float uploadTrackerBandwidth = getUploadTrackerBandwidth();
        float downloadTrackerBandwidth = getDownloadTrackerBandwidth();
        float connectionManagerUploadBandwidth = connectionManager.get()
                .getMeasuredUpstreamBandwidth();
        float connectionManagerDownloadBandwidth = connectionManager.get()
                .getMeasuredDownstreamBandwidth();

        List<Torrent> torrents = torrentManager.get().getTorrents();
        float torrentUploadBandwidth = calculateTorrentUpstreamBandwidth(torrents);
        float torrentUploadPayloadBandwidth = calculateTorrentUpstreamPayloadBandwidth(torrents);

        int newUpstreamKiloBytesPerSec = (int) addPositive(addPositive(uploadTrackerBandwidth, connectionManagerUploadBandwidth), torrentUploadBandwidth);
        int newUploaderKiloBytesPerSec = (int) addPositive(uploadTrackerBandwidth, torrentUploadPayloadBandwidth);

        uploadStat.addData(newUpstreamKiloBytesPerSec);

        // TODO downstream kilobytes per sec is missing non payload torrent
        // bandwidth.
        int newDownstreamKiloBytesPerSec = (int) addPositive(downloadTrackerBandwidth, connectionManagerDownloadBandwidth);
        int newDownloaderKiloBytesPerSec = (int) addPositive(0, downloadTrackerBandwidth);

        downloadStat.addData(newDownstreamKiloBytesPerSec);

        int maxUpstreamKiloBytesPerSec = getMaxMeasuredTotalUploadBandwidth();
        if (newUpstreamKiloBytesPerSec > maxUpstreamKiloBytesPerSec) {
            maxUpstreamKiloBytesPerSec = newUpstreamKiloBytesPerSec;
            UploadSettings.MAX_MEASURED_UPLOAD_KBPS.setValue(maxUpstreamKiloBytesPerSec);
        }

        int maxDownstreamKiloBytesPerSec = getMaxMeasuredTotalDownloadBandwidth();
        if (newDownstreamKiloBytesPerSec > maxDownstreamKiloBytesPerSec) {
            maxDownstreamKiloBytesPerSec = newDownstreamKiloBytesPerSec;
            DownloadSettings.MAX_MEASURED_DOWNLOAD_KBPS.setValue(maxDownstreamKiloBytesPerSec);
        }

        currentDownloaderPayloadBandwidthKiloBytes.set(newDownloaderKiloBytesPerSec);
        currentUploaderPayloadBandwidthKiloBytes.set(newUploaderKiloBytesPerSec);
        currentTotalDownloadBandwidthKiloBytes.set(newDownstreamKiloBytesPerSec);
        currentTotalUploadBandwidthKiloBytes.set(newUpstreamKiloBytesPerSec);
    }

    private float getDownloadTrackerBandwidth() {
        float bandwidth;
        try {
            // this includes torrents
            bandwidth = downloadTracker.get().getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
        return bandwidth;
    }

    private float getUploadTrackerBandwidth() {
        float bandwidth;
        try {
            // this does not include torrents
            bandwidth = uploadTracker.get().getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            bandwidth = 0;
        }
        return bandwidth;
    }

    /**
     * Returns the torrent upstream bandwidth in kilobytes per second.
     */
    private float calculateTorrentUpstreamBandwidth(List<Torrent> torrents) {

        float rate = 0;
        for (Torrent torrent : torrentManager.get().getTorrents()) {
            TorrentStatus torrentStatus = torrent.getStatus();
            if (torrentStatus != null) {
                rate = addPositive(rate, torrentStatus.getUploadRate());
            }
        }
        return rate / 1024;
    }

    /**
     * Returns the torrent upstream payload bandwidth in kilobytes per second.
     */
    private float calculateTorrentUpstreamPayloadBandwidth(List<Torrent> torrents) {

        float rate = 0;
        for (Torrent torrent : torrentManager.get().getTorrents()) {
            TorrentStatus torrentStatus = torrent.getStatus();
            if (torrentStatus != null) {
                // ignoring paused torrents because the rate takes a while to
                // cycle down event though the number should be zero.
                if (!torrentStatus.isPaused()) {
                    rate = addPositive(rate, torrentStatus.getUploadPayloadRate());
                }
            }
        }
        return rate / 1024;
    }

    private class BandwidthStat extends BasicKilobytesStatistic {
        public BandwidthStat(StatisticAccumulator statisticAccumulator) {
            super(statisticAccumulator);
        }
    }

    /**
     * Adds positive numbers in the list together returning the sum. 
     */
    private float addPositive(float one, float two) {
        float sum = 0;
        if(one > 0) {
            sum += one;
        }
        if(two > 0) {
            sum += two;
        }
        return sum;
    }
}
