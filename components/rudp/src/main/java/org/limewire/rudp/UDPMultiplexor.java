package org.limewire.rudp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.SynMessage;


/** 
 *  Manages the assignment of connection IDs and the routing of 
 *  {@link RUDPMessage RUDPMessages}. 
 */
public class UDPMultiplexor extends AbstractSelector {

    private static final Log LOG = LogFactory.getLog(UDPMultiplexor.class);

    /** The 0 slot is for incoming new connections so it is not assigned. */
    public static final byte UNASSIGNED_SLOT   = 0;

    /** Keep track of the assigned connections. */
    private volatile UDPSocketChannel[] _channels;
    
    /** A list of overflowed channels when registering. */
    private final List<SelectableChannel> channelsToRemove = new LinkedList<SelectableChannel>();
    
    /** A set of the currently connected keys. */
    private Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>(256);

    /** Keep track of the last assigned connection id so that we can use a 
        circular assignment algorithm.  This should cut down on message
        collisions after the connection is shut down. */
    private int _lastConnectionID;
    
    /** The RUDPContext that contains the TransportListener. */
    private final RUDPContext context;

    /**
     *  Initialize the UDPMultiplexor.
     */
    UDPMultiplexor(SelectorProvider provider, RUDPContext context) {
        super(provider);
        this.context = context;
        _channels       = new UDPSocketChannel[256];
        _lastConnectionID  = 0;
    }
    
    /**
     * Determines if we're connected to the given host.
     */
    public boolean isConnectedTo(InetAddress host) {
        UDPSocketChannel[] array = _channels;

        if (_lastConnectionID == 0)
            return false;
        for (int i = 0; i < array.length; i++) {
            UDPSocketChannel channel = array[i];
            if (channel != null && host.equals(channel.getRemoteSocketAddress().getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Route a message to the {@link UDPConnectionProcessor} identified via the message's
     *  connection ID.
     *  Notifies the provided listener (if any) if the channel is ready to produce events.
     */
    public void routeMessage(RUDPMessage msg, InetSocketAddress addr) {
        UDPSocketChannel[] array = _channels;
        int connID = msg.getConnectionID() & 0xff;
        UDPSocketChannel channel = null;
        // If connID equals 0 and SynMessage then associate with a connection
        // that appears to want it (connecting and with knowledge of it).
        if ( connID == 0 && msg instanceof SynMessage ) {
            LOG.debugf("route sym: {0}", msg);
            for (int i = 1; i < array.length; i++) {
                channel = array[i];
                if(channel == null)
                    continue;
                
                LOG.debugf("non-empty index: {0}, addr: {1}", i, channel.getRemoteSocketAddress());
                
                if ( channel.isConnectionPending() && channel.isForMe(addr, (SynMessage)msg)) {
                    LOG.debugf("found index: {0}, sender id: {1}", i, ((SynMessage)msg).getSenderConnectionID());
                    channel.getProcessor().handleMessage(msg);
                    break;
                } 
            }
            // Note: eventually these messages should find a match
            // so it is safe to throw away premature ones

        } else if(array[connID] != null) {  // If valid connID then send on to connection
            channel = array[connID];
            if (msg instanceof SynMessage) {
                LOG.debugf("already assigned syn: {0}", msg);
            }
            if (channel.getRemoteSocketAddress().equals(addr) )
                channel.getProcessor().handleMessage(msg);
        } else {
            LOG.debugf("message for non-existing connection: {0}", msg);
        }
        
        if (channel != null && channel.getProcessor().readyOps() != 0)
            context.getTransportListener().eventPending();
    }

    @Override
    protected void implCloseSelector() throws IOException {
        throw new IllegalStateException("should never be closed.");
    }

    /**
     * Registers a new channel with this Selector.
     * If we've already stored over the limit of channels, this will store
     * the channel in a temporary list to be cancelled on the next selection.
     */
    @Override
    protected synchronized SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        int connID;
        
        if(!(ch instanceof UDPSocketChannel))
            throw new IllegalSelectorException();
        
        UDPSocketChannel channel = (UDPSocketChannel)ch;

        UDPSocketChannel[] copy = new UDPSocketChannel[_channels.length];
        for (int i = 0; i < _channels.length; i++)
            copy[i] = _channels[i];

        for (int i = 1; i <= copy.length; i++) {
            connID = (_lastConnectionID + i) % 256;

            // We don't assign zero.
            if (connID == 0)
                continue;

            // If the slot is open, take it.
            if (copy[connID] == null) {
                _lastConnectionID = connID;
                copy[connID] = channel;
                channel.getProcessor().setConnectionId((byte)connID);
                _channels = copy;
                return new UDPSelectionKey(this, att, ch, ops);
            }
        }
        
        // We don't have enough space for this connection.  Add it to a temporary
        // list of bad connections which will be removed during selectNow.
        LOG.warn("Attempting to add over connection limit");
        channelsToRemove.add(ch);
        return new UDPSelectionKey(this, att, ch, ops);
    }

    /**
     * Returns all {@link SelectionKey SelectionKeys} this Selector is currently in control of.
     */
    @Override
    public Set<SelectionKey> keys() {
        UDPSocketChannel[] channels = _channels;
        Set<SelectionKey> keys = new HashSet<SelectionKey>();
        for(int i = 0; i < channels.length; i++) {
            if(channels[i] != null)
                keys.add(channels[i].keyFor(this));
        }
        synchronized(this) {
            for(SelectableChannel channel : channelsToRemove)
                keys.add(channel.keyFor(this));
        }
        return keys;
    }

    @Override
    public int select() throws IOException {
        throw new UnsupportedOperationException("blocking select not supported");
    }

    @Override
    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException("blocking select not supported");
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return selectedKeys;
    }

    /** Polls through all available channels and returns those that are ready. */
    @Override
    public int selectNow() throws IOException {
        UDPSocketChannel[] array = _channels;
        UDPSocketChannel[] removed = null;

        selectedKeys.clear();
        
        for (int i = 0; i < array.length; i++) {
            UDPSocketChannel channel = array[i];
            if (channel == null)
                continue;

            UDPSelectionKey key = (UDPSelectionKey)channel.keyFor(this);
            if (key != null) {
                if (key.isValid() && channel.isOpen()) {
                    int currentOps = channel.getProcessor().readyOps();
                    int readyOps = currentOps & key.interestOps();
                    if (readyOps != 0) {
                        key.setReadyOps(readyOps);
                        selectedKeys.add(key);
                    }
                } else {
                    if (removed == null)
                        removed = new UDPSocketChannel[array.length];
                    removed[i] = channel;
                }
            }
        }

        // Go through the removed list & remove them from _connections.
        // _connections may have changed (since we didn't lock while polling),
        // so we need to check and ensure the given UDPConnectionProcessor
        // is the same.
        synchronized (this) {
            if (removed != null) {
                UDPSocketChannel[] copy = new UDPSocketChannel[_channels.length];
                for (int i = 0; i < _channels.length; i++) {
                    if (_channels[i] == removed[i])
                        copy[i] = null;
                    else
                        copy[i] = _channels[i];
                }
                _channels = copy;
            }
            
            if(!channelsToRemove.isEmpty()) {
                for(SelectableChannel next : channelsToRemove) {
                    UDPSelectionKey key = (UDPSelectionKey)next.keyFor(this);
                    key.cancel();
                    key.setReadyOps(0);
                    selectedKeys.add(key);
                }
                channelsToRemove.clear();
            }
        }
        
        return selectedKeys.size();
    }

    @Override
    public Selector wakeup() {
        // Does nothing, since this never blocks.
        return this;
    }
}
