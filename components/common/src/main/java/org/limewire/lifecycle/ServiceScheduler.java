package org.limewire.lifecycle;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Allows one to easily schedule Runnables on a {@link ScheduledExecutorService}
 * during a Service startup.
 */
public interface ServiceScheduler {

    /**
     * Schedules the command to run once the service starts. The scheduled
     * command will stop when the service stops.
     * 
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long,
     *      TimeUnit)
     * @see ServiceRegistry#register(Service)
     */
    public StagedRegisterBuilder scheduleAtFixedRate(String commandName, Runnable command,
            long initialDelay, long period, TimeUnit unit, ScheduledExecutorService service);

    /**
     * Schedules the command to run once the service starts. The scheduled
     * command will stop when the service stops.
     * 
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long,
     *      long, TimeUnit)
     * @see ServiceRegistry#register(Service)
     */
    public StagedRegisterBuilder scheduleWithFixedDelay(String commandName, Runnable command,
            long initialDelay, long delay, TimeUnit unit, ScheduledExecutorService service);
}
