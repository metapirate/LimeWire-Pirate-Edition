package org.limewire.lifecycle;

public interface ServiceRegistry {
    
    /** Registers a service to be started in when the registry starts. */
    StagedRegisterBuilder register(Service service);
    
    /** Runs initialize on all registered services. */
    void initialize();
    
    /** Starts all services that are *not* registered with a custom stage. */
    void start();
    
    /** Stops all services that were started in the reverse order than they were started. */
    void stop();
    
    /** Starts all services within the given stage. */
    void start(Object stage);
    
    /** Adds a listener that will be notified of changes in the registry's state. */
    void addListener(ServiceRegistryListener serviceRegistryListener);
    

}
