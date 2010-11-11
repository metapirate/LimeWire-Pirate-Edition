package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

/**
 * Bootstraps this DHT node to the network.
 */
interface DHTBootstrapper {
    
    /**
     * Bootstraps this node to the network.
     */
    public void bootstrap();
    
    /**
     * Adds a host to the list of bootstrap hosts 
     * used to bootstrap to the network
     * 
     * @param hostAddress the <tt>SocketAddress</tt> of the bootstrap host
     */
    public void addBootstrapHost(SocketAddress hostAddress);
    
    /**
     * If the bootstrapper is waiting for nodes, pings this host 
     * in order to acquire DHT bootstrap hosts
     * 
     * @param hostAddress the <tt>SocketAddress</tt> of the host to ping
     */
    public void addPassiveNode (SocketAddress hostAddress);
    
    /**
     * Stops the bootstrap process
     */
    public void stop();
    
    /**
     * Returns whether or not the bootstrapper is waiting for 
     * nodes to bootstrap from.
     */
    public boolean isWaitingForNodes();
}
