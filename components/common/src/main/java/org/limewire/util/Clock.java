package org.limewire.util;


/**
 * An abstraction for the system clock.
 */
public interface Clock {
    
    /**
     * Returns the current time in milliseconds.  Note that
     * while the unit of time of the return value is a millisecond,
     * the granularity of the value depends on the underlying
     * operating system and may be larger.  For example, many
     * operating systems measure time in units of tens of
     * milliseconds.
     *
     * <p> See the description of the class <code>Date</code> for
     * a discussion of slight discrepancies that may arise between
     * "computer time" and coordinated universal time (UTC).
     *
     * @return  the difference, measured in milliseconds, between
     *          the current time and midnight, January 1, 1970 UTC.
     * @see     java.util.Date
     */
    public long now();
    
    /**
     * Returns the current value of the most precise available system
     * timer, in nanoseconds.
     *
     * <p>This method can only be used to measure elapsed time and is
     * not related to any other notion of system or wall-clock time.
     * The value returned represents nanoseconds since some fixed but
     * arbitrary time (perhaps in the future, so values may be
     * negative).  This method provides nanosecond precision, but not
     * necessarily nanosecond accuracy. No guarantees are made about
     * how frequently values change. Differences in successive calls
     * that span greater than approximately 292 years (2<sup>63</sup>
     * nanoseconds) will not accurately compute elapsed time due to
     * numerical overflow.
     *
     * <p> For example, to measure how long some code takes to execute:
     * <pre>
     *   long startTime = System.nanoTime();
     *   // ... the code being measured ...
     *   long estimatedTime = System.nanoTime() - startTime;
     * </pre>
     * 
     * @return The current value of the system timer, in nanoseconds.
     */
    public long nanoTime();
}
