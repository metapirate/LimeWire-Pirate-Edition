package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;

public interface Acceptor extends SocketProcessor {

    /**
     * @modifes this
     * @effects sets the IP address to use in pongs and query replies.
     *  If addr is invalid or a local address, this is not modified.
     *  This method must be to get around JDK bug #4073539, as well
     *  as to try to handle the case of a computer whose IP address
     *  keeps changing.
     */
    public void setAddress(InetAddress address);

    /**
     * Sets the external address.
     */
    public void setExternalAddress(InetAddress address);

    /**
     * tries to bind the serversocket and create UPnPMappings.
     * call before running.
     */
    public void bindAndStartUpnp();

    /**
     * Launches the port monitoring thread, MulticastService, and UDPService.
     */
    public void start();

    /**
     * Returns whether or not our advertised IP address is the same as what remote peers believe it is.
     */
    public boolean isAddressExternal();

    /**
     * Returns this' external address.
     */
    public byte[] getExternalAddress();

    /**
     * Returns this' address to use for ping replies, query replies,
     * and pushes.
     * 
     * @param preferForcedAddress whether or not to prefer the forced address if the IP address is forced.
     *   If false, the forced IP address will never be returned
     *   If true, the forced IP address will only be returned if one is set.
     */
    public byte[] getAddress(boolean preferForcedAddress);

    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     *
     * @param checkForcedPort if true returns the forced port if forcing an external
     * address and port is enabled
     * @return the listening port
     */
    public int getPort(boolean checkForcedPort);
    
    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager AND the UDPService
     *  is listening.  If either service CANNOT bind TCP/UDP to the port,
     *  <i>neither<i> service is modified and a IOException is throw.
     *  If port==0, tells this to stop listening for incoming GNUTELLA TCP AND
     *  UDP connections/messages.  This is properly synchronized and can be 
     *  called even while run() is being called.  
     */
    public void setListeningPort(int port) throws IOException;

    /**
     * Determines whether or not LimeWire has detected it is firewalled or not.
     */
    public boolean acceptedIncoming();


    /**
     * If we used UPnP Mappings this session, clean them up and revert
     * any relevant settings.
     */
    public void shutdown();
    
    long getIncomingExpireTime();    
    long getTimeBetweenValidates();
    long getWaitTimeAfterRequests();
    void resetLastConnectBackTime();

}