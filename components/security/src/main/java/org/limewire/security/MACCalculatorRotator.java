package org.limewire.security;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that hides the rotation of private keys.
 */
class MACCalculatorRotator implements MACCalculatorRepository {
    private final SettingsProvider provider;
    private final MACCalculatorFactory factory;
    private final ScheduledExecutorService scheduler;
    private MACCalculator current, old;
    private final Runnable rotator, expirer;

    /**
     * @param scheduler a <tt>SchedulingThreadPool</tt> that will execute the rotation
     * @param factory something that creates the QKGenerators
     * @param provider a <tt>SettingsProvider</tt>.  The change period must be bigger
     * than the grace period. 
     */
    MACCalculatorRotator(ScheduledExecutorService scheduler, 
            MACCalculatorFactory factory, 
            SettingsProvider provider) {
        this.provider = provider;
        this.factory = factory;
        this.scheduler = scheduler;
        
        if (provider.getGracePeriod() >= provider.getChangePeriod())
            throw new IllegalArgumentException("settings not supported");
        
        rotator = new Runnable() {
            public void run() {
                rotate();
            }
        };
        expirer = new Runnable() {
            public void run() {
                expireOld();
            }
        };
        
        rotate();
    }
    
    public synchronized MACCalculator[] getValidMACCalculators() {
        if (old == null)
            return new MACCalculator[]{current};
        else
            return new MACCalculator[]{current, old};
    }
    
    public synchronized MACCalculator getCurrentMACCalculator() {
        return current;
    }
    
    private void rotate() {
        MACCalculator newKQ = factory.createMACCalculator();
        synchronized(this) {
            old = current;
            current = newKQ;
        }
        scheduler.schedule(rotator, provider.getChangePeriod(), TimeUnit.MILLISECONDS);
        scheduler.schedule(expirer, provider.getGracePeriod(), TimeUnit.MILLISECONDS);
    }
    
    private synchronized void expireOld() {
        old = null;
    }
}
