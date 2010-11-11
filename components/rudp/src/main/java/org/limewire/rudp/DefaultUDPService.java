package org.limewire.rudp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.ByteBufferOutputStream;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.ReadWriteObserver;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.MessageFormatException;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;
import org.limewire.service.ErrorService;

/**
 * <p>A default implementation of <code>UDPService</code>.
 * This implementation is basic and will not work in the real world.
 * You must provide an implementation that implements these methods correctly.
 * </p>
 * Messages are created using the <code>DefaultMessageFactory</code> 
 * and handed off to the <code>UDPMultiplexor</code>.
 */
public class DefaultUDPService implements UDPService, ReadWriteObserver {
    
    private static final Log LOG = LogFactory.getLog(DefaultUDPService.class);
    
    /** The MessageFactory this is using. */
    private RUDPMessageFactory factory = new DefaultMessageFactory();
    /** The DatagramChannel we're reading from & writing to. */
    private DatagramChannel channel;
    /** The list of messages to be sent, as SendBundles. */
    private final List<SendBundle> OUTGOING_MSGS;
    /** The buffer that's re-used for reading incoming messages. */
    private final ByteBuffer BUFFER;
    /** The maximum size of a UDP message we'll accept. */
    private final int BUFFER_SIZE = 1024 * 2;
    /** The dispatcher this dispatches msgs to. */
    private final MessageDispatcher DISPATCHER;

    /** Constructs a new DefaultUDPService. */
    public DefaultUDPService(MessageDispatcher dispatcher) {
        OUTGOING_MSGS = new LinkedList<SendBundle>();
        byte[] backing = new byte[BUFFER_SIZE];
        BUFFER = ByteBuffer.wrap(backing);
        DISPATCHER = dispatcher;
    }
    
    /**
     * Returns the port that the service is listening on.
     * This WILL NOT CORRECTLY FIGURE OUT THE EXTERNAL PORT.
     */
    public int getStableListeningPort() {
        if(channel != null)
            return channel.socket().getLocalPort();
        else
            return 0;
    }
    
    /**
     * Returns the address the service is listening on.
     * This WILL NOT CORRECTLY FIGURE OUT THE EXTERNAL ADDRESS.
     */
    public InetAddress getStableListeningAddress() {
        if(channel != null)
            return channel.socket().getLocalAddress();
        else {
            try {
                return InetAddress.getLocalHost();
            } catch(UnknownHostException bad) {
                return null;
            }
        }
    }

    public boolean isListening() {
        return channel != null;
    }

    /** Always returns true. */
    public boolean isNATTraversalCapable() {
        return true;
    }
    
    /** Starts the UDPService. */
    public void start(int port) throws IOException {
        channel = getChannel(port);
        NIODispatcher.instance().registerReadWrite(channel, this);
    }
    
    /** Shuts down the UDPService. */
    public void shutdown() {
        if(channel != null) {
            try {
                channel.close();
            } catch(IOException ignored) {}
        }
    }
    
    /**  Returns a new DatagramChannel that is bound to the given port. */
    private DatagramChannel getChannel(int port) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        DatagramSocket s = channel.socket();
        s.setReceiveBufferSize(64*1024);
        s.setSendBufferSize(64*1024);
        s.setReuseAddress(true);
        s.bind(new InetSocketAddress(port));
        return channel;
    }
    
    /** Notification that an IOException occurred while reading/writing. */
    public void handleIOException(IOException iox) {
        if( !(iox instanceof java.nio.channels.ClosedChannelException ) )
            ErrorService.error(iox, "UDP Error.");
        else
            LOG.trace("Swallowing a UDPService ClosedChannelException", iox);
    }
    
    /** Notification that a read can happen. */
    public void handleRead() throws IOException {
        while (true) {
            BUFFER.clear();

            SocketAddress from;
            try {
                from = channel.receive(BUFFER);
            } catch (IOException iox) {
                break;
            }

            // no packet.
            if (from == null)
                break;

            if (!(from instanceof InetSocketAddress)) {
                ErrorService.error(new IllegalStateException("non inet address"), "from: " + from);
                continue;
            }

            InetSocketAddress addr = (InetSocketAddress) from;
            if (!NetworkUtils.isValidAddress(addr.getAddress()))
                continue;
            if (!NetworkUtils.isValidPort(addr.getPort()))
                continue;
            
            // Clone the buffer while creating, so the next message can be read using it.
            BUFFER.flip();
            ByteBuffer clone = ByteBuffer.allocate(BUFFER.remaining());
            clone.put(BUFFER);
            clone.flip();
            
            RUDPMessage message = null;
            try {
                message = factory.createMessage(clone);
            } catch(MessageFormatException ignored) {}
            
            if(message != null)
                processMessage(message, addr);
        }
    }
    
    /** Processes a single message. */
    protected void processMessage(RUDPMessage message, InetSocketAddress addr) {
        DISPATCHER.dispatch(message, addr);
    }
    
    /**
     * Sends the specified <tt>RUDPMessage</tt> to the specified host.
     * 
     * @param msg the <tt>RUDPMessage</tt> to send
     * @param host the host to send the message to
     */
    public void send(RUDPMessage msg, SocketAddress host) {
        if (msg == null)
            throw new IllegalArgumentException("Null Message");
        if (host == null)
            throw new IllegalArgumentException("Null InetAddress");
        if (!NetworkUtils.isValidSocketAddress(host))
            throw new IllegalArgumentException("Invalid Port: " + host);
        if(channel == null || channel.socket().isClosed())
            return; // ignore if not open.


        ByteBufferOutputStream baos = new ByteBufferOutputStream();
        try {
            msg.write(baos);
        } catch(IOException impossible) {
            ErrorService.error(impossible);
            return;
        }
        
        ByteBuffer buffer = baos.getBuffer();
        buffer.flip();
        
        synchronized(OUTGOING_MSGS) {
            OUTGOING_MSGS.add(new SendBundle(buffer, host));
            if(channel != null)
                NIODispatcher.instance().interestWrite(channel, true);
        }
    }   
    
    /** Notification that a write can happen. */
    public boolean handleWrite() throws IOException {
        synchronized(OUTGOING_MSGS) {
            while(!OUTGOING_MSGS.isEmpty()) {
                SendBundle bundle = OUTGOING_MSGS.remove(0);
                try {
                    if(channel.send(bundle.buffer, bundle.addr) == 0) {
                        // we removed the bundle from the list but couldn't send it,
                        // so we have to put it back in.
                        OUTGOING_MSGS.add(0, bundle);
                        return true; // no room left to send.
                    }
                } catch(IOException ignored) {
                    LOG.warn("Ignoring exception on socket", ignored);
                }
            }
            
            // if there's no data left to send, we don't wanna be notified of write events.
            NIODispatcher.instance().interestWrite(channel, false);
            return false;
        }
    }
    
    /** Wrapper for outgoing data */
    private static class SendBundle {
        private final ByteBuffer buffer;
        private final SocketAddress addr;
        
        SendBundle(ByteBuffer b, SocketAddress addr) {
            buffer = b;
            this.addr = addr;
        }
    }
    
}
