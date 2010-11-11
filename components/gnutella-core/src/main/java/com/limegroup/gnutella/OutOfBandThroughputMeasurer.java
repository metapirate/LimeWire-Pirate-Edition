package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

@EagerSingleton
public class OutOfBandThroughputMeasurer implements Service {
    
    private static final Log LOG = LogFactory.getLog(OutOfBandThroughputMeasurer.class);
    
    private final ScheduledExecutorService backgroundExecutor;
    private final OutOfBandStatistics outOfBandStatistics;

    @Inject
    public OutOfBandThroughputMeasurer(@Named("backgroundExecutor")
    ScheduledExecutorService backgroundExecutor, OutOfBandStatistics outOfBandStatistics) {
        this.backgroundExecutor = backgroundExecutor;
        this.outOfBandStatistics = outOfBandStatistics;
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    public String getServiceName() {
        return "OOB Throughput Measurer";
    }
    
    public void initialize() {
    }
    
    public void stop() {
    }
    

    public void start() {
        Runnable adjuster = new Runnable() {
            public void run() {
                if (LOG.isDebugEnabled())
                    LOG.debug("current success rate " + outOfBandStatistics.getSuccessRate()
                            + " based on " + outOfBandStatistics.getRequestedResponses()
                            + " measurements with a min sample size "
                            + outOfBandStatistics.getSampleSize());
                if (!outOfBandStatistics.isSuccessRateGreat()
                        && !outOfBandStatistics.isSuccessRateTerrible()) {
                    LOG.debug("boosting sample size");
                    outOfBandStatistics.increaseSampleSize();
                }
            }
        };
        
        int thirtyMins = 30 * 60 * 1000;
        backgroundExecutor.scheduleWithFixedDelay(adjuster, thirtyMins, thirtyMins, TimeUnit.MILLISECONDS);
    }
    
}
