package org.limewire.util;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

public class Stopwatch {
    
    private final Log log;
    private long start = System.nanoTime();
    
    public Stopwatch(final Log log) {
        this.log = log;
    }

    /**
     * Resets and returns elapsed time in milliseconds.
     */
    public long reset() {
        if(log.isTraceEnabled()) {
            long now = System.nanoTime();
            long elapsed = now - start;
            start = now;
            return TimeUnit.NANOSECONDS.toMillis(elapsed);
        } else {
            return -1;
        }
        
    }

    /**
     * Resets and logs elapsed time in milliseconds.
     */
    public void resetAndLog(String label) {
        if(log.isTraceEnabled())
            log.trace(label + ": " + reset() + "ms");
    }
}
