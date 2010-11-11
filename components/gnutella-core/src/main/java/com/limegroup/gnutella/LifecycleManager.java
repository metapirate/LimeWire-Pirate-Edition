package com.limegroup.gnutella;

import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.listener.EventListener;

/**
 * Defines the interface for the start up and shutdown of a LimeWire instance.
 */
public interface LifecycleManager {

    public boolean isLoaded();

    public boolean isStarted();

    public boolean isShutdown();

    /** Phase 1 of the startup process -- loads any tasks that can be run in the background. */
    public void loadBackgroundTasks();

    /** The core of the startup process, initializes all classes. */
    public void start();

    /**
     * Shuts down anything that requires shutdown.
     */
    // TODO: Make all of these things Shutdown Items.

    public void shutdown();

    /** Shuts down & executes something after shutdown completes. */
    public void shutdown(String toExecute);

    /** Gets the time this finished starting. */
    public long getStartFinishedTime();

    public void addListener(EventListener<LifeCycleEvent> listener);
    
    public boolean removeListener(EventListener<LifeCycleEvent> listener);

}