package com.limegroup.gnutella.statistics;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.Clock;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.Statistics;

/**
 * Periodically updates the uptime statistics.
 */
@EagerSingleton
final class UptimeStatTimer implements Service {

    /** Current uptime in seconds. */
    private volatile long currentUptime = 0;

    /** How often to update the uptime settings, in seconds. */
    private static final int UPDATE_INTERVAL = 10;

    /** How many uptimes and downtimes to record. */
    protected static final int HISTORY_LENGTH = 20;

    /** Downtime to use if this is the first session, in seconds. */
    protected static final int DEFAULT_DOWNTIME = 24 * 60 * 60; // 24 hours

    private long lastUpdateTime = 0;
    private volatile ScheduledFuture<?> future = null;
    private final AtomicBoolean firstUptimeUpdate = new AtomicBoolean(true);    
    private final ScheduledExecutorService backgroundExecutor;
    private final Statistics stats;
    private final Clock clock;

    @Inject
    UptimeStatTimer(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Statistics stats, Clock clock) {
        this.backgroundExecutor = backgroundExecutor;
        this.stats = stats;
        this.clock = clock;
    }

    @Inject void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getServiceName() {
        return I18nMarker.marktr("Uptime Statistics");
    }

    @Override
    public void initialize() {
        // Increment the session counter
        int sessions = ApplicationSettings.SESSIONS.getValue();
        ApplicationSettings.SESSIONS.setValue(sessions + 1);
        // Record the time between sessions
        long lastShutdown = ApplicationSettings.LAST_SHUTDOWN_TIME.getValue();
        long downtime;
        if(lastShutdown == 0)
            downtime = DEFAULT_DOWNTIME;
        else
            downtime = Math.max(0, (clock.now() - lastShutdown) / 1000);
        // If the number of downtimes is greater that the number of uptimes,
        // the last session must have ended without recording the uptime or
        // shutdown time. To avoid double-counting the downtime we should
        // overwrite the last downtime instead of appending.
        String[] downtimes = ApplicationSettings.DOWNTIME_HISTORY.get();
        String[] uptimes = ApplicationSettings.UPTIME_HISTORY.get();
        if(downtimes.length > uptimes.length)
            downtimes = updateHistory(downtimes, Long.toString(downtime));
        else
            downtimes = appendToHistory(downtimes, Long.toString(downtime));
        ApplicationSettings.DOWNTIME_HISTORY.set(downtimes);
        // Measure the time between refreshes
        lastUpdateTime = clock.now();
    }

    @Override
    public void start() {
        // Start the periodic update timer
        future = backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                refreshStats();
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        // Update the stats for the last time before saving
        refreshStats();
        Future future = this.future;
        if(future != null) {
            future.cancel(false);
            this.future = null;
        }
    }

    /**
     * Refreshes the uptime statistics. Package access for testing.
     */
    void refreshStats() {
        long now = clock.now();
        long elapsed = (now - lastUpdateTime) / 1000;
        if(elapsed > 0) {
            currentUptime += elapsed;
            updateUptimeHistory(currentUptime);
            long totalUptime = ApplicationSettings.TOTAL_UPTIME.getValue() + elapsed;
            ApplicationSettings.TOTAL_UPTIME.setValue(totalUptime);
            int sessions = ApplicationSettings.SESSIONS.getValue();
            if(sessions > 0) {
                ApplicationSettings.AVERAGE_UPTIME.setValue(totalUptime / sessions);
            }
            ApplicationSettings.FRACTIONAL_UPTIME.setValue(stats.calculateFractionalUptime());
            ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(now); // Pessimistic
        }        
        lastUpdateTime = now;
    }

    /**
     * Updates the uptime history with the current uptime.
     * Package access for testing.
     * 
     * @param currentUptime the current uptime in seconds
     */
    void updateUptimeHistory(long currentUptime) {
        String[] uptimes = ApplicationSettings.UPTIME_HISTORY.get();
        // The first update in each session should append to the array
        if(firstUptimeUpdate.getAndSet(false))
            uptimes = appendToHistory(uptimes, Long.toString(currentUptime));
        else
            uptimes = updateHistory(uptimes, Long.toString(currentUptime));
        ApplicationSettings.UPTIME_HISTORY.set(uptimes);
    }

    private String[] appendToHistory(String[] original, String newItem) {
        String[] copy;
        if(original.length < HISTORY_LENGTH) {
            copy = new String[original.length + 1];
            System.arraycopy(original, 0, copy, 0, original.length);
        } else {
            copy = new String[HISTORY_LENGTH];
            System.arraycopy(original, 1, copy, 0, copy.length - 1);
        }
        copy[copy.length - 1] = newItem;
        return copy;
    }

    private String[] updateHistory(String[] history, String newItem) {
        if(history.length == 0)
            history = new String[1];
        history[history.length - 1] = newItem;
        return history;
    }
}
