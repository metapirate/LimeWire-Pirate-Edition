package com.limegroup.gnutella.messages;

import java.net.UnknownHostException;
import java.util.Collection;

import org.limewire.io.GGEP;
import org.limewire.io.IpPort;
import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.messages.Message.Network;

public interface PingReplyFactory {

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, TTL & packed hosts.
     */
    public PingReply create(byte[] guid, byte ttl,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts);

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID and ttl.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     */
    public PingReply create(byte[] guid, byte ttl);

    /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL & return address.
     */
    public PingReply create(byte[] guid, byte ttl, IpPort addr);
    
    /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL, return address, local port and local IP.
     */
    public PingReply create(byte[] guid, byte ttl, int localPort,
            byte[] localIp, IpPort addr);

    /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL, return address & packed hosts. Either collection 
     * of hosts can be null!
     */
    public PingReply create(byte[] guid, byte ttl, IpPort returnAddr,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts);
    
   /**
     * Creates a new PingReply for this host with the specified
     * GUID, TTL, local port, local IP return address & packed hosts. Either collection 
     * of hosts can be null!
     */
    public PingReply create(byte[] guid, byte ttl, int localPort,
            byte [] localIP, IpPort returnAddr,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts);
    

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>AddressSecurityToken</tt> for this reply
     */
    public PingReply createQueryKeyReply(byte[] guid, byte ttl,
            AddressSecurityToken key);

    /**
     * Creates a new <tt>PingReply</tt> for this host with the specified
     * GUID, ttl, and query key.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param key the <tt>AddressSecurityToken</tt> for this reply
     */
    public PingReply createQueryKeyReply(byte[] guid, byte ttl, int port,
            byte[] ip, long sharedFiles, long sharedSize, boolean ultrapeer,
            AddressSecurityToken key);

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] address);

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  being an Ultrapeer
     */
    public PingReply createExternal(byte[] guid, byte ttl, int port,
            byte[] address, boolean ultrapeer);

    /**
     * Creates a new <tt>PingReply</tt> for an external node -- the data
     * in the reply will not contain data for this node.  In particular,
     * the data fields are set to zero because we do not know these
     * statistics for the other node.  This is primarily used for testing.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     * @param ultrapeer whether or not we should mark this node as
     *  being an Ultrapeer
     */
    public PingReply createExternal(byte[] guid, byte ttl, int port,
            byte[] address, int uptime, boolean ultrapeer);

    /**
     * Creates a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be called if the caller is sure that the given
     * node is, in fact, a GUESS-capable node.  This method is only used
     * to create pongs for nodes other than ourselves.  
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param ep the <tt>Endpoint</tt> instance containing data about 
     *  the remote host
     */
    public PingReply createGUESSReply(byte[] guid, byte ttl, Endpoint ep)
            throws UnknownHostException;

    /**
     * Creates a new <tt>PingReply</tt> instance for a GUESS node.  This
     * method should only be called if the caller is sure that the given
     * node is, in fact, a GUESS-capable node.  This method is only used
     * to create pongs for nodes other than ourselves.  Given that this
     * reply is for a remote node, we do not know the data for number of
     * shared files, etc, and so leave it blank.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param port the port the remote host is listening on
     * @param address the address of the node
     */
    public PingReply createGUESSReply(byte[] guid, byte ttl, int port,
            byte[] address);

    /**
     * Creates a new pong with the specified data -- used primarily for
     * testing!
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes);

    /**
     * Creates a new ping from scratch with ultrapeer and daily uptime extension
     * data.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension blocks are allocated if dailyUptime is non-negative.  
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGUESSCapable);

    /**
     * Creates a new PingReply with the specified data.
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGuessCapable, String locale, int slots);

    /**
     * Creates a new PingReply with the specified locale.
     *
     * @param guid the sixteen byte message GUID
     * @param ttl the message TTL to use
     * @param port my listening port.  MUST fit in two signed bytes,
     *  i.e., 0 < port < 2^16.
     * @param ip my listening IP address.  MUST be in dotted-quad big-endian,
     *  format e.g. {18, 239, 0, 144}.
     * @param files the number of files I'm sharing.  Must fit in 4 unsigned
     *  bytes, i.e., 0 < files < 2^32.
     * @param kbytes the total size of all files I'm sharing, in kilobytes.
     *  Must fit in 4 unsigned bytes, i.e., 0 < files < 2^32.
     * @param isUltrapeer true if this should be a marked ultrapeer pong,
     *  which sets kbytes to the nearest power of 2 not less than 8.
     * @param dailyUptime my average daily uptime, in seconds, e.g., 
     *  3600 for one hour per day.  Negative values mean "don't know".
     *  GGEP extension blocks are allocated if dailyUptime is non-negative.  
     * @param isGuessCapable guess capable
     * @param locale the locale 
     * @param slots the number of locale preferencing slots available
     * @param gnutHosts the Gnutella hosts to pack into this PingReply
     * @param dhtHosts the DHT hosts to pack into this PingReply
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] ip,
            long files, long kbytes, boolean isUltrapeer, int dailyUptime,
            boolean isGuessCapable, String locale, int slots,
            Collection<? extends IpPort> gnutHosts,
            Collection<? extends IpPort> dhtHosts);

    /**
     * Creates a new <tt>PingReply</tt> instance with the specified
     * criteria.
     *
     * @return a new <tt>PingReply</tt> instance containing the specified
     *  data
     */
    public PingReply create(byte[] guid, byte ttl, int port, byte[] ipBytes,
            long files, long kbytes, boolean isUltrapeer, GGEP ggep);

    public PingReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload) throws BadPacketException;

    /**
     * Creates a new <tt>PingReply</tt> instance from the network.
     *
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param hops the hops for this message
     * @param payload the message payload
     * @throws <tt>BadPacketException</tt> if the message is invalid for
     *  any reason
     */
    public PingReply createFromNetwork(byte[] guid, byte ttl, byte hops,
            byte[] payload, Network network) throws BadPacketException;

    /**
     * Returns a new <tt>PingReply</tt> instance with all the same data
     * as <tt>pingReply</tt>, but with the specified GUID.
     * @param guid the guid to use for the new <tt>PingReply</tt>
     *
     * @return a new <tt>PingReply</tt> instance with the specified GUID
     *  and all of the data from this <tt>PingReply</tt>
     * @throws IllegalArgumentException if the guid is not 16 bytes or the input
     * (this') format is bad
     */
    public PingReply mutateGUID(PingReply pingReply, byte[] guid);

}
