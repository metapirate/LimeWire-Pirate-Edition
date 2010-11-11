package org.limewire.rudp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.rudp.messages.SynMessage.Role;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Service-provider class for {@link UDPMultiplexor} selectors and 
 * {@link UDPSocketChannel} selectable channels.
 */
@Singleton
public class UDPSelectorProvider extends SelectorProvider {    
    private final RUDPContext context;
    private final AsynchronousEventBroadcaster<UDPSocketChannelConnectionEvent> connectionStateEventBroadcaster;

    @Inject
    public UDPSelectorProvider(RUDPContext context,
                               AsynchronousEventBroadcaster<UDPSocketChannelConnectionEvent> connectionStateEventBroadcaster) {
		this.context = context;
        this.connectionStateEventBroadcaster = connectionStateEventBroadcaster;
    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        throw new IOException("not supported");
    }

    @Override
    public Pipe openPipe() throws IOException {
        throw new IOException("not supported");
    }

    @Override
    public UDPMultiplexor openSelector() {
        UDPMultiplexor plexor = new UDPMultiplexor(this, context);
        return plexor;
    }

    /**
     * Opens an acceptor socket channel after a request has been received
     * that another party is trying to connect to this instance. 
     */
    public AbstractNBSocketChannel openAcceptorSocketChannel() {
        return new UDPSocketChannel(this, context, Role.ACCEPTOR, connectionStateEventBroadcaster);
    }

    @Override
    public AbstractNBSocketChannel openSocketChannel() {
        return new UDPSocketChannel(this, context, Role.REQUESTOR, connectionStateEventBroadcaster);
    }
    
    public Class<UDPSocketChannel> getUDPSocketChannelClass() {
        return UDPSocketChannel.class;
    }
    
    public RUDPContext getContext() {
        return context;
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
}
