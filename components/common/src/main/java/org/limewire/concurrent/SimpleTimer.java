package org.limewire.concurrent;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.service.ErrorService;

/**
 * An extension for {@link Timer}, allowing you to schedule a {@link Runnable}
 * task instead of scheduling a {@link TimerTask}.
 * 
 * This also exposes all the functionality of a {@link ScheduledListeningExecutorService}.
 */
public class SimpleTimer extends AbstractListeningExecutorService implements
        ScheduledListeningExecutorService {

    /** The underlying Timer of this SimpleTimer. */
    private final Timer TIMER;

    /** Whether or not we actively cancelled the timer. */
    private volatile boolean cancelled = false;

    /**
     * Creates a new active SimpleTimer.
     * 
     * @param isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(String name, boolean isDaemon) {
        TIMER = new Timer(name, isDaemon);
    }
    
    /**
     * Creates a new active SimpleTimer.
     * 
     * @param isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(boolean isDaemon) {
        TIMER = new Timer(isDaemon);
    }

    public ScheduledListeningFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ListeningScheduledTimerTask<?> future = new ListeningScheduledTimerTask<Object>(command, 0);
        scheduleInternal(future, unit.toMillis(delay), 0, false);
        return future;
    }

    public <V> ScheduledListeningFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ListeningScheduledTimerTask<V> future = new ListeningScheduledTimerTask<V>(callable, 0);
        scheduleInternal(future, unit.toMillis(delay), 0, false);
        return future;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
            TimeUnit unit) {
        ListeningScheduledTimerTask<?> future = new ListeningScheduledTimerTask<Object>(command, period);
        scheduleInternal(future, unit.toMillis(initialDelay), unit.toMillis(period), true);
        return future;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
            long delay, TimeUnit unit) {
        ListeningScheduledTimerTask<?> future = new ListeningScheduledTimerTask<Object>(command, delay);
        scheduleInternal(future, unit.toMillis(initialDelay), unit.toMillis(delay), false);
        return future;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public boolean isShutdown() {
        return cancelled;
    }

    public boolean isTerminated() {
        return cancelled;
    }

    public void shutdown() {
        cancelled = true;
        TIMER.cancel();
    }

    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    // Does not use ScheduledTimerTask so as to avoid creating the Future.
    public void execute(final Runnable command) {
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                try {
                    command.run();
                } catch (Throwable t) {
                    ErrorService.error(t);
                } 
            }
        };
        scheduleInternal(tt, 0, 0, false);
    }

    /** Schedules the task as necessary. */
    private void scheduleInternal(TimerTask task, long delay, long period, boolean fixedRate) {
        try {
            if (period == 0) {
                if (fixedRate) {
                    throw new IllegalArgumentException("cannot support 0 period w/ fixedRate");
                } else {
                    TIMER.schedule(task, delay);
                }
            } else {
                if (fixedRate) {
                    TIMER.scheduleAtFixedRate(task, delay, period);
                } else {
                    TIMER.schedule(task, delay, period);
                }
            }
        } catch (IllegalStateException ise) {
            if (cancelled)
                throw ise;
        }
    }

    /** A TimerTask that delegates to a ListeningRunnableFuture for a ListeningScheduledFuture. */
    private static class ListeningScheduledTimerTask<V> extends TimerTask implements
            ScheduledListeningFuture<V> {
        private final RunnableListeningFuture<V> task;

        public ListeningScheduledTimerTask(final Runnable r, long period) {
            task = new ListeningResetableFutureTask<V>(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } catch(RuntimeException e) {
                        ErrorService.error(e);
                        throw e;
                    } catch(Error e) {
                        ErrorService.error(e);
                        throw e;
                    } catch(Exception e) {
                        ErrorService.error(e);
                        throw new UndeclaredThrowableException(e);
                    }
                }
            }, null, period != 0);
        }

        public ListeningScheduledTimerTask(final Callable<V> c, long period) {
            task = new ListeningResetableFutureTask<V>(new Callable<V>() {
                public V call() {
                    try {
                        return c.call();
                    } catch(RuntimeException e) {
                        ErrorService.error(e);
                        throw e;
                    } catch(Error e) {
                        ErrorService.error(e);
                        throw e;
                    } catch(Exception e) {
                        ErrorService.error(e);
                        throw new UndeclaredThrowableException(e);
                    }
                }
            }, period != 0);
        }

        @Override
        public void run() {
            task.run();
        }

        public long getDelay(TimeUnit unit) {
            return -1;
        }

        public int compareTo(Delayed o) {
            return 0;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return task.cancel(mayInterruptIfRunning);
        }

        public V get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
            return task.get(timeout, unit);
        }

        public boolean isCancelled() {
            return task.isCancelled();
        }

        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public void addFutureListener(EventListener<FutureEvent<V>> listener) {
            task.addFutureListener(listener);
        }
    }

    /** A ListeningRunnableFuture that may run periodically. */
    private static class ListeningResetableFutureTask<V> extends ListeningFutureTask<V> {
        private final boolean periodic;
        
        public ListeningResetableFutureTask(Runnable runnable, V result, boolean periodic) {
            super(runnable, result);
            this.periodic = periodic;
        }

        public ListeningResetableFutureTask(Callable<V> callable, boolean periodic) {
            super(callable);
            this.periodic = periodic;
        }
        
        @Override
        public void run() {
            if(periodic) {
                super.runAndReset();
            } else {
                super.run();
            }
        }

    }

}