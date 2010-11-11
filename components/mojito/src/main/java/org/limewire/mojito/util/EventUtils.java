package org.limewire.mojito.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.ExecutorsHelper;

/**
 * A utility class to fire events.
 */
public class EventUtils {

    private static final AtomicReference<Thread> REF 
        = new AtomicReference<Thread>();
    
    private static final ThreadFactory FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "EventUtilsThread");
            thread.setDaemon(true);
            REF.set(thread);
            return thread;
        }
    };
    
    private static final ExecutorService EXECUTOR 
        = ExecutorsHelper.newSingleThreadExecutor(FACTORY);
    
    private EventUtils() {}
    
    /**
     * Returns true if the caller is the event {@link Thread}.
     */
    public static boolean isEventThread() {
        return REF.get() == Thread.currentThread();
    }
    
    /**
     * Fires the given {@link Runnable} on the event {@link Thread}.
     */
    public static void fireEvent(Runnable event) {
        EXECUTOR.execute(event);
    }
}
