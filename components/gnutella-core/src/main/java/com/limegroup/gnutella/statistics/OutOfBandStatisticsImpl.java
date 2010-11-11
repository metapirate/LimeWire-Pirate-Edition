package com.limegroup.gnutella.statistics;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SearchSettings;

import com.google.inject.Singleton;

/** A default implementation of {@link OutOfBandStatistics} */
@Singleton
class OutOfBandStatisticsImpl implements OutOfBandStatistics {
    
    private static final Log LOG = LogFactory.getLog(OutOfBandStatisticsImpl.class);
    
    private static final int SAMPLE_SIZE = 500;
    
    private AtomicInteger sampleSize = new AtomicInteger(SAMPLE_SIZE);
    private AtomicInteger requested = new AtomicInteger(0);
    private AtomicInteger received = new AtomicInteger(0);
    private AtomicInteger bypassed = new AtomicInteger(0);
    private AtomicInteger sent = new AtomicInteger(0);
    
    public void addBypassedResponse(int numBypassed) {
        bypassed.addAndGet(numBypassed);
    }
    
    public void addReceivedResponse(int numReceived) {
        received.addAndGet(numReceived);
    }
    
    public void addRequestedResponse(int numRequested) {
        requested.addAndGet(numRequested);
    }
    
    public void addSentQuery() {
        sent.incrementAndGet();
    }
    
    public int getRequestedResponses() {
        return requested.get();
    }
    
    public int getSampleSize() {
        return sampleSize.get();
    }
    
    public void increaseSampleSize() {
        sampleSize.addAndGet(SAMPLE_SIZE);
    }

    public double getSuccessRate() {
        double numRequested = requested.doubleValue();
        double numReceived  = received.doubleValue();
        return (numReceived/numRequested) * 100;
    }
    
    public boolean isSuccessRateGood() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get()) {
            LOG.debug("Assuming OOB success rate is good");
            return true;
        }
        int threshold = SearchSettings.OOB_SUCCESS_RATE_GOOD.getValue();
        boolean good = getSuccessRate() > threshold;
        if(LOG.isDebugEnabled()) {
            LOG.debug("OOB success rate of " +
                    getSuccessRate() + "% is " +
                    (good ? "" : "not ") + "good"); 
        }
        return good;
    }
    
    public boolean isSuccessRateGreat() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get()) {
            LOG.debug("Assuming OOB success rate is great");
            return true;
        }
        int threshold = SearchSettings.OOB_SUCCESS_RATE_GREAT.getValue();
        boolean great = getSuccessRate() > threshold;
        if(LOG.isDebugEnabled()) {
            LOG.debug("OOB success rate of " +
                    getSuccessRate() + "% is " +
                    (great ? "" : "not ") + "great"); 
        }
        return great;
    }
    
    public boolean isSuccessRateTerrible() {
        // we want a large enough sample space.....
        if (requested.get() < sampleSize.get()) {
            LOG.debug("Assuming OOB success rate is not terrible");
            return false;
        }
        int threshold = SearchSettings.OOB_SUCCESS_RATE_TERRIBLE.getValue();
        boolean terrible = getSuccessRate() < threshold; 
        if(LOG.isDebugEnabled()) {
            LOG.debug("OOB success rate of " +
                    getSuccessRate() + "% is " +
                    (terrible ? "" : "not ") + "terrible"); 
        }
        return terrible;
    }
    
    public boolean isOOBEffectiveForProxy() {
        if(SearchSettings.FORCE_OOB.getValue())
            return true;
        boolean effective = !((sent.get() > 40) && (requested.get() == 0));
        if(LOG.isDebugEnabled()) {
            LOG.debug("Sent " + sent.get() + " queries, requested " +
                    requested.get() + " responses, OOB is " +
                    (effective ? "" : "not ") + "effective for proxying");
        }
        return effective;
    }

    public boolean isOOBEffectiveForMe() {
        if(SearchSettings.FORCE_OOB.getValue())
            return true;
        boolean effective = !((sent.get() > 20) && (requested.get() == 0));
        if(LOG.isDebugEnabled()) {
            LOG.debug("Sent " + sent.get() + " queries, requested " +
                    requested.get() + " responses, OOB is " +
                    (effective ? "" : "not ") + "effective for me");
        }
        return effective;
    }

}
