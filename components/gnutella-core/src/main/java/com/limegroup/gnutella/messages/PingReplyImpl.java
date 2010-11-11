package com.limegroup.gnutella.messages;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.limewire.collection.BitNumbers;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.GGEP;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.util.Utilities;

/**
 * A ping reply message, AKA, "pong".  This implementation provides a way
 * to "mark" pongs as being from supernodes.
 */
public class PingReplyImpl extends AbstractMessage implements IpPort, Connectable, PingReply {
    
    /**
     * The list of extra Gnutella ip/ports contained in this reply.
     */
    private final List<IpPort> PACKED_IP_PORTS;
    
    /**
     * The list of extra DHT IP/ports contained in this reply.
     */
    private final List<IpPort> PACKED_DHT_IP_PORTS;
    
    /**
     * The list of extra IP/ports contained in this reply.
     */
    private final List<IpPort> PACKED_UDP_HOST_CACHES;

    /**
     * The IP address to connect to if this is a UDP host cache.
     * Null if this is not a UDP host cache.
     */
    private final String UDP_CACHE_ADDRESS;

    /**
     * Constant for the number of ultrapeer slots for this host.
     */
    private final int FREE_ULTRAPEER_SLOTS;

    /**
     * Constant for the number of free leaf slots for this host.
     */
    private final int FREE_LEAF_SLOTS;

    /** All the data.  We extract the port, IP address, number of files,
     *  and number of kilobytes lazily. */
    private final byte[] PAYLOAD;

    /** The IP string as extracted from payload[2..5].  Cached to avoid
     *  allocations.  LOCKING: obtain this' monitor. */
    private final InetAddress IP;

    /**
     * Constant for the port number of this pong.
     */
    private final int PORT;
    
    /**
     * The address this pong claims to be my external address.
     */
    private final InetAddress MY_IP;
    
    /**
     * The port this pong claims to be my external port.
     */
    private final int MY_PORT;

    /**
     * Constant for the number of shared files reported in the pong.
     */
    private final long FILES;

    /**
     * Constant for the number of shared kilobytes reported in the pong.
     */
    private final long KILOBYTES;

    /**
     * Constant int for the daily average uptime.
     */
    private final int DAILY_UPTIME;

    /**
     * Constant for whether or not the remote node supports unicast.
     */
    private final boolean SUPPORTS_UNICAST;

    /**
     * Constant for the vendor of the remote host.
     */
    private final String VENDOR;

    /**
     * Constant for the major version number reported in the vendor block.
     */
    private final int VENDOR_MAJOR_VERSION;

    /**
     * Constant for the minor version number reported in the vendor block.
     */
    private final int VENDOR_MINOR_VERSION;

    /**
     * Constant for the query key reported for the pong.
     */
    private final AddressSecurityToken QUERY_KEY;
    
    /**
     * Constant for the DHT Version. 
     */
    private final int DHT_VERSION;
    
    /**
     * Constant for the DHT mode (active/passive/none)
     */
    private final DHTMode DHT_MODE;
    
    /** True if the remote host supports TLS. */
    private final boolean TLS_CAPABLE;

    /**
     * Constant boolean for whether or not this pong contains any GGEP
     * extensions.
     */
    private final boolean HAS_GGEP_EXTENSION;

   

    /**
     * Constant for the locale.
     */
    private String CLIENT_LOCALE;
    
    /**
     * The number of free preferenced slots 
     */
    private int FREE_LOCALE_SLOTS;

    /**
     * Sole <tt>PingReply</tt> constructor. This establishes all ping reply
     * invariants.
     * 
     * @param guid the Globally Unique Identifier (GUID) for this message
     * @param ttl the time to live for this message
     * @param hops the hops for this message
     * @param payload the message payload
     * @throws BadPacketException
     */
    protected PingReplyImpl(byte[] guid, byte ttl, byte hops, byte[] payload, GGEP ggep,
            InetAddress ip, Network network, MACCalculatorRepositoryManager manager,
            NetworkInstanceUtils networkInstanceUtils) throws BadPacketException {
        super(guid, Message.F_PING_REPLY, ttl, hops, payload.length, network);
        PAYLOAD = payload;
        PORT = ByteUtils.ushort2int(ByteUtils.leb2short(PAYLOAD, 0));
        FILES = ByteUtils.uint2long(ByteUtils.leb2int(PAYLOAD, 6));
        KILOBYTES = ByteUtils.uint2long(ByteUtils.leb2int(PAYLOAD, 10));

        IP = ip;

        // GGEP parsing
        //GGEP ggep = parseGGEP();
        int dailyUptime = -1;
        boolean supportsUnicast = false;
        String vendor = "";
        int vendorMajor = -1;
        int vendorMinor = -1;
        
        int freeLeafSlots = -1;
        int freeUltrapeerSlots = -1;
        AddressSecurityToken key = null;
        boolean tlsCapable = false;
        
        String locale /** def. val from settings? */
            = ApplicationSettings.DEFAULT_LOCALE.get(); 
        int slots = -1; //-1 didn't get it.
        InetAddress myIP=null;
        int myPort=0;
        List<IpPort> packedIPs = Collections.emptyList();
        List<IpPort> packedDHTIPs = Collections.emptyList();
        List<IpPort> packedCaches = Collections.emptyList();
        String cacheAddress = null;
        
        int dhtVersion = -1;
        DHTMode dhtMode = null;
        
        // TODO: the exceptions thrown here are messy
        if(ggep != null) {
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME)) {
                try {
                    dailyUptime = 
                        ggep.getInt(GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME); 
                } catch(BadGGEPPropertyException e) {}
            }

            supportsUnicast = ggep.hasKey(GGEPKeys.GGEP_HEADER_UNICAST_SUPPORT);

            if (ggep.hasKey(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT);
                    key = new AddressSecurityToken(bytes, manager);
                } catch (InvalidSecurityTokenException e) {
                    throw new BadPacketException("invalid query key");
                } catch (BadGGEPPropertyException e) {
                    throw new BadPacketException("invalid query key");
                }
            }
            
            if(ggep.hasKey((GGEPKeys.GGEP_HEADER_UP_SUPPORT))) {
                try {
                    byte[] bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_UP_SUPPORT);
                    if(bytes.length >= 3) {
                        freeLeafSlots = bytes[1];
                        freeUltrapeerSlots = bytes[2];
                    }
                } catch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hasKey((GGEPKeys.GGEP_HEADER_DHT_SUPPORT))) {
                try {
                    byte[] bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_DHT_SUPPORT);
                    if(bytes.length >= 3) {
                        dhtVersion = ByteUtils.ushort2int(ByteUtils.beb2short(bytes, 0));
                        byte mode = (byte)(bytes[2] & DHTMode.DHT_MODE_MASK);
                        dhtMode = DHTMode.valueOf(mode);
                        if (dhtMode == null) {
                            // Reset the Version number if the mode
                            // is unknown
                            dhtVersion = -1;
                        }
                    }
                } catch (BadGGEPPropertyException e) {}
            }
            
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_CLIENT_LOCALE)) {
                try {
                    byte[] bytes = ggep.getBytes(GGEPKeys.GGEP_HEADER_CLIENT_LOCALE);
                    if(bytes.length >= 2)
                        locale = StringUtils.getASCIIString(bytes, 0, 2);
                    if(bytes.length >= 3)
                        slots = ByteUtils.ubyte2int(bytes[2]);
                } catch(BadGGEPPropertyException e) {}
            }
            
            if (ggep.hasKey(GGEPKeys.GGEP_HEADER_IPPORT)) {
                try{
                    byte[] data = ggep.getBytes(GGEPKeys.GGEP_HEADER_IPPORT);

                    byte [] myip = new byte[4];
                    // only copy the addr if the data is at least 6
                    // bytes (ip + port).  that way isValidAddress
                    // will fail & we don't need to recheck the length
                    // when getting the port.
                    if(data.length >= 6)
                        System.arraycopy(data,0,myip,0,4);
                    
                    if (NetworkUtils.isValidAddress(myip)) {
                        try{
                            myIP = InetAddress.getByAddress(myip);
                            myPort = ByteUtils.ushort2int(ByteUtils.leb2short(data,4));
                            
                            if (networkInstanceUtils.isPrivateAddress(myIP) ||
                                    !NetworkUtils.isValidPort(myPort) ) {
                                // liars, or we are behind a NAT and there is LAN outside
                                // either way we can't use it
                                myIP=null;
                                myPort=0;
                            }
                            
                        }catch(UnknownHostException bad) {
                            //keep the ip address null and the port 0
                        }
                    }
                }catch(BadGGEPPropertyException ignored) {}
            }
            
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE)) {
                cacheAddress = "";
                try {
                    cacheAddress = ggep.getString(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE);
                } catch(BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS)) {
                try {
                    byte[] data = ggep.getBytes(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS);
                    packedIPs = NetworkUtils.unpackIps(data);
                } catch(BadGGEPPropertyException bad) {
                } catch(InvalidDataException bpe) {}
                
                if(ggep.hasKey(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS_TLS)) {
                    try {
                        byte[] data = ggep.getBytes(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS_TLS);
                        packedIPs = decoratePackedIPs(data, packedIPs);
                    } catch(BadGGEPPropertyException bad) {
                    }
                }
            }
            
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_DHT_IPPORTS)) {
                try {
                    byte[] data = ggep.getBytes(GGEPKeys.GGEP_HEADER_DHT_IPPORTS);
                    packedDHTIPs = NetworkUtils.unpackIps(data);
                } catch(BadGGEPPropertyException bad) {
                } catch(InvalidDataException bpe) {
                }
            }
            
            if(ggep.hasKey(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES)) {
                try {
                    String data = ggep.getString(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES);
                    packedCaches = listCaches(data);
                } catch(BadGGEPPropertyException bad) {}
            }
            
            tlsCapable = ggep.hasKey(GGEPKeys.GGEP_HEADER_TLS_CAPABLE);
        }
        
        MY_IP = myIP;
        MY_PORT = myPort;
        HAS_GGEP_EXTENSION = ggep != null;
        DAILY_UPTIME = dailyUptime;
        SUPPORTS_UNICAST = supportsUnicast;
        VENDOR = vendor;
        VENDOR_MAJOR_VERSION = vendorMajor;
        VENDOR_MINOR_VERSION = vendorMinor;
        QUERY_KEY = key;
        FREE_LEAF_SLOTS = freeLeafSlots;
        FREE_ULTRAPEER_SLOTS = freeUltrapeerSlots;
        CLIENT_LOCALE = locale;
        FREE_LOCALE_SLOTS = slots;
        if(cacheAddress != null && "".equals(cacheAddress))
            UDP_CACHE_ADDRESS = getAddress();
        else
            UDP_CACHE_ADDRESS = cacheAddress;
        PACKED_IP_PORTS = packedIPs;
        PACKED_DHT_IP_PORTS = packedDHTIPs;
        PACKED_UDP_HOST_CACHES = packedCaches;
        DHT_VERSION = dhtVersion;
        DHT_MODE = dhtMode;
        TLS_CAPABLE = tlsCapable;
    }
    
    /** Iterates through the hosts and sets TLS data if the data indicated the host supports TLS. */
    private List<IpPort> decoratePackedIPs(byte[] tlsData, List<IpPort> hosts) {
        if(tlsData.length == 0)
            return hosts;
        
        List<IpPort> decorated = null; 
        BitNumbers tlsBits = new BitNumbers(tlsData);
        int hostIdx = 0;
        for(IpPort next : hosts) {
            if(tlsBits.isSet(hostIdx)) {
                ExtendedEndpoint ep = new ExtendedEndpoint(next.getInetAddress(), next.getPort());
                ep.setTLSCapable(true);
                if(decorated == null) {
                    decorated = new ArrayList<IpPort>(hosts.size());
                    decorated.addAll(hosts.subList(0, hostIdx)); // add all prior hosts.
                }
                decorated.add(ep);
            } else if(decorated != null) {
                decorated.add(next); // preserve decorated
            }
            
            // If we've gone past the end of however much is stored,
            // we're done.
            if(hostIdx >= tlsBits.getMax()) {
                // add the rest of the hosts to the decorated list if necessary
                if(decorated != null && hostIdx+1 < hosts.size())
                    decorated.addAll(hosts.subList(hostIdx+1, hosts.size()));
                break;
            }
            
            hostIdx++;
        }
        
        if(decorated != null) {
            assert decorated.size() == hosts.size() : "decorated: " + decorated + ", hosts: " + hosts;
            return decorated;
        } else {
            return hosts;
        }
    }


    /**
     * Returns whether or not this pong is reporting any free slots on the 
     * remote host, either leaf or ultrapeer.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf or ultrapeer
     *  slots, otherwise <tt>false</tt>
     */
    public boolean hasFreeSlots() {
        return hasFreeLeafSlots() || hasFreeUltrapeerSlots();    
    }
    
    /**
     * Returns whether or not this pong is reporting free leaf slots on the 
     * remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free leaf slots, 
     *  otherwise <tt>false</tt>
     */
    public boolean hasFreeLeafSlots() {
        return FREE_LEAF_SLOTS > 0;
    }

    /**
     * Returns whether or not this pong is reporting free ultrapeer slots on  
     * the remote host.
     * 
     * @return <tt>true</tt> if the remote host has any free ultrapeer slots, 
     *  otherwise <tt>false</tt>
     */
    public boolean hasFreeUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS > 0;
    }
    
    /**
     * Accessor for the number of free leaf slots reported by the remote host.
     * This will return -1 if the remote host did not include the necessary 
     * GGEP block reporting slots.
     * 
     * @return the number of free leaf slots, or -1 if the remote host did not
     *  include this information
     */
    public int getNumLeafSlots() {
        return FREE_LEAF_SLOTS;
    }

    /**
     * Accessor for the number of free ultrapeer slots reported by the remote 
     * host.  This will return -1 if the remote host did not include the  
     * necessary GGEP block reporting slots.
     * 
     * @return the number of free ultrapeer slots, or -1 if the remote host did 
     *  not include this information
     */    
    public int getNumUltrapeerSlots() {
        return FREE_ULTRAPEER_SLOTS;
    }

    @Override
    protected void writePayload(OutputStream out) throws IOException {
        out.write(PAYLOAD);
    }

    /**
     * Accessor for the port reported in this pong.
     *
     * @return the port number reported in the pong
     */
    public int getPort() {
        return PORT;
    }

    /**
     * Returns the IP field in standard dotted decimal format, e.g.,
     * "127.0.0.1".  The most significant byte is written first.
     */
    public String getAddress() { 
        return IP.getHostAddress();
    }

    /**
     * Returns the IP address bytes (MSB first).
     */
    public byte[] getIPBytes() {
        byte[] ip=new byte[4];
        ip[0]=PAYLOAD[2];
        ip[1]=PAYLOAD[3];
        ip[2]=PAYLOAD[4];
        ip[3]=PAYLOAD[5];
        
        return ip;
    }
    
    /**
     * Accessor for the number of files shared, as reported in the
     * pong.
     *
     * @return the number of files reported shared
     */
    public long getFiles() {
        return FILES;
    }

    /**
     * Accessor for the number of kilobytes shared, as reported in the
     * pong.
     *
     * @return the number of kilobytes reported shared
     */
    public long getKbytes() {
        return KILOBYTES;
    }

    /** Returns the average daily uptime in seconds from the GGEP payload.
     *  If the pong did not report a daily uptime, returns -1.
     *
     * @return the daily uptime reported in the pong, or -1 if the uptime
     *  was not present or could not be read
     */
    public int getDailyUptime() {
        return DAILY_UPTIME;
    }


    /** Returns whether or not this host support unicast, GUESS-style
     *  queries.
     *
     * @return <tt>true</tt> if this host does support GUESS-style queries,
     *  otherwise <tt>false</tt>
     */
    public boolean supportsUnicast() {
        return SUPPORTS_UNICAST;
    }

    /** Returns the AddressSecurityToken (if any) associated with this pong.  May be null!
     *
     * @return the <tt>AddressSecurityToken</tt> for this pong, or <tt>null</tt> if no
     *  key was specified
     */
    public AddressSecurityToken getQueryKey() {
        return QUERY_KEY;
    }
    
    /**
     * Gets the list of packed IP/Ports.
     */
    public List<IpPort> getPackedIPPorts() {
        return PACKED_IP_PORTS;
    }
    
    /**
     * Gets the list of packed DHT IP/Ports.
     */
    public List<IpPort> getPackedDHTIPPorts() {
        return PACKED_DHT_IP_PORTS;
    }
    
    /**
     * Gets a list of packed IP/Ports of UDP Host Caches.
     */
    public List<IpPort> getPackedUDPHostCaches() {
        return PACKED_UDP_HOST_CACHES;
    }
    
    public DHTMode getDHTMode() {
        return DHT_MODE;
    }
    
    public int getDHTVersion() {
        return DHT_VERSION;
    }

    /**
     * Returns whether or not this pong has a GGEP extension.
     *
     * @return <tt>true</tt> if the pong has a GGEP extension, otherwise
     *  <tt>false</tt>
     */
    public boolean hasGGEPExtension() {
        return HAS_GGEP_EXTENSION;
    }
    
    /**
     * Unzips data about UDP host caches & returns a list of'm.
     */
    private List<IpPort> listCaches(String allCaches) {
        List<IpPort> theCaches = new LinkedList<IpPort>();
        StringTokenizer st = new StringTokenizer(allCaches, "\n");
        while(st.hasMoreTokens()) {
            String next = st.nextToken();
            // look for possible features and ignore'm
            int i = next.indexOf("&");
            // basically ignore.
            if(i != -1)
                next = next.substring(0, i);
            i = next.indexOf(":");
            int port = 6346;
            if(i == 0 || i == next.length()) {
                continue;
            } else if(i != -1) {
                try {
                    port = Integer.valueOf(next.substring(i+1)).intValue();
                } catch(NumberFormatException invalid) {
                    continue;
                }
            } else {
                i = next.length(); // setup for i-1 below.
            }
            if(!NetworkUtils.isValidPort(port))
                continue;
            String host = next.substring(0, i);
            try {
                theCaches.add(new IpPortImpl(host, port));
            } catch(UnknownHostException invalid) {
                continue;
            }
        }
        return Collections.unmodifiableList(theCaches);
    }


    ////////////////////////// Pong Marking //////////////////////////

    /** 
     * Returns true if this message is "marked", i.e., likely from an
     * Ultrapeer. 
     *
     * @return <tt>true</tt> if this pong is marked as an Ultrapeer pong,
     *  otherwise <tt>false</tt>
     */
    public boolean isUltrapeer() {
        //Returns true if kb is a power of two greater than or equal to eight.
        long kb = getKbytes();
        if (kb < 8)
            return false;
        return Utilities.isPowerOf2(ByteUtils.long2int(kb));
    }

    // overrides Object.toString
    @Override
    public String toString() {
        return "PingReply("+getAddress()+":"+getPort()+
            ", free ultrapeers slots: "+hasFreeUltrapeerSlots()+
            ", free leaf slots: "+hasFreeLeafSlots()+
            ", vendor: "+VENDOR+" "+VENDOR_MAJOR_VERSION+"."+
                VENDOR_MINOR_VERSION+
            ", "+super.toString()+
            ", locale : " + CLIENT_LOCALE + ")";
    }

    /**
     * Implements <tt>IpPort</tt> interface.  Returns the <tt>InetAddress</tt>
     * for this host.
     * 
     * @return the <tt>InetAddress</tt> for this host
     */ 
    public InetAddress getInetAddress() {
        return IP;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    @Override
    public String getAddressDescription() {
        return getInetSocketAddress().toString();
    }

    public InetAddress getMyInetAddress() {
        return MY_IP;
    }
    
    public int getMyPort() {
        return MY_PORT;
    }
    
    /**
     * Access the client_locale.
     */
    public String getClientLocale() {
        return CLIENT_LOCALE;
    }

    public int getNumFreeLocaleSlots() {
        return FREE_LOCALE_SLOTS;
    }
    
    /**
     * Accessor for host cacheness.
     */
    public boolean isUDPHostCache() {
        return UDP_CACHE_ADDRESS != null;
    }
    
    /**
     * Gets the UDP host cache address.
     */
    public String getUDPCacheAddress() {
        return UDP_CACHE_ADDRESS;
    }
    
    /** Returns true if the host supports TLS. */
    public boolean isTLSCapable() {
        return TLS_CAPABLE;
    }
    
    public byte[] getPayload() {
        return PAYLOAD;
    }
    
    @Override
    public Class<? extends Message> getHandlerClass() {
        return PingReply.class;
    }
}
