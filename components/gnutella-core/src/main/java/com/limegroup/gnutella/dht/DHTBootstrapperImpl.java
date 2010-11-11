package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.BootstrapResult.ResultType;
import org.limewire.mojito.util.ExceptionUtils;
import org.limewire.service.ErrorService;

class DHTBootstrapperImpl implements DHTBootstrapper {
    
    private static final Log LOG = LogFactory.getLog(DHTBootstrapperImpl.class);
    
    /**
     * A list of DHT bootstrap hosts coming from the Gnutella network. 
     * Limit size to 50 for now.
     */
    private final Set<SocketAddress> hosts = new FixedSizeLIFOSet<SocketAddress>(50, EjectionPolicy.FIFO);
    
    /**
     * A flag that indicates whether or not we've tried to
     * bootstrap from the RouteTable 
     */
    private boolean triedRouteTable = false;
    
    /**
     * The future of the ping process
     */
    private DHTFuture<PingResult> pingFuture;
    
    /**
     * A flag that indicates whether or not the current
     * pingFuture (see above) is pinging Nodes from the
     * RouteTable
     */
    private boolean fromRouteTable = false;
    
    /**
     * The future of the bootstrap process
     */
    private DHTFuture<BootstrapResult> bootstrapFuture;
    
    /**
     * The DHT controller
     */
    private final DHTController controller;

    /**
     * The DHTNodeFetcher instance
     */
    private DHTNodeFetcher nodeFetcher;
    
    /**
     * The lock Object
     */
    private final Object lock = new Object();
    
    private final DHTNodeFetcherFactory dhtNodeFetcherFactory;
    
    public DHTBootstrapperImpl(DHTController controller,
            DHTNodeFetcherFactory dhtNodeFetcherFactory) {
        this.controller = controller;
        this.dhtNodeFetcherFactory = dhtNodeFetcherFactory;
    }
    
    /**
     * Boostraps in the following order:
     * <ol>
     * <li>If we have received hosts from the Gnutella network, try them.
     * <li>Else try the persisted routing table.
     * <li>Else try the SIMPP list.
     * <li>Else start node fetcher and wait for hosts coming from the network.
     * </ol>
     * If at any moment while bootstraping from the routing table 
     * we receive nodes from the network, it should pre-empt the existing
     * bootstrap and start with them. This is achieved by calling cancel on 
     * the future.
     */
    public void bootstrap() {
        synchronized (lock) {
            
            if (getMojitoDHT().isBootstrapped()) {
                return;
            }
            
            if (hosts.isEmpty()) {
                tryBootstrapFromRouteTable();
            } else {
                tryBootstrapFromHostsSet();
            }
        }
    }
    
    /**
     * Adds a host to the head of a list of bootstrap hosts ordered by Most Recently Seen.
     * If this bootstrapper is waiting for hosts or is bootstrapping from the persisted RT, 
     * this method tries to bootstrap immediately after.
     * 
     * @param hostAddress the SocketAddress of the new bootstrap host.
     */
    public void addBootstrapHost(SocketAddress hostAddress) {
        synchronized (lock) {
            
            if (!getMojitoDHT().isRunning() || getMojitoDHT().isBootstrapped()) {
                return;
            }
            
            if(LOG.isDebugEnabled()) {
                LOG.debug("Adding host: "+hostAddress);
            }
            
            hosts.add(hostAddress);
            tryBootstrapFromHostsSet();
        }
    }
    
    public void addPassiveNode(SocketAddress hostAddress) {
        synchronized (lock) {
            if (nodeFetcher == null || !isWaitingForNodes()) {
                return;
            }
            
            nodeFetcher.requestDHTHosts(hostAddress);
        }
    }
    
    /**
     * Stops bootstrapping
     */
    public void stop() {
        synchronized (lock) {
            if (pingFuture != null) {
                pingFuture.cancel(true);
                pingFuture = null;
            }
            
            if (bootstrapFuture != null) {
                bootstrapFuture.cancel(true);
                bootstrapFuture = null;
            }
            
            stopNodeFetcher();
            triedRouteTable = false;
            fromRouteTable = false;
        }
    }
    
    /**
     * We're waiting for Nodes if:
     * <ol>
     * <li>We're NOT bootstrapped
     * <li>And there's no bootstrap process active OR we are bootstrapping from routetable
     * </ol>
     */
    public boolean isWaitingForNodes() {
        synchronized (lock) {
            return !getMojitoDHT().isBootstrapped() && bootstrapFuture == null;
        }
    }

    /**
     * Tries to bootstrap the local Node from an existing
     * RouteTable.
     */
    private void tryBootstrapFromRouteTable() {
        synchronized (lock) {
            
            // Make sure we try this only once. 
            if (triedRouteTable) {
                return;
            }
            
            if (pingFuture != null || bootstrapFuture != null) {
                return;
            }
            
            pingFuture = getMojitoDHT().findActiveContact();
            pingFuture.addFutureListener(new PongListener(pingFuture));
            
            triedRouteTable = true;
            fromRouteTable = true;
        }
    }
    
    /**
     * Tries to bootstrap the local Node from the hosts Set
     */
    private void tryBootstrapFromHostsSet() {
        synchronized (lock) {
            // We're already in the bootstrapping stage? If so
            // don't bother any further!
            if (bootstrapFuture != null) {
                return;
            }
            
            // Are we already pinging somebody? Interrupt the
            // process in case of RouteTable pings as we've
            // probably more luck with a fresh IP:Port we got
            // from the DHTNodeFetcher!
            if (fromRouteTable) {
                fromRouteTable = false;
                if (pingFuture != null) {
                    pingFuture.cancel(true);
                    pingFuture = null;
                }
            }
            
            if (pingFuture != null) {
                return;
            }
            
            Iterator<SocketAddress> it = hosts.iterator();
            assert (it.hasNext());
            
            SocketAddress addr = it.next();
            it.remove();
            
            pingFuture = getMojitoDHT().ping(addr);
            pingFuture.addFutureListener(new PongListener(pingFuture));
        }
    }
    
    /**
     * Notify our connections and event listeners 
     * that we are now a bootstrapped DHT node 
     */
    private void finish() {
        controller.sendUpdatedCapabilities();
    }
    
    /**
     * Returns MojitoDHT instance
     */
    private MojitoDHT getMojitoDHT() {
        return controller.getMojitoDHT();
    }
    
    /**
     * Stops the DHTNodeFetcher. You must synchronize on
     * the 'lock' Object prior to calling this!
     */
    private void stopNodeFetcher() {
        if (nodeFetcher != null) {
            nodeFetcher.stop();
            nodeFetcher = null;
        }
    }
    
    /**
     * Gets the SIMPP host responsible for the key space containing the local node ID.
     * 
     * Non-private for testing.
     * 
     * @return the SocketAddress of a SIMPP bootstrap host, or null if we don't have any.
     */
    SocketAddress getSimppHost() {
        String[] simppHosts = DHTSettings.DHT_BOOTSTRAP_HOSTS.get();
        List<SocketAddress> list = new ArrayList<SocketAddress>(simppHosts.length);

        for (String hostString : simppHosts) {
            int index = hostString.indexOf(":");
            if(index < 0 || index == hostString.length()-1) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(new UnknownHostException("invalid SIMPP host: " + hostString));
                }
                
                continue;
            }
            
            try {
                String host = hostString.substring(0, index);
                int port = Integer.parseInt(hostString.substring(index+1).trim());
                list.add(new InetSocketAddress(host, port));
            } catch(NumberFormatException nfe) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(new UnknownHostException("invalid host: " + hostString));
                }
            }
        }

        if (list.isEmpty()) {
            return null;
        }
        
        KUID localId = getMojitoDHT().getLocalNodeID();
        
        //each host in the list is responsible for a subspace of the keyspace
        //first 4 bits responsible for dividing the keyspace
        int localPrefix = ((localId.getBytes()[0] & 0xF0) >> 4);
        //now map to hostlist size
        int index = (int)((list.size()/16f) * localPrefix);
        return list.get(index);
    }
    
    /** For testing. */
    boolean isBootstrappingFromRouteTable() {
        return fromRouteTable;
    }
    
    /** For testing. */
    DHTFuture<PingResult> getPingFuture() {
        return pingFuture;
    }
    
    /** For testing */
    DHTFuture<BootstrapResult> getBootstrapFuture() {
        return bootstrapFuture;
    }
    
    /**
     * The PongListener waits for the Ping response (Pong) from a
     * remote Node. If a Node responds we'll begin bootstrapping
     * from it and if it doesn't we'll:
     * <ol>
     * <li>start the DHTNodeFetcher if it isn't already running
     * <li>check the hosts Set for other Nodes and ping 'em
     * </ol>
     */
    private class PongListener extends DHTFutureAdapter<PingResult> {
        
        private final DHTFuture<PingResult> myFuture;
        
        public PongListener(DHTFuture<PingResult> myFuture) {
            this.myFuture = myFuture;
        }
        
        @Override
        protected void operationComplete(FutureEvent<PingResult> event) {
            switch (event.getType()) {
                case SUCCESS:
                    handleFutureSuccess(event.getResult());
                    break;
                case CANCELLED:
                    handleCancellationException();
                    break;
                case EXCEPTION:
                    handleExecutionException(event.getException());
                    break;
            }
        }


        private void handleFutureSuccess(PingResult result) {
            synchronized (lock) {
                if (pingFuture != myFuture) {
                    return;
                }
                
                pingFuture = null;

                // Stop the DHTNodeFetcher, we don't need it anymore
                // as we found a Node that did respond to our initial
                // bootstrap ping
                stopNodeFetcher();
                
                bootstrapFuture = getMojitoDHT().bootstrap(result.getContact());
                bootstrapFuture.addFutureListener(new BootstrapListener());
            }
        }
        
        private void handleExecutionException(ExecutionException e) {
            synchronized (lock) {
                if (pingFuture != myFuture) {
                    return;
                }
                
                pingFuture = null;
                
                if (ExceptionUtils.isCausedBy(e, DHTException.class)
                        || ExceptionUtils.isCausedBy(e, TimeoutException.class)) {
                    // Try to bootstrap from a SIMPP Host if
                    // bootstrapping failed
                    // and try the hosts Set otherwise
                    SocketAddress simpp = null;
                    if (fromRouteTable && (simpp = getSimppHost()) != null) {
                        addBootstrapHost(simpp);
                    } else {
                        retry();
                    }
                } else if (!ExceptionUtils.isCausedBy(e, IllegalArgumentException.class)) {
                    LOG.error("ExecutionException", e);
                    ErrorService.error(e);
                    stop();
                } 
            }
        }
        
        private void retry() {
            // Start the DHTNodeFetcher if it isn't running
            // The NodeFetcher calls addBootstrapHost() which
            // will restart the bootstrapping. Otherwise see
            // if there are entries in the hosts Set
            if (nodeFetcher == null) {
                nodeFetcher = dhtNodeFetcherFactory.createNodeFetcher(DHTBootstrapperImpl.this);
                nodeFetcher.start();
            } else {
                bootstrap();
            }
        }
        
        private void handleCancellationException() {
            synchronized (lock) {
                LOG.debug("Bootstrap Ping Cancelled");
                
                if (pingFuture != myFuture) {
                    return;
                }
                
                stop();
            }
        }
    }
    
    /**
     * The BootstrapListener waits for the result of the bootstrapping result.
     * On a success we'll update our capabilities and if bootstrapping failed
     * we'll just try it again. 
     */
    private class BootstrapListener extends DHTFutureAdapter<BootstrapResult> {
        
        @Override
        protected void operationComplete(FutureEvent<BootstrapResult> event) {
            switch (event.getType()) {
                case SUCCESS:
                    handleFutureSuccess(event.getResult());
                    break;
                case CANCELLED:
                    handleCancellationException();
                    break;
                case EXCEPTION:
                    handleExecutionException(event.getException());
                    break;
            }
        }

        private void handleFutureSuccess(BootstrapResult result) {
            boolean finish = false;
            synchronized (lock) {
                bootstrapFuture = null;

                ResultType type = result.getResultType();
                
                LOG.debug("Future success type: "+ type);
                switch(type) {
                    case BOOTSTRAP_SUCCEEDED:
                        finish = true;
                        break;
                    case BOOTSTRAP_FAILED:
                        // Try again!
                        bootstrap();
                        break;
                    default:
                        //ignore other results
                        break;
                }
            }
            
            // Do not hold 'lock' when calling finish.
            if (finish) {
                finish();
            }
        }
        
        private void handleExecutionException(ExecutionException e) {
            synchronized (lock) {
                LOG.error("ExecutionException", e);
                
                if (!(e.getCause() instanceof DHTException)) {
                    ErrorService.error(e);
                }

                stop();
            }
        }
        
        public void handleCancellationException() {
            synchronized (lock) {
                LOG.debug("Bootstrap Canceled");
                stop();
            }
        }
    }
}
