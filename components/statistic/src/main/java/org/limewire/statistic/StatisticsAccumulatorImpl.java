package org.limewire.statistic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Manages recording {@link Statistic Statistics}.  This will handle
 * ensuring that every statistic added to this accumulator is stored
 * every second.
 */
@EagerSingleton
final class StatisticsAccumulatorImpl implements StatisticAccumulator, Service {
	
    private final ScheduledExecutorService backgroundExecutor;

    private volatile ScheduledFuture<?> scheduledFuture;
    
	private final List<Statistic> basicStatistics = new CopyOnWriteArrayList<Statistic>();
	
	@Inject
	StatisticsAccumulatorImpl(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
	    this.backgroundExecutor = backgroundExecutor;
	}
	
	@Inject void register(ServiceRegistry registry) {
	    registry.register(this);
	}
	
	public void initialize() {
	}
	
	public void start() {
	    if(scheduledFuture != null)
	        throw new IllegalStateException("already started!");
	    scheduledFuture = backgroundExecutor.scheduleWithFixedDelay(new Runner(), 0, 1000, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
	    ScheduledFuture<?> f = scheduledFuture;
	    if(f != null) {
	        f.cancel(false);
	        scheduledFuture = null;
	    }
	}
	
	public String getServiceName() {
	    return I18nMarker.marktr("Statistic Management");
	}

	/* (non-Javadoc)
     * @see org.limewire.statistic.StatisticAccumulator#addBasicStatistic(org.limewire.statistic.Statistic)
     */
	public void addBasicStatistic(Statistic stat) {
		basicStatistics.add(stat);
	}

	private class Runner implements Runnable {
    	public void run() {
            for(Statistic stat : basicStatistics) {
				stat.storeCurrentStat();
    		}
    	}
	}

}
