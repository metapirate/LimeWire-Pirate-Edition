package org.limewire.http.reactor;

import org.apache.http.params.HttpParams;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeConnectingIOReactorFactory {

    private final SocketsManager socketsManager;

    @Inject
    public LimeConnectingIOReactorFactory(SocketsManager socketsManager) {
        this.socketsManager = socketsManager;
    }
    
    public LimeConnectingIOReactor createIOReactor(HttpParams parameters) {
        return new LimeConnectingIOReactor(parameters, NIODispatcher.instance().getScheduledExecutorService(), socketsManager);
    }
    
}
