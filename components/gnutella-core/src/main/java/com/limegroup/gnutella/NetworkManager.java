package com.limegroup.gnutella;

import java.io.IOException;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Service;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.TLSManager;
import org.limewire.net.address.AddressEvent;

public interface NetworkManager extends Service, ListenerSupport<AddressEvent>, TLSManager {

    /** @return true if your IP and port information is valid.
     */
    public boolean isIpPortValid();

    public GUID getUDPConnectBackGUID();

    /** 
     * Returns whether or not this node is capable of performing OOB queries.
     */
    public boolean isOOBCapable();

    /**
     * Returns whether or not this node is capable of sending its own
     * GUESS queries.  This would not be the case only if this node
     * has not successfully received an incoming UDP packet.
     *
     * @return <tt>true</tt> if this node is capable of running its own
     *  GUESS queries, <tt>false</tt> otherwise
     */
    public boolean isGUESSCapable();

    /**
     * Returns the Non-Forced port for this host.
     *
     * @return the non-forced port for this host
     */
    public int getNonForcedPort();

    /**
     * Returns the port used for downloads and messaging connections.
     * Used to fill out the My-Address header in ManagedConnection.
     * @see com.limegroup.gnutella.Acceptor#getPort
     */
    public int getPort();

    /**
     * Returns the Non-Forced IP address for this host.
     *
     * @return the non-forced IP address for this host
     */
    public byte[] getNonForcedAddress();

    /**
     * Returns the raw IP address for this host.
     *
     * @return the raw IP address for this host
     */
    public byte[] getAddress();

    /**
     * Returns the external IP address for this host.
     */
    public byte[] getExternalAddress();

    /**
     * Notification that we've either just set or unset acceptedIncoming.
     */
    public boolean incomingStatusChanged();

    /**
     * Notifies components that this' IP address has changed.
     */
    // TODO: Convert to listener pattern
    public boolean addressChanged();
    
    /**
     * used to notify the <code>NetworkManager</code> of a change in
     * the external IP.
     */
    public void externalAddressChanged();

    /**
     * used to notify the <code>NetworkManager</code> of a change in
     * the port.
     */
    public void portChanged();

    /**
     * used to notify the <code>NetworkManager</code> of a new
     * <code>MediatorAddress</code> (i.e., push proxy)
     */
    public void newPushProxies(Set<Connectable> pushProxies);

    /** 
     * Returns true if this has accepted an incoming connection, and hence
     * probably isn't firewalled.  (This is useful for colorizing search
     * results in the GUI.)
     */
    public boolean acceptedIncomingConnection();

    /**
     * Sets the port on which to listen for incoming connections.
     * If that fails, this is <i>not</i> modified and IOException is thrown.
     * If port==0, tells this to stop listening to incoming connections.
     */
    public void setListeningPort(int port) throws IOException;

    public boolean canReceiveUnsolicited();

    public boolean canReceiveSolicited();

    public boolean canDoFWT();

    public int getStableUDPPort();

    public GUID getSolicitedGUID();
    
    public int supportsFWTVersion();

    /**
     * Returns the external, public address of this peer. Will return an 
     * {@link NetworkUtils#isValidIpPort(org.limewire.io.IpPort) invalid}
     * address if no address is known yet.
     * <p>
     * Will return the external address whether the peer is firewalled or not.
     */
    public Connectable getPublicAddress();

    /**
     * Validates that tls will work, and disables it for the session if it will not. 
     */
    public void validateTLS();
}
