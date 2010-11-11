package org.limewire.io;

import java.net.UnknownHostException;

import org.limewire.util.Objects;

public class UnresolvedIpPortImpl implements UnresolvedIpPort {
    
    private final String host;
    private final int port;

    public UnresolvedIpPortImpl(String host, int port) {
        this.host = Objects.nonNull(host, "host");
        this.port = port;
    }

    /**
     * Expects an address port pair in the form "address:port". 
     */
    public UnresolvedIpPortImpl(String addressAndPort) throws InvalidDataException {
        String[] parts = addressAndPort.split(":");
        if(parts.length != 2) {
            throw new InvalidDataException("Input is expected as an address port pair in the form, address:port");
        }
        this.host = parts[0];
        try {
            this.port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidDataException("Port is expected to be an integer.", e);
        }
    }

    @Override
    public IpPort resolve() throws UnknownHostException {
        return new IpPortImpl(host, port);
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getAddress() {
        return host;
    }
}
