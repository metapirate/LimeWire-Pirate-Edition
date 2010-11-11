package org.limewire.nio;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A scheduled FutureTask that can be added to ScheduledThreadPoolExecutors.
 * This contains no support for period schedules.
 */
class ScheduledFutureTask<V> extends FutureTask<V> implements ScheduledFuture<V> {

    /** Base of nanosecond timings, to avoid wrapping */
    private static final long NANO_ORIGIN = System.nanoTime();

    /**
     * Sequence number to break scheduling ties, and in turn to guarantee FIFO
     * order among tied entries.
     */
    private static final AtomicLong sequencer = new AtomicLong(0);

    /** Sequence number to break ties FIFO */
    private final long sequenceNumber;

    /** The time the task is enabled to execute in nanoTime units */
    private long time;
    
    /**
     * Creates a one-shot action with given nanoTime-based trigger time
     */
    ScheduledFutureTask(Runnable r, V result, long ns) {
        super(r, result);
        this.time = ns+now();
        this.sequenceNumber = sequencer.getAndIncrement();
    }

    /**
     * Creates a one-shot action with given nanoTime-based trigger
     */
    ScheduledFutureTask(Callable<V> callable, long ns) {
        super(callable);
        this.time = ns+now();
        this.sequenceNumber = sequencer.getAndIncrement();
    }

    public long getDelay(TimeUnit unit) {
        long d = unit.convert(time - now(), TimeUnit.NANOSECONDS);
        return d;
    }

    /**
     * Returns nanosecond time offset by origin
     */
    private final long now() {
        return System.nanoTime() - NANO_ORIGIN;
    }

    public int compareTo(Delayed other) {
        if (other == this) // compare zero ONLY if same object
            return 0;
        ScheduledFutureTask<?> x = (ScheduledFutureTask<?>) other;
        long diff = time - x.time;
        if (diff < 0)
            return -1;
        else if (diff > 0)
            return 1;
        else if (sequenceNumber < x.sequenceNumber)
            return -1;
        else
            return 1;
    }
}