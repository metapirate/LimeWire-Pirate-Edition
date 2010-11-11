package org.limewire.io;



import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Provides a default {@link IpPort} implementation to return IP information
 * ({@link InetAddress}, host name, and port number). <code>IpPortImpl</code>
 * uses constructor arguments (either directly or via parsing) to set IP 
 * information.
 */
public class IpPortImpl implements IpPort {
    
    private final InetSocketAddress addr;
    private final String addrString;
    
    /** Constructs a new IpPort based on the given SocketAddress. */
    public IpPortImpl(InetSocketAddress addr) {
        this(addr, addr.getAddress().getHostAddress());
    }
    
    /** Constructs a new IpPort with the given SocketAddress, explicitly defining the string-addr. */
    public IpPortImpl(InetSocketAddress addr, String addrString) {
        this.addr = addr;
        this.addrString = addrString;
    }
    
    /** Constructs a new IpPort using the addr & port. */
    public IpPortImpl(InetAddress addr, int port) {
        this(new InetSocketAddress(addr, port));
    }
    
    /** Constructs a new IpPort using the given host & port.*/
    public IpPortImpl(String host, int port) throws UnknownHostException {
        this(new InetSocketAddress(InetAddress.getByName(host), port), host);
    }
    
    /** Constructs a new IpPort using the given byte[] & port. */
    public IpPortImpl(byte[] addr, int port) {
        this(new InetSocketAddress(getAddressFromBytes(addr), port), asString(addr));
    }
    
    /** Constructs an IpPort using the given host:port */
    public IpPortImpl(String hostport) throws UnknownHostException {
        int colonIdx = hostport.indexOf(":");
        if(colonIdx == hostport.length() - 1)
            throw new UnknownHostException("invalid hostport: " + hostport);
        
        String host = hostport;
        int port = 80;
        if(colonIdx != -1) {
            host = hostport.substring(0, colonIdx);
            try {
                port = Integer.parseInt(hostport.substring(colonIdx+1).trim());
            } catch(NumberFormatException nfe) {
                throw new UnknownHostException("invalid hostport: " + hostport);
            }
        }
        
        this.addr = new InetSocketAddress(InetAddress.getByName(host), port);
        this.addrString = host;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return addr;
    }
    
    public InetAddress getInetAddress() {
        return addr.getAddress();
    }
    
    public String getAddress() {
        return addrString;
    }
    
    public int getPort() {
        return addr.getPort();
    }
    
    @Override
    public String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
    
    private static String asString(byte[] addr) {
     // xxx.xxx.xxx.xxx => 15 chars
        StringBuilder sb = new StringBuilder(19);
        for(int i = 0; i < addr.length; i++) {
            sb.append(addr[i] & 0xFF);
            if(i != addr.length - 1)
                sb.append(".");
        }
        return sb.toString();
    }
    
    private static InetAddress getAddressFromBytes(byte[] address) {
        try {
            return InetAddress.getByAddress(address);
        } catch(UnknownHostException uhe) {
            throw new IllegalArgumentException("invalid address: " + asString(address));
        }
    }
}