package org.limewire.lifecycle;

public interface ServiceRegistryListener {

    void initializing(Service service);
    void starting(Service service);
    void stopping(Service service);
    
}
