package org.limewire.core.api.lifecycle;

import org.limewire.listener.EventListener;

public interface LifeCycleManager {
    
    public boolean isLoaded();

    public boolean isStarted();

    public boolean isShutdown();
    
    public void addListener(EventListener<LifeCycleEvent> listener);
    
    public boolean removeListener(EventListener<LifeCycleEvent> listener);
    
}
