package org.limewire.core.api.daap;

import java.io.IOException;

public interface DaapManager {

    public boolean isServerRunning();
    
    public void restart() throws IOException;
    
    public void updateService() throws IOException;
    
    public void stop();
    
    public void disconnectAll();

    void start() throws IOException;
}
