package org.limewire.lifecycle;

/**
 * Defines the interface for services that need to be started, stopped and initialized.
 */
public interface Service {

    void start();
    
    void stop();
    
    void initialize();
    
    String getServiceName();

}
