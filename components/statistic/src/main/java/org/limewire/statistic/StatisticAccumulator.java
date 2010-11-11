package org.limewire.statistic;

import org.limewire.lifecycle.Service;

/** Defines the interface from which statistics can be accumulated. */
/* TODO: This shouldn't require it be a service -- the Impl should decide that. */
public interface StatisticAccumulator extends Service {

    /**
     * Adds a <tt>Statistic</tt> to the set of normal (not advanced) 
     * statistics to record.
     *
     * @param stat the <tt>Statistic</tt> to add
     */
    public void addBasicStatistic(Statistic stat);

}