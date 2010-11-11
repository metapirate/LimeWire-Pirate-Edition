package org.limewire.core.impl.lifecycle;

import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.listener.EventListener;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.LifecycleManager;

@Singleton
public class LifeCycleManagerImpl implements LifeCycleManager {

    private final LifecycleManager lifecycleManager;

    @Inject
    public LifeCycleManagerImpl(LifecycleManager lifecycleManager) {
        this.lifecycleManager = Objects.nonNull(lifecycleManager, "lifecycleMaanger");
    }

    @Override
    public void addListener(EventListener<LifeCycleEvent> listener) {
        lifecycleManager.addListener(listener);
    }

    @Override
    public boolean isLoaded() {
        return lifecycleManager.isLoaded();
    }

    @Override
    public boolean isShutdown() {
        return lifecycleManager.isShutdown();
    }

    @Override
    public boolean isStarted() {
        return lifecycleManager.isStarted();
    }

    @Override
    public boolean removeListener(EventListener<LifeCycleEvent> listener) {
        return lifecycleManager.removeListener(listener);
    }
}
