package com.limegroup.gnutella;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.rudp.UDPMultiplexor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.RoutedConnection;

@Singleton
public class ConnectionServicesImpl implements ConnectionServices {
    
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<HostCatcher> hostCatcher;
    private final Provider<UDPMultiplexor> udpMultiplexor;
    private final Provider<UploadManager> uploadManager;
    private final Provider<Acceptor> acceptor;
    private final Provider<SpamServices> spamServices;

    @Inject
    public ConnectionServicesImpl(
            Provider<ConnectionManager> connectionManager,
            Provider<HostCatcher> hostCatcher,
            Provider<UDPMultiplexor> udpMultiplexor,
            Provider<UploadManager> uploadManager, Provider<Acceptor> acceptor,
            Provider<SpamServices> spamServices) {
        this.connectionManager = connectionManager;
        this.hostCatcher = hostCatcher;
        this.udpMultiplexor = udpMultiplexor;
        this.uploadManager = uploadManager;
        this.acceptor = acceptor;
        this.spamServices = spamServices;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isShieldedLeaf()
     */
    public boolean isShieldedLeaf() {
        return connectionManager.get().isShieldedLeaf();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isActiveSuperNode()
     */
    public boolean isActiveSuperNode() {
        return connectionManager.get().isActiveSupernode();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isSupernode()
     */
    public boolean isSupernode() {
        return connectionManager.get().isSupernode();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isConnecting()
     */
    public boolean isConnecting() {
        return connectionManager.get().isConnecting();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isConnected()
     */
    public boolean isConnected() {
    	return connectionManager.get().isConnected();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isFullyConnected()
     */
    public boolean isFullyConnected() {
    	return connectionManager.get().isFullyConnected();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#getPreferencedHosts(boolean, java.lang.String, int)
     */
    public Collection<IpPort> getPreferencedHosts(boolean isUltrapeer, String locale, int num) {
        
        Set<IpPort> hosts = new IpPortSet();
        
        if(isUltrapeer)
            hosts.addAll(hostCatcher.get().getUltrapeersWithFreeUltrapeerSlots(locale,num));
        else
            hosts.addAll(hostCatcher.get().getUltrapeersWithFreeLeafSlots(locale,num));
        
        // If we don't have enough hosts, add more.
        
        if(hosts.size() < num) {
            //we first try to get the connections that match the locale.
            for(IpPort ipp : connectionManager.get().getInitializedConnectionsMatchLocale(locale)) {
                if(hosts.size() >= num)
                    break;
                hosts.add(ipp);
            }
            
            //if we still don't have enough hosts, get them from the list
            //of all initialized connection
            if(hosts.size() < num) {
                for(IpPort ipp : connectionManager.get().getInitializedConnections()) {
                    if(hosts.size() >= num)
                        break;
                    hosts.add(ipp);
                }
            }
        }
        
        return hosts;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#countConnectionsWithNMessages(int)
     */
    public int countConnectionsWithNMessages(int messageThreshold) {
    	return connectionManager.get().countConnectionsWithNMessages(messageThreshold);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#getActiveConnectionMessages()
     */
    public int getActiveConnectionMessages() {
    	return connectionManager.get().getActiveConnectionMessages();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#removeConnection(com.limegroup.gnutella.ManagedConnection)
     */
    public void removeConnection(RoutedConnection c) {
        connectionManager.get().remove(c);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#disconnect()
     */
    public void disconnect() {
    	// Delegate to connection manager
        connectionManager.get().disconnect(false);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#connect()
     */
    public void connect() {
        // Delegate to connection manager
        connectionManager.get().connect();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#isConnectedTo(java.net.InetAddress)
     */
    public boolean isConnectedTo(InetAddress addr) {
        // ideally we would check download sockets too, but
        // because of the way ManagedDownloader is built, it isn't
        // too practical.
        // TODO: rewrite ManagedDownloader
        
        String host = addr.getHostAddress();
        return connectionManager.get().isConnectedTo(host) ||
               udpMultiplexor.get().isConnectedTo(addr) ||
               uploadManager.get().isConnectedTo(addr); // ||
               // dloadManager.isConnectedTo(addr);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#connectToHostAsynchronously(java.lang.String, int, com.limegroup.gnutella.util.SocketsManager.ConnectType)
     */
    public void connectToHostAsynchronously(String hostname, int portnum, ConnectType type) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.0.0.1" since
        //they are aliases for this machine.
    	
        byte[] cIP = null;
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
            cIP = addr.getAddress();
        } catch(UnknownHostException e) {
            return;
        }
        if ((cIP[0] == 127) && (portnum==acceptor.get().getPort(true)) &&
    		ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
    		return;
        } else {
            byte[] managerIP=acceptor.get().getAddress(true);
            if (Arrays.equals(cIP, managerIP)
                && portnum==acceptor.get().getPort(true))
                return;
        }
    
        if (spamServices.get().isAllowed(addr)) {
            connectionManager.get().createConnectionAsynchronously(hostname, portnum, type);
    	}
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionServices#getNumInitializedConnections()
     */
    public int getNumInitializedConnections() {
    	return connectionManager.get().getNumInitializedConnections();
    }

}
