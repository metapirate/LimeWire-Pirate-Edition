package org.limewire.util;

import com.google.inject.Singleton;

/** Implementation of the system clock that delegates to java.lang.System. */
@Singleton
public class ClockImpl implements Clock {
    
    public long now() {
        return System.currentTimeMillis();
    }
    
    public long nanoTime() {
        return System.nanoTime();
    }

}
