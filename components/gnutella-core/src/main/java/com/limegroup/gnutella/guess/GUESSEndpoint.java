package com.limegroup.gnutella.guess;

import java.net.InetAddress;

import org.limewire.io.IpPortImpl;

/** An IpPort intended for use with GUESS code that requires hashtables. */
public final class GUESSEndpoint extends IpPortImpl {

    /**
     * Constructs a new <tt>GUESSEndpoint</tt> with the specified IP and port.
     * 
     * @param address the ip address of the host
     * @param port the port the host is listening on
     */
    public GUESSEndpoint(InetAddress address, int port) {
        super(address, port);
    }

    /**
     * Returns true if two GUESSEndpoint objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        boolean retBool = false;
        if (o instanceof GUESSEndpoint) {
            GUESSEndpoint ep = (GUESSEndpoint) o;
            retBool = (getAddress().equals(ep.getAddress())) && (getPort() == ep.getPort());
        }
        return retBool;
    }

    /**
     * Returns this' hashCode.
     */
    @Override
    public int hashCode() {
        int result = 79;
        result = 37 * result + getAddress().hashCode();
        result = 37 * result + getPort();
        return result;
    }

    @Override
    public String toString() {
        return "GUESSEndpoint: " + getInetAddress() + ":" + getPort();
    }

}
