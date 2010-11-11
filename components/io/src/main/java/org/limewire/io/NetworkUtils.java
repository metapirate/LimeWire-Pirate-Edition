package org.limewire.io;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.util.ByteUtils;
import org.limewire.util.Decorator;

/**
 * Provides methods for network programming. 
 * <code>NetworkUtils</code>' methods check the validity of IP addresses, ports
 * and socket addresses. <code>NetworkUtils</code> includes both 
 * IPv4 and 
 * <a href="http://en.wikipedia.org/wiki/IPv6">IPv6</a> compliant methods.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class NetworkUtils {
    
    /**
     * Netmask for Class C Networks.
     */
    public static final int CLASS_C_NETMASK = 0xFFFFFF00;
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private NetworkUtils() {}
    
    /**
     * Determines if the given addr or port is valid.
     * Both must be valid for this to return true.
     */
    public static boolean isValidAddressAndPort(byte[] addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }
    
    /**
     * Determines if the given addr or port is valid.
     * Both must be valid for this to return true.
     */
    public static boolean isValidAddressAndPort(String addr, int port) {
        return isValidAddress(addr) && isValidPort(port);
    }
    
    /** 
     * Determines if the given IpPort is valid. Does resolve address again
     * by name to do so.
     */
    public static boolean isValidIpPort(IpPort ipport) {
        return isValidAddress(ipport.getAddress()) && isValidPort(ipport.getPort());
    }

    /**
     * Returns whether or not the specified port is within the valid range of
     * ports.
     * 
     * @param port
     *            the port number to check
     */
    public static boolean isValidPort(int port) {
        return (port > 0 && port <= 0xFFFF);
    }
	
    /**
     * Returns whether or not the specified address is valid.
     * <p>
     * This method is IPv6 compliant
     */
    public static boolean isValidAddress(byte[] address) {
        return !isAnyLocalAddress(address) 
            && !isInvalidAddress(address)
            && !isBroadcastAddress(address)
            && !isDocumentationAddress(address);
    }
    
    /**
     * Returns whether or not the specified IP is valid.
     */
    public static boolean isValidAddress(IP ip) {
        int msb = (ip.addr >> 24) & 0xFF;
        return (msb != 0x00 && msb != 0xFF);
    }
    
    /**
     * Returns whether or not the specified InetAddress is valid.
     */
    public static boolean isValidAddress(InetAddress address) {
        return !address.isAnyLocalAddress() 
            && !isInvalidAddress(address)
            && !isBroadcastAddress(address)
            && !isDocumentationAddress(address);
    }
    
    /**
     * Returns whether or not the specified host is a valid address.
     */
    public static boolean isValidAddress(String host) {
        try {
            return isValidAddress(InetAddress.getByName(host));
        } catch(UnknownHostException uhe) {
            return false;
        }
    }
	
    /**
     * @return true if the provided string is a dotted ipv4 address
     * of the format "a.b.c.d"
     */
    public static boolean isDottedIPV4(String s) {
        int octets = 0;
        while(octets < 3) {
            int dot = s.indexOf(".");
            if (dot == -1)
                return false;
            String octet = s.substring(0,dot);
            try {

                int parsed = Integer.parseInt(octet);
                if (parsed < 0 || parsed > 255)
                    return false;
            }
            catch (NumberFormatException bad) {
                return false;
            }
            octets++;
            s = s.substring(Math.min(dot+1,s.length()),s.length());
        }
        if (s.indexOf(".") != -1)
            return false;
        try {
            int parsed = Integer.parseInt(s);
            if (parsed < 0 || parsed > 255)
                return false;
        }
        catch (NumberFormatException bad) {
            return false;
        }
        return true;
    }
    
    /**
     * Determines if <code>hostAndPort</code> is either "host" or in
     * "host:port" format in which case the port is checked if it is within a
     * valid range.
     */
    public static boolean isAddress(String hostAndPort) {
        hostAndPort = hostAndPort.trim();
        int i = hostAndPort.indexOf(":");
        if (i == -1) {
            return hostAndPort.length() > 0;
        } else if (i > 0){
            try {
                final int port = Integer.parseInt(hostAndPort.substring(i + 1));
                return isValidPort(port);
            } catch(NumberFormatException e) {
            }            
        }
        return false;
    }

    /**
     * @return whether the IpPort is a valid external address.
     */
    static boolean isValidExternalIpPort(IpPort addr) {
        InetAddress address = addr.getInetAddress();
        return address != null 
            && isValidAddress(address) 
            && isValidPort(addr.getPort());
    }
    
    /**
     * Returns whether or not the specified InetAddress and Port is valid.
     */
    public static boolean isValidSocketAddress(SocketAddress address) {
        InetSocketAddress iaddr = (InetSocketAddress)address;
        
        return !iaddr.isUnresolved()
            && isValidAddress(iaddr.getAddress())
            && isValidPort(iaddr.getPort());
    }
    
    /**
     * Returns true if the InetAddress is any of our local machine addresses
     * 
     * This method is IPv6 compliant
     */
    public static boolean isLocalAddress(InetAddress addr) {
        // There are cases where InetAddress.getLocalHost() returns addresses
        // such as 127.0.1.1 (note the two 1) but if you iterate through all
        // NetworkInterfaces and look at every InetAddress then it's not there
        // and NetworkInterface.getByInetAddress(...) returns null 'cause it
        // cannot find an Interface for it. The following checks take care
        // of this case.
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }
        
        // Note: The ideal way of doing this would be to return:
        //      NetworkInterface.getByInetAddress(addr) != null;
        // However, that call crashes the JVM on a lot of machines.
        // So, we have a crappy & long-winded workaround...
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if(Arrays.equals(addr.getAddress(), address.getAddress())) {
                        return true;
                    }
                }
            }
        } catch(SocketException err) {
            return false;
        }
        
        return false;
    }
    
    /**
     * Returns true if the SocketAddress is any of our local machine addresses
     */
    public static boolean isLocalAddress(SocketAddress addr) {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        return !iaddr.isUnresolved() && isLocalAddress(iaddr.getAddress());
    }
    
    /**
     * Returns whether or not the two IP addresses share the same
     * first octet in their address.  
     * <p>
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in an IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(InetAddress addr0, InetAddress addr1) {
        return isCloseIP(addr0.getAddress(), addr1.getAddress());
    }
    
    /**
     * Returns whether or not the two IP addresses share the same
     * first octet in their address.  
     * <p>
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in an IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isCloseIP(byte[] addr0, byte[] addr1) {
        if ((isIPv4Address(addr0) && isIPv4Address(addr1)) 
                || (isIPv4MappedAddress(addr0) && isIPv4MappedAddress(addr1))) {
            return addr0[/* 0 */ addr0.length - 4] == addr1[/* 0 */ addr1.length - 4];                    
        }
        return false;
    }
    
    /**
     * Returns whether or not the two IP addresses share the same
     * first two octets in their address -- the most common
     * indication that they may be on the same network.
     * <p>
     * Private networks are NOT CONSIDERED CLOSE.
     * <p>
     * This method is IPv6 compliant but returns always false if
     * any of the two addresses in a true IPv6 address.
     * 
     * @param addr0 the first address to compare
     * @param addr1 the second address to compare
     */
    public static boolean isVeryCloseIP(byte[] addr0, byte[] addr1) {
        if ((isIPv4Address(addr0) && isIPv4Address(addr1)) 
                || (isIPv4MappedAddress(addr0) && isIPv4MappedAddress(addr1))) {
            
            return addr0[/* 0 */ addr0.length - 4] == addr1[/* 0 */ addr1.length - 4]
                && addr0[/* 1 */ addr0.length - 3] == addr1[/* 1 */ addr1.length - 3];
        }
        return false;
    }
    
    /**
     * Utility method for determining whether or not the given 
     * address is private taking an InetAddress object as argument
     * like the isLocalAddress(InetAddress) method. 
     * <p>
     * This method is IPv6 compliant.
     * <p>
     * Don't make this method public please.
     *
     * @return <tt>true</tt> if the specified address is private,
     *  otherwise <tt>false</tt>
     */
    static boolean isPrivateAddress(InetAddress address) {
        if (address.isAnyLocalAddress() 
                || address.isLoopbackAddress() 
                || address.isLinkLocalAddress() 
                || address.isSiteLocalAddress()
                || isUniqueLocalUnicastAddress(address)
                || isBroadcastAddress(address)
                || isInvalidAddress(address)
                || isDocumentationAddress(address)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns true if both addresses belong to the same site local network.
     * This method is IPV6 safe.
     */
    public static boolean areInSameSiteLocalNetwork(InetAddress address1, InetAddress address2) {
        return areInSameSiteLocalNetwork(address1.getAddress(), address2.getAddress());
    }
    
    /**
     * Returns true if both addresses belong to the same site local network.
     * This method is IPV6 safe.
     */
    public static boolean areInSameSiteLocalNetwork(byte[] address1, byte[] address2) {
        if (address1.length != address2.length) {
            return false;
        }
        if (address1.length == 4) {
            if (address1[0] == 10) {
                return address2[0] == 10;
            } else if (address1[0] == (byte)172 && (address1[1] & 0xF0) == 16) {
                return address2[0] == (byte)172 && (address2[1] & 0xF0) == 16;
            } else if (address1[0] == (byte)192 && address1[1] == (byte)168) {
                return address2[0] == (byte)192 && address2[1] == (byte)168;
            } else {
                return false;
            }
        } else if (address1.length == 16) {
            try {
                InetAddress a1 = InetAddress.getByAddress(address1);
                InetAddress a2 = InetAddress.getByAddress(address2);
                // it looks like ipv6 only has one type of site local address, so
                // just check if both addresses are site local
                return a1.isSiteLocalAddress() && a2.isSiteLocalAddress();
            } catch (UnknownHostException e) {
                // impossible since addresses are of correct length
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("addresses of illegal length: " + address1.length);
    }
    
    /**
     * Checks if the given address is a private address.
     * <p>
     * This method is IPv6 compliant
     * 
     * @param address the address to check
     */
    public static boolean isPrivateAddress(byte[] address) {
        if (isAnyLocalAddress(address) 
                || isInvalidAddress(address)
                || isLoopbackAddress(address) 
                || isLinkLocalAddress(address) 
                || isSiteLocalAddress(address)
                || isUniqueLocalUnicastAddress(address)
                || isBroadcastAddress(address)
                || isDocumentationAddress(address)) {
            return true;
        }
        
        return false;
    }
    
    /** 
     * Returns the IP (given in BIG-endian) format as standard
     * dotted-decimal, e.g., 192.168.0.1<p> 
     *
     * @param ip the IP address in BIG-endian format
     * @return the IP address as a dotted-quad string
     */
     public static final String ip2string(byte[] ip) {
         return ip2string(ip, 0);
     }
         
    /** 
     * Returns the IP (given in BIG-endian) format of
     * buf[offset]...buf[offset+3] as standard dotted-decimal, e.g.,
     * 192.168.0.1<p> 
     *
     * @param ip the IP address to convert
     * @param offset the offset into the IP array to convert
     * @return the IP address as a dotted-quad string
     */
    public static final String ip2string(byte[] ip, int offset) {
        // xxx.xxx.xxx.xxx => 15 chars
        StringBuilder sbuf = new StringBuilder(16);   
        sbuf.append(ByteUtils.ubyte2int(ip[offset]));
        sbuf.append('.');
        sbuf.append(ByteUtils.ubyte2int(ip[offset+1]));
        sbuf.append('.');
        sbuf.append(ByteUtils.ubyte2int(ip[offset+2]));
        sbuf.append('.');
        sbuf.append(ByteUtils.ubyte2int(ip[offset+3]));
        return sbuf.toString();
    }
    
    /**
     * Determines if the given socket is from a local host.
     * <p>
     * This method is IPv6 compliant.
     */
    public static boolean isLocalHost(Socket socket) {
        return isLocalAddress(socket.getInetAddress());
    }
    
    /**
     * Packs a Collection of IpPorts into a byte array.
     */
    public static byte[] packIpPorts(Collection<? extends IpPort> ipPorts) {
        byte[] data = new byte[ipPorts.size() * 6];
        int offset = 0;
        for(IpPort next : ipPorts) {
            byte[] addr = next.getInetAddress().getAddress();
            int port = next.getPort();
            System.arraycopy(addr, 0, data, offset, 4);
            offset += 4;
            ByteUtils.short2leb((short)port, data, offset);
            offset += 2;
        }
        return data;
    }
    
    /**
     * Parses an ip:port byte-packed values.  
     * 
     * @return a collection of <tt>IpPort</tt> objects.
     * @throws InvalidDataException if an invalid IP is found or the size 
     * is not divisible by six
     */
    public static List<IpPort> unpackIps(byte [] data) throws InvalidDataException {
        return unpackIps(data, null);
    }
    
    
    /**
     * Parses an ip:port byte-packed values.
     * The decorator is consulted for each IpPort prior to inserted it into the list.  
     * 
     * @param data the packed IpPorts.
     * @param decorator A decorator that can optionally change the IpPort that is added into the returned list.
     * @return a collection of <tt>IpPort</tt> objects.
     * @throws InvalidDataException if an invalid IP is found or the size 
     * is not divisible by six
     */
    public static List<IpPort> unpackIps(byte [] data, Decorator<IpPort, ? extends IpPort> decorator) throws InvalidDataException {
        if (data.length % 6 != 0)
            throw new InvalidDataException("invalid size");

        int size = data.length/6;
        List<IpPort> ret = new ArrayList<IpPort>(size);
        byte [] current = new byte[6];
        for (int i=0;i<size;i++) {
            System.arraycopy(data,i*6,current,0,6);
            IpPort ipp = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
            if(decorator != null) {
                ipp = decorator.decorate(ipp);
                if(ipp == null)
                    throw new InvalidDataException("decorator returned null");
            }
            ret.add(ipp);
        }

        return Collections.unmodifiableList(ret);
    }
    
    /**
     * Filters unique IPs based on a Class C Netmask.
     */
    public static <T extends IpPort> Collection<T> filterOnePerClassC(Collection<T> c) {
        return filterUnique(c, CLASS_C_NETMASK);
    }
    
    /**
     * Filters unique IPs based on a netmask.
     */
    public static <T extends IpPort> Collection<T> filterUnique(Collection<T> c, int netmask) {
        ArrayList<T> ret = new ArrayList<T>(c.size());
        Set<Integer> ips = new HashSet<Integer>();
        for (T ip : c) {
            if (ips.add( getMaskedIP(ip.getInetAddress(), netmask) ))
                ret.add(ip);
            
        }
        ret.trimToSize();
        return ret;
    }
    
    /**
     * Returns the Class C Network part of the given InetAddress.
     * See {@link #getMaskedIP(InetAddress, int)} for more info.
     */
    public static int getClassC(InetAddress addr) {
        return getMaskedIP(addr, CLASS_C_NETMASK);
    }
    
    /**
     * Applies the netmask on the lower four bytes of the given 
     * InetAddress and returns it as an Integer.
     * <p>
     * This method is IPv6 compliant but shouldn't be called if
     * the InetAddress is neither IPv4 compatible nor mapped!
     */
    public static int getMaskedIP(InetAddress addr, int netmask) {
        byte[] address = addr.getAddress();
        return ByteUtils.beb2int(address, /* 0 */ address.length - 4) & netmask;
    }
    
    /**
     * Converts integer <code>ip</code> into byte array.
     * <p>
     * This method is not IPv6 compliant.
     */
    public static byte[] toByteAddress(int ip) {
        byte[] address = new byte[4];
        ByteUtils.int2beb(ip, address, 0);
        return address;
    }
    
    /**
     * @param decMask a netmask in decimal, like /24
     * @return an integer that can be and-ed for masking
     */
    public static int getHexMask(int decMask) {
        if (decMask < 0 || decMask > 32)
            throw new IllegalArgumentException("bad mask "+decMask);
        if (decMask == 0)
            return 0;
        return 0xFFFFFFFF << (32 - decMask);
    }
    
    /**
     * @return A non-loopback IPv4 address of a network interface on the local
     *         host.
     * @throws UnknownHostException
     */
    public static InetAddress getLocalAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getLocalHost();
        
        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
            return addr;
        }
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        addr = addresses.nextElement();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            return addr;
                        }
                    }
                }
            }
        } catch (SocketException se) {
        }

        throw new UnknownHostException(
                "localhost has no interface with a non-loopback IPv4 address");
    }
    
    /**
     * Returns the IP:Port as byte array.
     * <p>
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(SocketAddress addr, java.nio.ByteOrder order) throws UnknownHostException {
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        if (iaddr.isUnresolved()) {
            throw new UnknownHostException(iaddr.toString());
        }
        
        return getBytes(iaddr.getAddress(), iaddr.getPort(), order);
    }
    
    /**
     * Returns the IP:Port as byte array.
     * <p>
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(IpPort ipp, java.nio.ByteOrder order) {
        return getBytes(ipp.getInetAddress(), ipp.getPort(), order);
    }
    
    /**
     * Returns the IP:Port as byte array.
     * <p>
     * This method is IPv6 compliant
     */
    public static byte[] getBytes(InetAddress addr, int port, java.nio.ByteOrder order) {
        if (!isValidPort(port))
            throw new IllegalArgumentException("Port out of range: " + port);
        if(!isValidAddress(addr))
            throw new IllegalArgumentException("invalid addr: " + addr);
        
        byte[] address = addr.getAddress();

        byte[] dst = new byte[address.length + 2];
        System.arraycopy(address, 0, dst, 0, address.length);
        if(order == java.nio.ByteOrder.BIG_ENDIAN)
            ByteUtils.short2beb((short)port, dst, dst.length-2);
        else // if order == LITTLE_ENDIAN
            ByteUtils.short2leb((short)port, dst, dst.length-2);
        return dst;
    }
    
    /**
     * Creates an IpPort out of network data. The ByteOrder is used to determine
     * the order of the port. Throws <code>InvalidDataException</code> if the
     * data is invalid.
     * <p>
     * This method is IPv6 compliant.
     */
    public static IpPort getIpPort(byte[] ipport, java.nio.ByteOrder order)
      throws InvalidDataException {
        if(ipport.length < 6)
            throw new InvalidDataException("length must be >= 6, is: " + ipport.length);
        
        short shortport;
        if(order == java.nio.ByteOrder.BIG_ENDIAN)
            shortport = ByteUtils.beb2short(ipport, ipport.length - 2);
        else // if order == LITTLE_ENDIAN
            shortport = ByteUtils.leb2short(ipport, ipport.length - 2);
        int port = ByteUtils.ushort2int(shortport);
        
        if (!NetworkUtils.isValidPort(port))
            throw new InvalidDataException("Bad Port: " + port);
        
        // Optimized for IPv4 as far as memory goes..
        if(ipport.length == 6) {
            IP ip = new IP(ipport, 0);
            if(!NetworkUtils.isValidAddress(ip)) {
                throw new InvalidDataException("invalid addr: " + ip);
            }
            
            return new MemoryOptimizedIpPortImpl(ip, shortport);
        } else { // any other length, go w/ IPv6.
            InetAddress host;
            try {
                byte[] ip = new byte[ipport.length - 2];
                System.arraycopy(ipport, 0, ip, 0, ip.length);  
                host = InetAddress.getByAddress(ip);
            } catch(UnknownHostException uhe) {
                throw new InvalidDataException(uhe);
            }
            if(!NetworkUtils.isValidAddress(host)) {
                throw new InvalidDataException("invalid addr: " + host);
            }
            return new IpPortImpl(host, port);
        }
    }
    
    /**
     * Parses IP port in the same fashion as {@link #getIpPort(byte[], java.nio.ByteOrder)}.
     * <p>
     * Creates Connectable using <code>tlsCapable</code>. 
     */
    public static Connectable getConnectable(byte[] ipport, java.nio.ByteOrder order, boolean tlsCapable) throws InvalidDataException {
        return new ConnectableImpl(getIpPort(ipport, order), tlsCapable);
    }
    
    
    /**
     * Returns true if both SocketAddresses are either IPv4 or IPv6 addresses
     * <p>
     * This method is IPv6 compliant
     */
    public static boolean isSameAddressSpace(SocketAddress a, SocketAddress b) {
        return isSameAddressSpace(
                    ((InetSocketAddress)a).getAddress(), 
                    ((InetSocketAddress)b).getAddress());
    }
    
    /**
     * Returns true if both InetAddresses are compatible (IPv4 and IPv4
     * or IPv6 and IPv6).
     * <p>
     * This method is IPv6 compliant
     */
    public static boolean isSameAddressSpace(InetAddress a, InetAddress b) {
        if (a == null || b == null) {
            return false;
        }
        
        // Both are either IPv4 or IPv6
        if ((a instanceof Inet4Address && b instanceof Inet4Address)
                || (a instanceof Inet6Address && b instanceof Inet6Address)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns the IPv6 address bytes of IPv6 and IPv4 IP addresses. 
     * @throws IllegalArgumentException if given a different address type
     */
    public static byte[] getIPv6AddressBytes(InetAddress address) {
        byte[] bytes = address.getAddress();
        switch (bytes.length) {
            case 16:
                // Return the IPv6 address
                return bytes;
            case 4:
                // Turn the IPv4 address into a IPv4 mapped IPv6
                // address
                byte[] result = new byte[16];
                result[10] = (byte) 0xff;
                result[11] = (byte) 0xff;
                System.arraycopy(bytes, 0, result, 12, bytes.length);
                return result;
            default:
                throw new IllegalArgumentException("unhandled address length");
        }
    }
    
    /**
     * Returns true if an IPv6 representation of <code>address</code> exists.
     */
    public static boolean isIPv6Compatible(InetAddress address) {
        int length = address.getAddress().length;
        return length == 4 || length == 16;
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 address
     */
    private static boolean isIPv4Address(byte[] address) {
        return address.length == 4;
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 compatible address.
     * They're used when IPv6 systems need to communicate with each other, 
     * but are separated by an IPv4 network.
     */
    static boolean isIPv4CompatibleAddress(byte[] address) { 
        // Is it a IPv4 compatible IPv6 address?
        // (copied from Inet6Address)
        if (address.length == 16 
                && (address[ 0] == 0x00) && (address[ 1] == 0x00) 
                && (address[ 2] == 0x00) && (address[ 3] == 0x00) 
                && (address[ 4] == 0x00) && (address[ 5] == 0x00) 
                && (address[ 6] == 0x00) && (address[ 7] == 0x00) 
                && (address[ 8] == 0x00) && (address[ 9] == 0x00) 
                && (address[10] == 0x00) && (address[11] == 0x00))  {   
            return true;
        }
        
        return false;  
    }
    
    /**
     * Returns true if the given byte-array is an IPv4 mapped address.
     * IPv4 mapped addresses indicate systems that do not support IPv6. 
     * They are limited to IPv4. An IPv6 host can communicate with an 
     * IPv4 only host using the IPv4 mapped IPv6 address.
     */
    static boolean isIPv4MappedAddress(byte[] address) {
        if (address.length == 16 
                && (address[ 0] == 0x00) && (address[ 1] == 0x00) 
                && (address[ 2] == 0x00) && (address[ 3] == 0x00) 
                && (address[ 4] == 0x00) && (address[ 5] == 0x00) 
                && (address[ 6] == 0x00) && (address[ 7] == 0x00) 
                && (address[ 8] == 0x00) && (address[ 9] == 0x00) 
                && (address[10] == (byte)0xFF) && (address[11] == (byte)0xFF)) {   
            return true;
        }
        
        return false;  
    }
    
    /**
     * Returns true if it's an IPv4 InetAddress and the first octed is 0x00.
     */
    private static boolean isInvalidAddress(InetAddress address) {
        return isInvalidAddress(address.getAddress());
    }
    
    /**
     * Returns true if it's an IPv4 InetAddress and the first octet is 0x00.
     */
    private static boolean isInvalidAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return address[/* 0 */ address.length - 4] == 0x00;
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is an any local address.
     */
    static boolean isAnyLocalAddress(byte[] address) {
        if (address.length == 4 || address.length == 16) {
            byte test = 0;
            for (int i = 0; i < address.length; i++) {
                test |= address[i];
            }
            
            return (test == 0x00);
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a loopback address.
     */
    static boolean isLoopbackAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 127;
        } else if (address.length == 16) {
            byte test = 0x00;
            for (int i = 0; i < 15; i++) {
                test |= address[i];
            }
            return (test == 0x00) && (address[15] == 0x01);
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a link-local address.
     */
    static boolean isLinkLocalAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 169
                && (address[/* 1 */ address.length - 3] & 0xFF) == 254;
            
        // FE80::/64
        } else if (address.length == 16) {
            return (address[0] & 0xFF) == 0xFE
                && (address[1] & 0xC0) == 0x80;
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a site-local address.
     * IPv6 site-local addresses were deprecated in September 2004 
     * by RFC 3879 and replaced by RFC 4193 (Unique Local IPv6 Unicast
     * Addresses).
     */
    static boolean isSiteLocalAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return  (address[/* 0 */ address.length - 4] & 0xFF) == 10
                || ((address[/* 0 */ address.length - 4] & 0xFF) == 172
                &&  (address[/* 1 */ address.length - 3] & 0xF0) == 16)
                || ((address[/* 0 */ address.length - 4] & 0xFF) == 192
                &&  (address[/* 1 */ address.length - 3] & 0xFF) == 168);
            
        // Has been deprecated in September 2004 by RFC 3879 
        // FEC0::/10
        } else if (address.length == 16) {
            return (address[0] & 0xFF) == 0xFE
                && (address[1] & 0xC0) == 0xC0;
        }
        return false;
    }
    
    /**
     * Returns true if the given InetAddress is an Unique Local IPv6
     * Unicast Address. See RFC 4193 for more info.
     */
    public static boolean isUniqueLocalUnicastAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isUniqueLocalUnicastAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is an Unique Local IPv6
     * Unicast Address. See RFC 4193 for more info.
     */
    private static boolean isUniqueLocalUnicastAddress(byte[] address) {
        // FC00::/7
        if (address.length == 16) {
            return (address[0] & 0xFE) == 0xFC;
        }
        return false;
    }

    /**
     * Returns true if the given InetAddress is a broadcast address.
     */
    public static boolean isBroadcastAddress(InetAddress address) {
        return isBroadcastAddress(address.getAddress());
    }
    
    /**
     * Returns true if the given byte-array is a broadcast address
     * <p>
     * This method is IPv6 compliant but returns always false if
     * the given address is neither a true IPv4, nor an IPv4-mapped
     * address.
     */
    private static boolean isBroadcastAddress(byte[] address) {
        if (isIPv4Address(address) || isIPv4MappedAddress(address)) {
            return (address[/* 0 */ address.length - 4] & 0xFF) == 0xFF;
        }
        
        return false;
    }
    
    /**
     * Returns true if the given InetAddress is a private IPv4-compatible
     * address.
     * <p>
     * It checks for a somewhat tricky and undefined case. An address such
     * as ::0000:192.168.0.1 is an IPv6 address, it's an IPv4-compatible
     * address but it's by IPv6 definition not a site-local (private) address.
     * On the other hand it's a private IPv4 address.
     */
    public static boolean isPrivateIPv4CompatibleAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isPrivateIPv4CompatibleAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array is a private IPv4-compatible
     * address.
     */
    private static boolean isPrivateIPv4CompatibleAddress(byte[] address) {
        if (isIPv4CompatibleAddress(address)) {
            // Copy the lower four bytes and perform the
            // checks on it to determinate whether or not
            // it's a private IPv4 address
            byte[] ipv4 = new byte[4];
            System.arraycopy(address, 12, ipv4, 0, ipv4.length);
            return isPrivateAddress(ipv4);
        }
        return false;
    }
    
    /**
     * Returns true if the given InetAddress has a prefix that's used in
     * Documentation. It's a non-routeable IPv6 address. See <a href=
     * "http://www.ietf.org/rfc/rfc3849.txt">RFC 3849</a>  
     * for more information.
     */
    public static boolean isDocumentationAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            return isDocumentationAddress(address.getAddress());
        }
        return false;
    }
    
    /**
     * Returns true if the given byte-array has a prefix that's used in
     * Documentation. It's a non-routeable IPv6 address. See <a href=
     * "http://www.ietf.org/rfc/rfc3849.txt">RFC 3849</a> 
     * for more information.
     */
    private static boolean isDocumentationAddress(byte[] address) {
        // 2001:0DB8::/32
        if (address.length == 16) {
            return (address[0] & 0xFF) == 0x20
                && (address[1] & 0xFF) == 0x01
                && (address[2] & 0xFF) == 0x0D
                && (address[3] & 0xFF) == 0xB8;
        }
        return false;
    }

    /**
     * Parses port from string and checks if it's valid.
     * 
     * @throws IOException if the port could not be parsed or is invalid
     */
    static int parsePort(String portString) throws IOException {
        try {
            int port = Integer.parseInt(portString);
            if(!isValidPort(port)) {
                throw new IOException("invalid port: " + port);
            }
            return port;
        } catch(NumberFormatException invalid) {
            throw (IOException)new IOException().initCause(invalid);
        }
    }
    
    /**
     * Gets {@link InetAddress} and checks if it's valid.
     * @throws IOException if the address is not valid address
     */
    static InetAddress getAndCheckAddress(String addressString) throws IOException {
        InetAddress address = InetAddress.getByName(addressString);
        if (!isValidAddress(address))
            throw new IOException("invalid addr: " + address);
        
        return address;
    }
    
    /**
     * Returns the index of the ':' separator between ip and port and checks
     * if it's at a valid position.
     * @throws IOException if separator is not found or not at a valid position
     */
    static int getAndCheckIpPortSeparator(String ipPort) throws IOException {
        int separator = ipPort.indexOf(":");
        
        //see if this is a valid ip:port address; 
        if (separator <= 0 || separator!= ipPort.lastIndexOf(":") || separator == ipPort.length() - 1)
            throw new IOException("invalid separator in http: " + ipPort);
        
        return separator;
    }
    
    /**
     * Returns a Connectable of the ipPort as described by "a.b.c.d:port".
     * 
     * @param ipPort a string representing an IP and port 
     * @throws IOException parsing failed.
     */
    public static Connectable parseIpPort(String ipPort, boolean tlsCapable) throws IOException {
        int separator = getAndCheckIpPortSeparator(ipPort);
        InetAddress address = getAndCheckAddress(ipPort.substring(0, separator));
        int port = parsePort(ipPort.substring(separator+1));
        return new ConnectableImpl(new InetSocketAddress(address, port), tlsCapable);
    }
    
    
    /**
     * @param http a string representing a port and an ip
     * @return an object implementing IpPort 
     * @throws IOException parsing failed.
     */
    public static IpPort parsePortIp(String http) throws IOException{
        int separator = getAndCheckIpPortSeparator(http);
        int port = parsePort(http.substring(0, separator));
        InetAddress address = getAndCheckAddress(http.substring(separator+1));
        return new IpPortImpl(address, port);
    }
}
