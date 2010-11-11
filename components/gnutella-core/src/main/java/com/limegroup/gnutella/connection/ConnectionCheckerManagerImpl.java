package com.limegroup.gnutella.connection;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.UploadServices;

@Singleton
public class ConnectionCheckerManagerImpl implements ConnectionCheckerManager {
    
    private static final Log LOG = LogFactory.getLog(ConnectionCheckerManagerImpl.class);

    /**
     * Array of standard internet hosts to connect to when determining whether
     * or not the user has a live Internet connection.  These are randomized
     * so a minimum number is hit on each check.  Note that we only hit one 
     * random server per test and that we only test the connection if we have
     * ample evidence that the users machine is no longer connected, resulting
     * in minimal traffic to these sites.
     */
    private static final String[] STANDARD_HOSTS = {
        "www.wanadoo.fr",
        "www.tiscali.com",
        "www.ntt.com",
        "www.tonline.com",
        "www.download.com",
        "www.ibm.com",
        "www.sun.com",
        "www.apple.com",
        "www.ebay.com",
        "www.sun.com",
        "www.monster.com",
        "www.uunet.com",
        "www.real.com",
        "www.microsoft.com",
        "www.sco.com",
        "www.google.com",
        "www.cnn.com",
        "www.amazon.com",
        "www.espn.com", 
        "www.yahoo.com",
        "www.oracle.com",
        "www.dell.com",
        "www.ge.com",
        "www.sprint.com",
        "www.att.com",
        "www.mci.com",
        "www.cisco.com",
        "www.intel.com",
        "www.motorola.com",
        "www.hp.com",
        "www.gateway.com",
        "www.sony.com",
        "www.ford.com",
        "www.gm.com",
        "www.aol.com",
        "www.verizon.com",
        "www.passport.com",
        "www.go.com",
        "www.overture.com",
        "www.earthlink.net",
        "www.bellsouth.net",
        "www.excite.com",
        "www.paypal.com",
        "www.altavista.com",
        "www.weather.com",
        "www.mapquest.com",
        "www.geocities.com",
        "www.juno.com",
        "www.msnbc.com",
        "www.lycos.com",
        "www.comcast.com",
    };
    
    private final AtomicInteger numWorkarounds = new AtomicInteger();
    
    private volatile ConnectionChecker currentChecker;

    private ConnectionServices connectionServices;

    private Provider<ConnectionManager> connectionManager;

    private UploadServices uploadServices;

    private SocketsManager socketsManager;

    private final DownloadServices downloadServices;

    private final Provider<UDPConnectionChecker> udpConnectionChecker;

    private final ExecutorService executor;

    private volatile boolean connected;

    private volatile Future<Boolean> currentCheckerFuture;

    @Inject
    public ConnectionCheckerManagerImpl(ConnectionServices connectionServices,
            Provider<ConnectionManager> connectionManager,
            UploadServices uploadServices,
            DownloadServices downloadServices,
            SocketsManager socketsManager,
            Provider<UDPConnectionChecker> udpConnectionChecker,
            @Named("unlimitedExecutor") ExecutorService executor) {
        this.connectionServices = connectionServices;
        this.connectionManager = connectionManager;
        this.uploadServices = uploadServices;
        this.downloadServices = downloadServices;
        this.socketsManager = socketsManager;
        this.udpConnectionChecker = udpConnectionChecker;
        this.executor = executor;
    }

    public Future<Boolean> checkForLiveConnection() {
        LOG.debug("Checking for live connection");
    
        boolean startThread = false;
        synchronized (this) {
            if (currentChecker == null) {
                startThread = true;
                currentChecker = new ConnectionChecker(numWorkarounds, getDefaultHosts(),
                        connectionServices, uploadServices, downloadServices, 
                        socketsManager, udpConnectionChecker.get());
            }
        }
        
        // Only create a new thread if one isn't alive.
        if(startThread) {
            LOG.debug("Starting a new connection-checker thread");
            currentCheckerFuture = executor.submit(new ConnectionCheckerRunner());
        }
        
        return currentCheckerFuture;
    }

    public String[] getDefaultHosts() {
        return STANDARD_HOSTS;
    }
    
    public int getNumWorkarounds() {
        return numWorkarounds.get();
    }

    public boolean isConnected() {
        return connected;
    }
    
    private class ConnectionCheckerRunner implements ConnectionCheckerListener, Callable<Boolean> {

        public void connected() {
            LOG.debug("Connected");
            ConnectionCheckerManagerImpl.this.connected = true;
        }

        public void noInternetConnection() {
            LOG.debug("No internet connection");
            ConnectionCheckerManagerImpl.this.connected = false;
            connectionManager.get().noInternetConnection();
        }

        public Boolean call() {
            final ConnectionChecker checker;
            synchronized (ConnectionCheckerManagerImpl.this) {
                checker = currentChecker;
            }
            try {
                checker.run(this);
            } finally {
                synchronized (ConnectionCheckerManagerImpl.this) {
                    currentChecker = null;
                }
            }
            
            return ConnectionCheckerManagerImpl.this.connected; 
        }

    }       
    
}
