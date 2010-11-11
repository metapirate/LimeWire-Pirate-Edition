package org.limewire.core.impl.daap;

import java.io.IOException;

import org.limewire.core.api.daap.DaapManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DaapManagerImpl implements DaapManager {

    private final com.limegroup.gnutella.daap.DaapManager daapManager;
    
    @Inject
    public DaapManagerImpl(com.limegroup.gnutella.daap.DaapManager daapManager) {
        this.daapManager = daapManager;
    }
    
    @Override
    public void disconnectAll() {
        daapManager.disconnectAll();
    }

    @Override
    public boolean isServerRunning() {
        return daapManager.isServerRunning();
    }

    @Override
    public void restart() throws IOException {
        daapManager.restart();
    }

    @Override
    public void stop() {
        daapManager.stop();
    }

    @Override
    public void updateService() throws IOException {
        daapManager.updateService();
    }

    @Override
    public void start() throws IOException {
        daapManager.start();
    }
}
