package org.limewire.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inject.EagerSingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.Stopwatch;

@EagerSingleton
class ServiceRegistryImpl implements ServiceRegistry {
    
    private static final Log LOG = LogFactory.getLog(ServiceRegistryImpl.class);
    
    private final List<StagedRegisterBuilderImpl> builders
        = new ArrayList<StagedRegisterBuilderImpl>();

    private final Map<Object, List<ServiceHolder>> services
        = new HashMap<Object, List<ServiceHolder>>();
    
    private final List<ServiceHolder> startedServices
        = new ArrayList<ServiceHolder>();
    
    private final List<ServiceRegistryListener> registryListeners
        = new ArrayList<ServiceRegistryListener>();
        
    public void initialize() {
        // Remove builders & assign services.
        for(Iterator<StagedRegisterBuilderImpl> iter = builders.iterator(); iter.hasNext(); ) {
            StagedRegisterBuilderImpl builder = iter.next();
            Object stage = builder.getCustomStage();
            if(stage == null)
                stage = builder.getStage();
            List<ServiceHolder> servicesInStage = services.get(stage);
            if(servicesInStage == null) {
                servicesInStage = new ArrayList<ServiceHolder>();
                services.put(stage, servicesInStage);
            }
            servicesInStage.add(new ServiceHolder(builder.getService()));
            iter.remove();
        }
        
        // Do the actual initialization.
        
        // First go through built-in stages.
        for(ServiceStage stage : getStagesInOrder()) {
            if(services.get(stage) != null) {
                for(ServiceHolder service : services.get(stage)) {
                    service.init();
                }
            }
        }
        // Then go through custom stages.
        for(Map.Entry<Object, List<ServiceHolder>> entry : services.entrySet()) {
            if(entry.getKey().getClass() != ServiceStage.class) {
                if(entry.getValue() != null) {
                    for(ServiceHolder service : entry.getValue()) {
                        service.init();
                    }
                }
            }
        }
    }
    
    public void start(Object stage) {
        initialize();        
        startStage(stage);
    }
    
    public void start() {
        initialize();
        Stopwatch stopwatch = new Stopwatch(LOG);        
        for(ServiceStage stage : getStagesInOrder()) {
            startStage(stage);
        }
        stopwatch.resetAndLog("started ServiceRegistry");
    }
    
    private void startStage(Object stage) {
        Stopwatch stopwatch = new Stopwatch(LOG);
        List<ServiceHolder> servicedStages = services.get(stage);
        if(servicedStages != null) {
            for(Iterator<ServiceHolder> iter = servicedStages.iterator(); iter.hasNext(); ) {
                ServiceHolder service = iter.next();
                try {
                    service.start();
                    startedServices.add(service);
                } catch (Throwable e) {
                    //catching exception to potentially allow other services to 
                    //startup if there is an error starting any other services.
                    ExceptionUtils.reportOrReturn(e);
                }
                iter.remove();
            }
            for (ServiceHolder startedService : startedServices) {
                try {
                    startedService.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();  // TODO log, throw?
                }
            }
        }
        if(LOG.isTraceEnabled()) {
            stopwatch.resetAndLog("started stage " + stage.toString());
        }
    }
    
    public void stop() {
        Stopwatch stopwatch = new Stopwatch(LOG);       
        for(int i = startedServices.size()-1; i >= 0; i--) {
            startedServices.get(i).stop();
        }
        for(int i = startedServices.size()-1; i >= 0; i--) {
            try {
                startedServices.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO log, throw?
            }
            startedServices.remove(i);
        }
        if(LOG.isTraceEnabled()) {
            stopwatch.resetAndLog("stopped ServiceRegistry");
        }
    }
    
    public StagedRegisterBuilder register(Service service) {
        StagedRegisterBuilderImpl builder = new StagedRegisterBuilderImpl(service);
        builders.add(builder);
        return builder;
    }
    
    public void addListener(ServiceRegistryListener serviceRegistryListener) {
        registryListeners.add(serviceRegistryListener);
    }

    ServiceStage[] getStagesInOrder() {
        return new ServiceStage[] { ServiceStage.EARLY, ServiceStage.NORMAL, ServiceStage.LATE, ServiceStage.VERY_LATE };
    }
    
    private class ServiceHolder {
        private final AnnotatedService service;
        private boolean initted;
        private boolean started;
        private boolean stopped;
        
        public ServiceHolder(Service service) {
            this.service = new AnnotatedService(service);
        }
        
        void init() {
            if(!initted) {
                initted = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.initializing(service);
                }
                service.initialize();
            }
        }

        void start() {
            if(!started) {
                started = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.starting(service);
                }
                service.start();
            }
        }

        void stop() {
            if(!stopped) {
                stopped = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.stopping(service);
                }
                service.stop();
            }
        }
        
        void join() throws InterruptedException {
            service.join();    
        }

        private class AnnotatedService implements Service {
            private final Service service;
            private volatile Thread serviceExecutor;
            private volatile Asynchronous asynchronous;

            AnnotatedService(Service service) {
                this.service = service;
            }
            
            void join() throws InterruptedException {
                if(asynchronous != null) {
                    asynchronous.join().join(serviceExecutor, asynchronous.timeout());
                }
            }

            public void initialize() {
                service.initialize();
            }
                  
            public String getServiceName() {
                return service.getServiceName();
            }

            public void start() {
                asynchronous = getAsynchronousAnnotation("start");
                if(asynchronous != null) {
                    serviceExecutor = asyncStart();
                } else {
                    Stopwatch stopwatch = new Stopwatch(LOG);
                    service.start();
                    if(LOG.isTraceEnabled()) {
                        stopwatch.resetAndLog("started " + service.getClass());
                    }
                }
            }

            private Thread asyncStart() {
                Thread startThread = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        Stopwatch stopwatch = new Stopwatch(LOG);
                        service.start();
                        if(LOG.isTraceEnabled()) {
                            stopwatch.resetAndLog("started " + service.getClass());
                        }
                    }
                }, "ServiceRegistry-start-" + service.getServiceName());
                startThread.setDaemon(asynchronous.daemon());
                startThread.start();
                return startThread;
            }

            public void stop() {
                joinOnStart();
                asynchronous = getAsynchronousAnnotation("stop");
                if(asynchronous != null) {
                    serviceExecutor = asyncStop();
                } else {
                    Stopwatch stopwatch = new Stopwatch(LOG);
                    service.stop();
                    if(LOG.isTraceEnabled()) {
                        stopwatch.resetAndLog("stopped " + service.getClass());
                    }
                }
            }

            private void joinOnStart() {
                if(asynchronous != null) { // annotation on the "start" method
                    try {
                        serviceExecutor.join();
                    } catch (InterruptedException e) {
                        LOG.debug("interrupted while join()'ing on start: ", e);
                    }
                }
            }

            private Thread asyncStop() {
                Thread stopThread = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        Stopwatch stopwatch = new Stopwatch(LOG);
                        service.stop();
                        if(LOG.isTraceEnabled()) {
                            stopwatch.resetAndLog("stopped " + service.getClass());
                        }
                    }
                }, "ServiceRegistry-stop-" + service.getServiceName());
                stopThread.setDaemon(asynchronous.daemon());
                stopThread.start();
                return stopThread;
            }
            
            private Asynchronous getAsynchronousAnnotation(String methodName){
                try {
                    return service.getClass().getMethod(methodName).getAnnotation(Asynchronous.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
