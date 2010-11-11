package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.util.Clock;

import com.google.inject.Inject;

/**
 * Maintains various session statistics, like uptime.  
 */
@EagerSingleton
public class Statistics {

    private final Clock clock;

    /** The number of seconds in a day. */
    protected static final int SECONDS_PER_DAY = 24 * 60 * 60;

    /** The time this was constructed. */
    private final long startTime;

    @Inject
    public Statistics(Clock clock) {
        this.clock = clock;
        startTime = clock.now();
    }

    /** 
     * Returns the amount of time this has been running.
     * @return the session uptime in milliseconds
     */        
    public long getUptime() {
        // Return 0 if the clock has gone backwards
        return Math.max(0, clock.now() - startTime);
    }
    
    /**
     * @return the clock time when uptime began recording from.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Calculates the average number of seconds this host runs per day.
     * @return uptime in seconds/day.
     * @see calculateFractionalUptime
     */
    public int calculateDailyUptime() {
        return (int)(calculateFractionalUptime() * SECONDS_PER_DAY);
    }

    /** 
     * Calculates the fraction of time this host runs, a unitless quantity
     * between zero and 1.
     * @see calculateDailyUptime  
     */
    public float calculateFractionalUptime() {
        String[] uptimes = ApplicationSettings.UPTIME_HISTORY.get();
        String[] downtimes = ApplicationSettings.DOWNTIME_HISTORY.get();
        float up = 0, down = 0;
        for(int i = 0; i < uptimes.length; i++) {
            up += Long.valueOf(uptimes[i]);
        }
        for(int i = 0; i < downtimes.length; i++) {
            down += Long.valueOf(downtimes[i]);
        }
        if(up + down == 0f)
            return 0f;
        else
            return up / (up + down); 
    }
}
