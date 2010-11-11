package org.limewire.http.httpclient;

import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.limewire.service.ErrorService;

import com.google.inject.Provider;

/**
 * A <code>ClientConnectionManager</code> that will close idle connections
 */
class ReapingClientConnectionManager extends ThreadSafeClientConnManager {
    private final ScheduledFuture connectionCloserTask;
    private final IdleConnectionCloser connectionCloser;

    public ReapingClientConnectionManager(Provider<SchemeRegistry> schemeRegistry,
            Provider<ScheduledExecutorService> scheduler, Provider<HttpParams> defaultParams) {
        super(schemeRegistry.get());
        connectionCloser = new IdleConnectionCloser();
        // TODO revist - move this until later (eg., getConnection())
        connectionCloserTask = scheduler.get().scheduleWithFixedDelay(connectionCloser, 0L, 10L, TimeUnit.SECONDS);
    }
    
    @Override
    public ClientConnectionRequest requestConnection(HttpRoute route, Object state) {
        // The manager is set in this way b/c it is a
        // bad idea to pass "this" in a constructor
        connectionCloser.setManagerOnce(this);
        return super.requestConnection(route, state);
    }

    @Override
    public void shutdown() {
        connectionCloserTask.cancel(true);
        super.shutdown();
    }
    
    void setSocket(Socket s) {
        SchemeRegistry registry = getSchemeRegistry();
        for (Object o : registry.getSchemeNames()) {
            String name = (String) o;
            Scheme scheme = registry.getScheme(name);
            ((SocketWrapperProtocolSocketFactory) scheme.getSocketFactory()).setSocket(s);
        }
    }
    
    // static - so that passing instances of it to the scheduler in 
    // the ReapingClientConnectionManager constructor
    // does not inadvertantly pass "this" inside a constructor
    private static class IdleConnectionCloser implements Runnable {

        private static final long IDLE_TIME = 30 * 1000; // 30 seconds.
    
        private final AtomicReference<ClientConnectionManager> managerHolder;
    
        IdleConnectionCloser(){
            managerHolder = new AtomicReference<ClientConnectionManager>();
        }

        /**
         * Sets the manager only if one has not yet been set
         */
        void setManagerOnce(ClientConnectionManager manager) {
            managerHolder.compareAndSet(null, manager);
        }
        
        public void run() {
            try {
                ClientConnectionManager manager = managerHolder.get();
                if(manager != null) {
                    manager.closeIdleConnections(IDLE_TIME, TimeUnit.MILLISECONDS);
                }
            } catch (Throwable t) {
                ErrorService.error(t);
            }
        }
    }
}
