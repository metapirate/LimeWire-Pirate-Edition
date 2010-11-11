package org.limewire.collection;

import java.util.LinkedList;

/**
 * Measures the average number of elements in a queueing system.
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Little's_law">Little's Law</a> for
 * more information.
 * 
 * <pre>
 * try{
 *         QueueCounter qc = new QueueCounter(10);
 *         System.out.println(&quot;Average size: &quot; + qc.getAverageSize());
 *         for(int i = 0; i &lt; 10; i++){
 *             qc.recordArrival();
 *             Thread.sleep( 1000 );
 *         }
 *         for(int i = 0; i &lt; 10; i++)
 *             qc.recordDeparture();
 *         System.out.println(&quot;Average size: &quot; + qc.getAverageSize());
 *     } catch(Exception e) {
 *         e.printStackTrace();
 *     }               
 * 
 *     Output:
 *         Average size: -1.0
 *         Average size: 5.500395106845166
 * </pre>
 */
public class QueueCounter {
    private LinkedList<Long> arrivals;

    private NumericBuffer<Long> timesInSystem;

    private NumericBuffer<Long> interarrivalTimes;

    private long lastArrival;

    public QueueCounter(int historySize) {
        arrivals = new LinkedList<Long>();
        timesInSystem = new NumericBuffer<Long>(historySize);
        interarrivalTimes = new NumericBuffer<Long>(historySize);
    }

    /**
     * Record an arrival in the system.
     */
    public synchronized void recordArrival() {
        long now = System.nanoTime();
        if (!arrivals.isEmpty())
            interarrivalTimes.add(now - lastArrival);
        arrivals.addLast(now);
        lastArrival = now;
    }

    /**
     * Record a departure from the system.
     */
    public synchronized void recordDeparture() {
        timesInSystem.add(System.nanoTime() - arrivals.removeFirst());
    }

    /**
     * @return the average number of elements in the system.
     */
    public synchronized double getAverageSize() {
        if (timesInSystem.getSize() < timesInSystem.getCapacity())
            return -1;
        return timesInSystem.average().doubleValue() / interarrivalTimes.average().doubleValue();
    }

    /**
     * @return if the system could be considered stale. More specifically, this
     *         returns true if all of the following are true:
     * 
     *         <pre>
     *  1. we have enough historical data and
     *  2. the system has been empty for longer than the period over which the 
     *  historical data was recorded.
     * </pre>
     * 
     *         Note that this is an arbitrary recommendation and may be
     *         completely irrelevant for some purposes.
     */
    public synchronized boolean isStale() {
        if (timesInSystem.size() < timesInSystem.getCapacity())
            return false;
        if (!arrivals.isEmpty())
            return false;
        return System.nanoTime() - lastArrival > interarrivalTimes.sum().longValue()
                + timesInSystem.last();
    }

    /**
     * Forgets all recorded data.
     */
    public synchronized void reset() {
        timesInSystem.clear();
        interarrivalTimes.clear();
        arrivals.clear();
        lastArrival = 0;
    }
}
