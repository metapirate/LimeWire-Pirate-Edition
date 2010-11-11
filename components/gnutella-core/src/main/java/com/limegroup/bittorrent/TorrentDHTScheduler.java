package com.limegroup.bittorrent;

import java.io.File;
import java.util.Date;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;

/**
 * The dht scheduler will check the torrents every minute to see if they may
 * need the dht to continue downloading.
 * 
 * If a torrent does need the dht and the dht is not started, it will be
 * started. Every five minutes there after the torrent will be checked to see if
 * it still needs the dht. If it does not, then that torrent will be marked as
 * not needing the dht.
 * 
 * If there are no torrents needing the dht then it will be shutdown.
 */
public class TorrentDHTScheduler implements Runnable {
    private static final Log LOG = LogFactory.getLog(TorrentDHTScheduler.class);

    private static final String LAST_DHT_START_TIME = "lastDHTStartTime";
    private static final String DHT_ENABLED = "dhtEnabled";

    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = ONE_MINUTE * 60;

    private final TorrentManager torrentManager;

    @Inject
    public TorrentDHTScheduler(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
    }

    @Override
    public void run() {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Running DHT Checks: {0}", new Date());
        }

        torrentManager.getLock().lock();
        try {
            boolean dhtStarted = torrentManager.isDHTStarted();
            boolean dhtEnabled = false;
            for (Torrent torrent : torrentManager.getTorrents()) {
                if (dhtEnabled(torrent) && shouldDisableDHT(torrent)) {
                    torrent.setProperty(DHT_ENABLED, Boolean.FALSE);
                    dhtEnabled |= false;
                    LOG.debugf("Disabling DHT for torrent: {0}", torrent.getName());
                } else if (needsDHTConnections(torrent) && shouldTryDHT(torrent)) {
                    torrent.setProperty(LAST_DHT_START_TIME, new Long(System.currentTimeMillis()));
                    torrent.setProperty(DHT_ENABLED, Boolean.TRUE);
                    dhtEnabled |= true;
                    LOG.debugf("Enabling DHT for torrent: {0}", torrent.getName());
                } else if (dhtEnabled(torrent)) {
                    dhtEnabled |= true;
                    LOG.debugf("Keeping DHT Enabled for torrent: {0}", torrent.getName());
                } else {
                    LOG.debugf("Keeping DHT Disabled for torrent: {0}", torrent.getName());
                }
            }

            File dhtStateFile = torrentManager.getTorrentManagerSettings().getDHTStateFile();
            if (!dhtStarted && dhtEnabled) {
                torrentManager.startDHT(dhtStateFile);
                LOG.debugf("DHT Started");
                // TODO this will currently not work, starting a dht after a
                // torrent has already been added
                // does not currently attach the dht to the torrent, in
                // 0.14.7
                // this behavior will be updated
                // to attach the dht to already running torrents. we can remove
                // this comment then.
                // or we can add the fix into our own build once it becomes
                // available.
            } else if (dhtStarted && !dhtEnabled) {
                torrentManager.saveDHTState(dhtStateFile);
                torrentManager.stopDHT();
                LOG.debugf("DHT Stopped");
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("DHT Kept: {0}", (dhtStarted ? "on" : "off"));
                }
            }
        } finally {
            torrentManager.getLock().unlock();
        }
    }

    private boolean dhtEnabled(Torrent torrent) {
        Boolean dhtEnabled = torrent.getProperty(DHT_ENABLED, Boolean.FALSE);
        return dhtEnabled.booleanValue();
    }

    private boolean shouldDisableDHT(Torrent torrent) {
        long runningTimeOfTorrent = getTorrentRunningTime(torrent);

        if (!torrent.isStarted() || torrent.isFinished() || torrent.isPrivate()
                || runningTimeOfTorrent < ONE_MINUTE) {
            return true;
        }

        Long lastDHTStartTime = torrent.getProperty(LAST_DHT_START_TIME, null);
        if (dhtEnabled(torrent) && lastDHTStartTime != null) {
            long runningTimeMillis = getTimeSinceLastDHTStartForTorrent(lastDHTStartTime);
            long runningTimeMinutes = runningTimeMillis / (60 * 1000);

            if (LOG.isDebugEnabled()) {
                LOG.debugf("DHT Running for torrent: {0} for {1} minutes.", torrent.getName(),
                        runningTimeMinutes);
            }

            if (runningTimeMinutes > 30) {
                // only try the dht for 30 minutes at a time
                return true;
            }
            
            // check the number of peers every five minutes max.
            if (runningTimeMinutes % 5 == 0) {
                int numDHTNonTrackerPeers = 0;
                int numTotalPeers = 0;
                int numNonDHTPeers = 0;

                for (TorrentPeer peer : torrent.getTorrentPeers()) {
                    numTotalPeers++;
                    if (peer.isFromDHT() && !peer.isFromTracker()) {
                        numDHTNonTrackerPeers++;
                    } else {
                        numNonDHTPeers++;
                    }
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Torrent: {0} has {1} total peers.", torrent.getName(),
                            numTotalPeers);
                    LOG.debugf("Torrent: {0} has {1} DHT non tracker peers.", torrent.getName(),
                            numDHTNonTrackerPeers);
                    LOG.debugf("Torrent: {0} has {1} non DHT peers.", torrent.getName(),
                            numNonDHTPeers);
                }
                return numTotalPeers > 0 && (numNonDHTPeers / (float) numTotalPeers) > .50;
            }
        }

        return false;
    }

    private boolean shouldTryDHT(Torrent torrent) {
        Boolean dhtEnabled = torrent.getProperty(DHT_ENABLED, null);
        Long lastDHTStartTime = torrent.getProperty(LAST_DHT_START_TIME, null);

        if (dhtEnabled == null || lastDHTStartTime == null) {
            return true;
        }

        long timeSinceLastDHTStart = getTimeSinceLastDHTStartForTorrent(lastDHTStartTime);
        return timeSinceLastDHTStart > ONE_HOUR;
    }

    private boolean needsDHTConnections(Torrent torrent) {
        if (!torrent.isPaused()) {
            return torrent.getNumConnections() < 5;
        }
        return false;
    }

    /**
     * Returns the time in milliseconds that the dht was last enabled for this
     * torrent.
     */
    private long getTimeSinceLastDHTStartForTorrent(Long lastDHTStartTime) {
        long timeSinceLastDHTStart = System.currentTimeMillis() - lastDHTStartTime.longValue();
        return timeSinceLastDHTStart;
    }

    /**
     * Returns the torrents running time in milliseconds.
     */
    private long getTorrentRunningTime(Torrent torrent) {
        long runningTimeOfTorrent = System.currentTimeMillis() - torrent.getStartTime();
        return runningTimeOfTorrent;
    }
}
