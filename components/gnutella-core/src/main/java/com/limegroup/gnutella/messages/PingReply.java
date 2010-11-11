package com.limegroup.gnutella.messages;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public interface PingReply extends Message {

    /**
     * Constant for the standard size of the pong payload.
     */
    public static final int STANDARD_PAYLOAD_SIZE = 14;

    /**
     * Returns whether or not this pong is reporting any free slots on the 
     * remote host, either leaf or ultrapeer.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf or ultrapeer
     *  slots, otherwise <tt>false</tt>
     */
    boolean hasFreeSlots();

    /**
     * Returns whether or not this pong is reporting free leaf slots on the 
     * remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf slots, 
     *  otherwise <tt>false</tt>
     */
    boolean hasFreeLeafSlots();

    /**
     * Returns whether or not this pong is reporting free ultrapeer slots on  
     * the remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free ultrapeer slots, 
     *  otherwise <tt>false</tt>
     */
    boolean hasFreeUltrapeerSlots();

    /**
     * Accessor for the number of free leaf slots reported by the remote host.
     * This will return -1 if the remote host did not include the necessary 
     * GGEP block reporting slots.
     * 
     * @return the number of free leaf slots, or -1 if the remote host did not
     *  include this information
     */
    int getNumLeafSlots();

    /**
     * Accessor for the number of free ultrapeer slots reported by the remote 
     * host.  This will return -1 if the remote host did not include the  
     * necessary GGEP block reporting slots.
     * 
     * @return the number of free ultrapeer slots, or -1 if the remote host did 
     *  not include this information
     */
    int getNumUltrapeerSlots();

    /**
     * Accessor for the port reported in this pong.
     *
     * @return the port number reported in the pong
     */
    int getPort();

    /**
     * Returns the IP field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    String getAddress();

    /**
     * Returns the ip address bytes (MSB first).
     */
    byte[] getIPBytes();

    /**
     * Accessor for the number of files shared, as reported in the
     * pong.
     *
     * @return the number of files reported shared
     */
    long getFiles();

    /**
     * Accessor for the number of kilobytes shared, as reported in the
     * pong.
     *
     * @return the number of kilobytes reported shared
     */
    long getKbytes();

    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  If the pong did not report a daily uptime, returns -1.
     *
     * @return the daily uptime reported in the pong, or -1 if the uptime
     *  was not present or could not be read
     */
    int getDailyUptime();

    /** Returns whether or not this host support unicast, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>false</tt>
     */
    boolean supportsUnicast();

    /** Returns the AddressSecurityToken (if any) associated with this pong.  May be null!
     *
     * @return the <tt>AddressSecurityToken</tt> for this pong, or <tt>null</tt> if no
     *  key was specified
     */
    AddressSecurityToken getQueryKey();

    /**
     * Gets the list of packed IP/Ports.
     */
    List<IpPort> getPackedIPPorts();

    /**
     * Gets the list of packed DHT IP/Ports.
     */
    List<IpPort> getPackedDHTIPPorts();

    /**
     * Gets a list of packed IP/Ports of UDP Host Caches.
     */
    List<IpPort> getPackedUDPHostCaches();

    DHTMode getDHTMode();

    int getDHTVersion();

    /**
     * Returns whether or not this pong has a GGEP extension.
     *
     * @return <tt>true</tt> if the pong has a GGEP extension, otherwise
     *  <tt>false</tt>
     */
    boolean hasGGEPExtension();

    /** 
     * Returns true if this message is "marked", i.e., likely from an
     * Ultrapeer. 
     *
     * @return <tt>true</tt> if this pong is marked as an Ultrapeer pong,
     *  otherwise <tt>false</tt>
     */
    boolean isUltrapeer();

    /**
     * Implements <tt>IpPort</tt> interface.  Returns the <tt>InetAddress</tt>
     * for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */
    InetAddress getInetAddress();

    InetSocketAddress getInetSocketAddress();

    InetAddress getMyInetAddress();

    int getMyPort();

    /**
     * Access the client_locale.
     */
    String getClientLocale();

    int getNumFreeLocaleSlots();

    /**
     * Accessor for host cacheness.
     */
    boolean isUDPHostCache();

    /**
     * Gets the UDP host cache address.
     */
    String getUDPCacheAddress();

    /** Returns true if the host supports TLS. */
    boolean isTLSCapable();

    public byte[] getPayload(); 
}