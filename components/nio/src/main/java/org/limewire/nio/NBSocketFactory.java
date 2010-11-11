package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** 
 * Creates sockets typed specifically for {@link NBSocket NBSockets}. 
 */
public abstract class NBSocketFactory extends ExtendedSocketFactory {

    @Override
    public abstract NBSocket createSocket() throws IOException;

    @Override
    public abstract NBSocket createSocket(String host, int port) throws IOException, UnknownHostException;

    @Override
    public abstract NBSocket createSocket(InetAddress host, int port) throws IOException;

    @Override
    public abstract NBSocket createSocket(String host, int port,
                                          InetAddress localHost, int localPort)
       throws IOException, UnknownHostException;

    @Override
    public abstract NBSocket createSocket(InetAddress address, int port,
                                          InetAddress localAddress, int localPort)
      throws IOException;

}
