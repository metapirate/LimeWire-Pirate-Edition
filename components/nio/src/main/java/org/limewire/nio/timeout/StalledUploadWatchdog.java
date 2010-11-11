package org.limewire.nio.timeout;


import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Periodic;
import org.limewire.nio.observer.Shutdownable;


/**
 * Kills an OutputStream after a certain amount of time has passed.<p>
 * 
 * The flow has the following methodology:
 * When constructed, this class is inactive and does nothing.
 * To activate the stall-checking, you must call 'activate'.
 * When activate is called, it updates the 'check time' to be
 * DELAY_TIME plus the current time and also schedules the task
 * if necessary.
 * To de-active the stall-checking, you must call 'deactivate'.
 * Deactivate does NOT remove the task from the RouterService's schedule,
 * but it does tell the checker to not kill the output stream when
 * it is run.<p>
 *
 * Because the task can be reactivated without rescheduling, it is 
 * possible that RouterService may 'run' the task before the most
 * recent delay time has expired.  To counteract this, 'activate'
 * will store the time that it expects 'run' to be called.  If 'run' is
 * called too soon, it will reschedule the task to be run at the 
 * appropriate time.<p>
 *
 * All methods are synchronized and guaranteed to not lock the timer queue.
 */
public final class StalledUploadWatchdog extends Periodic {
    
    private static final Log LOG =
        LogFactory.getLog(StalledUploadWatchdog.class);
    
    /**
     * The amount of time to wait before we close this connection
     * if nothing has been written to the socket.
     * <p>
     * Non final for testing.
     */
    public static long DELAY_TIME = 1000 * 60 * 2; //2 minutes    
    
    private final Closer closer;
    
    private final long delayTime;
    
    
    public StalledUploadWatchdog(long delayTime, ScheduledExecutorService service) {
        super(new Closer(), service);
        this.closer = (Closer)getRunnable();
        this.delayTime = delayTime;
    }
    
    /**
     * De-activates the killing of the NormalUploadState.
     */
    public boolean deactivate() {
        if (LOG.isDebugEnabled())
            LOG.debug("Deactivated on "+closer.shutdownable);
        unschedule();
        closer.shutdownable = null;
        return closer.closed;
    }
    
    /**
     * Activates the checking.
     */
    public synchronized void activate(Shutdownable shutdownable) {
        if(LOG.isDebugEnabled())
            LOG.debug("Activated on: " + shutdownable);
        rescheduleIfLater(delayTime);
        
        closer.shutdownable = shutdownable;
    }
    
    /**
     * Kills the upload if we're active, and tells the state
     * to close the connection.
     * Reschedules if needed.
     */
    private static class Closer implements Runnable {
        private volatile Shutdownable shutdownable;
        private volatile boolean closed;
        public void run() {
            closed = true;
            // If it was null, it was already closed
            // by an outside source.
            Shutdownable toShut = shutdownable;
            if( toShut != null ) {
                if(LOG.isDebugEnabled())
                    LOG.debug("STALLED!  Killing: " + toShut);                    
                toShut.shutdown();
                shutdownable = null;
            }
        }
    }
}
