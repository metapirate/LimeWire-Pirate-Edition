package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.limewire.util.StringUtils;

/** A default implementation of Connectable. */
public class ConnectableImpl implements Connectable {

    public static final Connectable INVALID_CONNECTABLE;
    
    static {
        try {
            INVALID_CONNECTABLE = new ConnectableImpl("0.0.0.0", 1, false);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final IpPort ipPort;
    private final boolean tlsCapable;
    
    private int hashCode = -1;
    
    /** Constructs a Connectable that delegates to the IpPort and may be tls capable. */
    public ConnectableImpl(IpPort ipPort, boolean tlsCapable) {
        this.ipPort = ipPort;
        this.tlsCapable = tlsCapable;
    }
    
    /** Constructs a Connectable based on the given InetSocketAddress. */
    public ConnectableImpl(InetSocketAddress addr, boolean tlsCapable) {
        this(new IpPortImpl(addr), tlsCapable);
    }
    
    /** Constructs a Connectable based on the given host data. */
    public ConnectableImpl(InetAddress host, int port, boolean tlsCapable) {
        this(new IpPortImpl(host, port), tlsCapable);
    }    
    
    /** Constructs a Connectable based on the given host data. */
    public ConnectableImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
        this(new IpPortImpl(host, port), tlsCapable);
    }
    
    public ConnectableImpl(String hostPort, boolean tlsCapable) throws UnknownHostException {
        this(new IpPortImpl(hostPort), tlsCapable);
    }
    
    /** Copy-constructor for Connectables. */
    public ConnectableImpl(Connectable connectable) {
        this(new IpPortImpl(connectable.getInetSocketAddress(), connectable.getAddress()),
             connectable.isTLSCapable());
    }

    public boolean isTLSCapable() {
        return tlsCapable; 
    }

    public String getAddress() {
        return ipPort.getAddress();
    }

    public InetAddress getInetAddress() {
        return ipPort.getInetAddress();
    }

    public int getPort() {
        return ipPort.getPort();
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return ipPort.getInetSocketAddress();
    }
    
    @Override
    public String getAddressDescription() {
        return getAddress();
    }
    
    @Override
    public String toString() {
        return StringUtils.toStringBlacklist(this, hashCode, INVALID_CONNECTABLE);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Connectable) {
            Connectable connectable = (Connectable)obj;
            return getInetAddress().equals(connectable.getInetAddress())
            && getPort() == connectable.getPort() 
            && isTLSCapable() == connectable.isTLSCapable();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hash = hashCode;
        if (hash == -1) {
            hash = getInetAddress().hashCode();
            hash = hash * 31 + getPort();
            hash = hash * 31 + (tlsCapable ? 1 : 0);
            hashCode = hash;
        }
        return hashCode;
    }

}
