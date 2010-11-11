package org.limewire.collection;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.util.Clock;

/**
 * A utility to schedule, reschedule and cancel the execution of a task.
 * 
 * <pre>
 * Calendar cal = new GregorianCalendar();
 *     System.out.println(&quot;1) &quot; + cal.get(Calendar.SECOND));
 *     
 *     Periodic p = new Periodic(new Runnable() {
 *         public void run() {
 *             Calendar cal = new GregorianCalendar();
 *             System.out.println(&quot;3) &quot; + cal.get(Calendar.SECOND));
 *             }}, new SimpleTimer(false));
 * 
 *     p.rescheduleIfLater(5000);
 *     System.out.println(&quot;2) &quot; + cal.get(Calendar.SECOND));
 *     
 *     Time run-dependant Output:
 *         1) 23
 *         2) 23
 *         3) 28
 * </pre>
 */
public class Periodic {

    private static final long NANO_ORIGIN = System.nanoTime();

    private final ScheduledExecutorService scheduler;

    private final Delegate d;

    private long nextExecuteTime;

    private Future future;
    
    private final Clock clock;

    private long now() {
        if(clock == null) {
            return System.nanoTime() - NANO_ORIGIN;
        } else {
            return clock.nanoTime() - NANO_ORIGIN;
        }
    }

    /**
     * Creates a periodic task.
     * 
     * @param r the <tt>Runnable</tt> to execute
     * @param scheduler the <tt>SchedulingThreadPool</tt> to schedule execution
     *        on.
     */
    public Periodic(Runnable r, ScheduledExecutorService scheduler) {
        this(r, scheduler, null);
    }
    
    /**
     * Creates a periodic task, with the given clock.
     * 
     * @param r the <tt>Runnable</tt> to execute
     * @param scheduler the <tt>SchedulingThreadPool</tt> to schedule execution
     *        on.
     */
    public Periodic(Runnable r, ScheduledExecutorService scheduler, Clock clock) {
        this.d = new Delegate(r);
        this.scheduler = scheduler;
        this.clock = clock;
    }    

    /**
     * Changes the execution time of this Periodic task if it is later than the
     * current execution time.
     * <p>
     * Note: some implementations of <tt>ScheduledExecutorService</tt> use
     * nanoseconds as their time unit, so do not schedule anything for more than
     * 292 years in the future. More practically, this means you should not use
     * Long.MAX_VALUE as parameter.
     * <p>
     * 
     * @param newDelay the new delay from now when this should be executed
     * @return true if the execution time changed
     */
    public synchronized boolean rescheduleIfLater(long newDelay) {
        newDelay = TimeUnit.MILLISECONDS.toNanos(newDelay);
        long now = now();
        if (Long.MAX_VALUE - now < newDelay)
            newDelay = Long.MAX_VALUE - now;
        long newTime = now + newDelay;
        if (future == null) {
            nextExecuteTime = newTime;
            if (newDelay > 0)
                future = scheduler.schedule(d, newDelay, TimeUnit.NANOSECONDS);
            else
                scheduler.execute(d);
            return true;
        } else if (newTime > nextExecuteTime) {
            nextExecuteTime = newTime;
            return true;
        }
        return false;
    }

    /**
     * Changes the execution time of this Periodic task if it is sooner than the
     * current execution time.
     * 
     * @param newDelay the new delay from now when this should be executed
     * @return true if the execution time changed
     */
    public synchronized boolean rescheduleIfSooner(long newDelay) {
        // if not scheduled at all, just schedule
        if (future == null)
            return rescheduleIfLater(newDelay);

        newDelay = TimeUnit.MILLISECONDS.toNanos(newDelay);
        long now = now();
        if (Long.MAX_VALUE - now < newDelay)
            newDelay = Long.MAX_VALUE - now;
        long newTime = now + newDelay;
        if (newTime < nextExecuteTime) {
            future.cancel(false);
            nextExecuteTime = newTime;
            if (newDelay > 0)
                future = scheduler.schedule(d, newDelay, TimeUnit.NANOSECONDS);
            else {
                future = null;
                scheduler.execute(d);
            }
            return true;
        }
        return false;
    }

    /**
     * Cancels any scheduled execution of the task.
     */
    public synchronized void unschedule() {
        if (future != null) {
            future.cancel(false);
            future = null;
            nextExecuteTime = -1;
        }
    }

    private class Delegate implements Runnable {
        private final Runnable r;

        Delegate(Runnable r) {
            this.r = r;
        }

        public void run() {
            synchronized (Periodic.this) {
                future = null;

                long now = now();
                if (now < nextExecuteTime) {
                    future = scheduler.schedule(this, Math.max(0, nextExecuteTime - now),
                            TimeUnit.NANOSECONDS);
                    return;
                }
            }
            r.run();
        }
    }

    protected Runnable getRunnable() {
        return d.r;
    }

}
