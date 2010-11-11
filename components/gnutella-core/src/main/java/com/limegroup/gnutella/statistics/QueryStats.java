package com.limegroup.gnutella.statistics;

import org.limewire.collection.NumericBuffer;

import com.google.inject.Singleton;

/**
 * Keeps track and reports some statistics about local queries.
 */
@Singleton
public class QueryStats {
    
    private NumericBuffer<Long> times = new NumericBuffer<Long>(200);
    
    public synchronized void recordQuery() {
        times.add(System.currentTimeMillis());
    }
    
    public synchronized long getLastQueryTime() {
        if (times.isEmpty())
            return 0;
        return times.first();
    }
}
