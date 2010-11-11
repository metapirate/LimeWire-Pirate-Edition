package org.limewire.mojito.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.AsyncFutureTask;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.Context;
import org.limewire.mojito.util.EventUtils;

/**
 * {@link DHTFutureTask}s have a built-in watchdog {@link Thread} that 
 * interrupts all waiting {@link Thread}s after a predefined period of
 * time.
 * 
 * @see AsyncFutureTask
 */
public class DHTFutureTask<V> extends AsyncFutureTask<V> implements DHTFuture<V> {

    private static final ScheduledExecutorService WATCHDOG 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("WatchdogThread"));
    
    private final Context context;
    
    private final DHTTask<V> task;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * Creates an {@link DHTFutureTask}
     */
    public DHTFutureTask(Context context, DHTTask<V> task) {
        
        this.context = context;
        this.task = task;
        
        this.timeout = task.getWaitOnLockTimeout();
        this.unit = TimeUnit.MILLISECONDS;
    }
    
    public Context getContext() {
        return context;
    }
    
    @Override
    protected synchronized void doRun() {
        if (!isDone()) {
            watchdog();
            start();
        }
    }
    
    /**
     * Starts the {@link DHTTask}
     */
    protected synchronized void start() {
        task.start(this);
    }
    
    /**
     * Starts the watchdog task
     */
    private synchronized boolean watchdog() {
        if (timeout == -1L || isDone()) {
            return false;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                synchronized (DHTFutureTask.this) {
                    if (!isDone()) {
                        wasTimeout = true;
                        handleTimeout(timeout, unit);
                    }
                }
            }
        };
        
        watchdog = WATCHDOG.schedule(task, timeout, unit);
        return true;
    }
    
    /**
     * Called in case of a timeout. The default implmenetation
     * calls {@link #setException(Throwable)} with a {@link TimeoutException}
     * as an argument.
     */
    protected void handleTimeout(long timeout, TimeUnit unit) {
        setException(new TimeoutException(timeout + " " + unit));
    }
    
    @Override
    public long getTimeout(TimeUnit unit) {
        return unit.convert(timeout, this.unit);
    }
    
    @Override
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }
    
    @Override
    public synchronized boolean isTimeout() {
        return wasTimeout;
    }
    
    /**
     * Overridden and declared final to do some critical bookkeeping. 
     * Please override {@link #done0()} in custom implementations.
     */
    @Override
    protected final void done() {
        synchronized (this) {
            // Cancel the watchdog
            if (watchdog != null) {
                watchdog.cancel(true);
            }
        }
        
        done0();
    }
    
    /**
     * Protected method that is invoked when this {@link DHTFutureTask}
     * completes. You may override this method.
     * 
     * @see #done()
     */
    protected void done0() {
        // Override
    }

    @Override
    protected boolean isEventThread() {
        return EventUtils.isEventThread();
    }
    
    @Override
    protected void fireOperationComplete(
            final EventListener<FutureEvent<V>>[] listeners,
            final FutureEvent<V> event) {
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DHTFutureTask.super.fireOperationComplete(listeners, event);
            }
        };
        
        EventUtils.fireEvent(task);
    }
}
