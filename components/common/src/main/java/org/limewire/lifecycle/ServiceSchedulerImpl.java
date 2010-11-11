package org.limewire.lifecycle;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;

class ServiceSchedulerImpl implements ServiceScheduler {
    
    private final ServiceRegistry serviceRegistry;
    
    @Inject ServiceSchedulerImpl(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    
    @Override
    public StagedRegisterBuilder scheduleAtFixedRate(String commandName, Runnable command, long initialDelay,
            long period, TimeUnit unit, ScheduledExecutorService service) {
        return serviceRegistry.register(new ScheduledService(commandName, true, command, initialDelay, period, unit, service));
    }
    
    @Override
    public StagedRegisterBuilder scheduleWithFixedDelay(String commandName, Runnable command, long initialDelay,
            long delay, TimeUnit unit, ScheduledExecutorService service) {
        return serviceRegistry.register(new ScheduledService(commandName, false, command, initialDelay, delay, unit, service));
    }
    
    private static class ScheduledService implements Service {
        private final String commandName;
        private final boolean fixedRate;
        private final Runnable command;
        private final long initialDelay;
        private final long delayOrPeriod;
        private final TimeUnit timeUnit;
        private final ScheduledExecutorService service;
        private volatile ScheduledFuture<?> future;
        
        public ScheduledService(String commandName, boolean fixedRate, Runnable command, long initialDelay, long delayOrPeriod,
                TimeUnit timeUnit, ScheduledExecutorService service) {
            this.commandName = commandName;
            this.fixedRate = fixedRate;
            this.command = command;
            this.initialDelay = initialDelay;
            this.delayOrPeriod = delayOrPeriod;
            this.timeUnit = timeUnit;
            this.service = service;
        }

        @Override
        public String getServiceName() {
            return commandName;
        }
        
        @Override
        public void initialize() {}
        
        @Override
        public void start() {
            if(fixedRate) {
                future = service.scheduleAtFixedRate(command, initialDelay, delayOrPeriod, timeUnit);
            } else {
                future = service.scheduleWithFixedDelay(command, initialDelay, delayOrPeriod, timeUnit);
            }
            
        }
        @Override
        public void stop() {
            ScheduledFuture<?> localFuture = future;
            if(localFuture != null) {
                localFuture.cancel(false);
                future = null;
            }
        }
        
    }

}
