package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;

import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.ConnectObserver;

/**
 * A {@link Socket} that does all of its connecting/reading/writing using NIO.
 * <p>
 * Input/OutputStreams are provided for blocking I/O (although 
 * internally non-blocking I/O is used). To use event-based reads,
 * use {@link #setReadObserver(org.limewire.nio.channel.ChannelReadObserver)} 
 * and read-events are passed to the {@link ReadObserver}. 
 * <p>
 * A {@link ChannelReadObserver} must be used so the <code>Socket</code> can
 * set the appropriate underlying channel.
 */
public class NIOSocket extends AbstractNBSocket {

    /** The underlying channel the socket is using. */
    private final SocketChannel channel;

    /** The Socket that this delegates to. */
    private final Socket socket;

    /** The remote socket address. */
    private volatile SocketAddress remoteSocketAddress;

    /**
     * Constructs an <code>NIOSocket</code> using a pre-existing <code>Socket</code>.
     * To be used by <code>NIOServerSocket</code> while accepting incoming connections.
     */
    protected NIOSocket(Socket s) {
        channel = s.getChannel();
        socket = channel.socket();
        remoteSocketAddress = s.getRemoteSocketAddress();
        initIncomingSocket();
        setInitialReader();
        setInitialWriter();
        NIODispatcher.instance().register(channel, this);
    }

    /** Creates an unconnected <code>NIOSocket</code>. */
    public NIOSocket() throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        initOutgoingSocket();
        setInitialReader();
        setInitialWriter();
    }

    /** Creates an <code>NIOSocket</code> and connects (with no timeout) to addr/port. */
    public NIOSocket(InetAddress addr, int port) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        initOutgoingSocket();
        setInitialReader();
        setInitialWriter();
        connect(new InetSocketAddress(addr, port));
    }

    /** Creates an <code>NIOSocket</code> locally bound to localAddr/localPort 
     * and connects (with no timeout) to addr/port.
     */
    public NIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        initOutgoingSocket();
        setInitialReader();
        setInitialWriter();
        bind(new InetSocketAddress(localAddr, localPort));
        connect(new InetSocketAddress(addr, port));
    }

    /** Creates an <code>NIOSocket</code> and connects (with no timeout) to addr/port. */
    public NIOSocket(String addr, int port) throws UnknownHostException, IOException {
        this(InetAddress.getByName(addr), port);
    }

    /** Creates an <code>NIOSocket</code> locally bound to localAddr/localPort 
     * and connects (with no timeout) to addr/port. 
     */
    public NIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        this(InetAddress.getByName(addr), port, localAddr, localPort);
    }
    
    /** Performs initialization for an incoming Socket. Doesn't do anything right now. */
    protected void initIncomingSocket() {
        
    }

    /** Performs initialization for this <code>NIOSocket</code>. Currently just 
     * makes the channel non-blocking. */
    protected void initOutgoingSocket() throws IOException {
        channel.configureBlocking(false);
    }

    /** Binds the socket to the <code>SocketAddress</code>. */
    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
    }

    /** Stores the connecting address so we can retrieve it later. */
    @Override
    public boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) {
        remoteSocketAddress = addr;
        return super.connect(addr, timeout, observer);
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }
    
    @Override
    public InetAddress getInetAddress() {
        //The separate variable for storage is necessary because Sockets created
        //with SocketChannel.open() return null when there's no connection.
        if(remoteSocketAddress != null)
            return ((InetSocketAddress)remoteSocketAddress).getAddress();
        else
            return null;
    }
    
    @Override
    public int getPort() {
        //The separate variable for storage is necessary because Sockets created
        //with SocketChannel.open() return null when there's no connection.
        if(remoteSocketAddress != null)
            return ((InetSocketAddress)remoteSocketAddress).getPort();
        else
            return 0;
    }

    /** Constructs an InterestReadChannel adapter around the SocketChannel. */
    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        return new SocketInterestReadAdapter(channel);
    }

    /** Constructs an InterestWriteChannel adapter around the SocketChannel. */
    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        return new SocketInterestWriteAdapter(channel);
    }

    /** Shuts down input, output and the socket. */
    @Override
    protected void shutdownImpl() {
        try {
            shutdownInput();
        } catch (IOException ignored) {
        }

        try {
            shutdownOutput();
        } catch (IOException ignored) {
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        } catch (Error ignored) {
        }
    }

    // /////////////////////////////////////////////
    // / BELOW ARE ALL WRAPPERS FOR SOCKET.
    // /////////////////////////////////////////////
    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        try {
            return socket.getLocalAddress();
        } catch (Error osxSucks) {
            // On OSX 10.3 w/ Java 1.4.2_05, if the connection dies
            // prior to this method being called, an Error is thrown.
            try {
                return InetAddress.getLocalHost();
            } catch (UnknownHostException uhe) {
                return null;
            }
        } catch (UnsupportedAddressTypeException uate) {
            SocketAddress localAddr = socket.getLocalSocketAddress();
            throw new RuntimeException("wrong address type: " + (localAddr == null ? null : localAddr.getClass()), uate);
        }
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return socket.getOOBInline();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return socket.getSoLinger();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return socket.getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return socket.isBound();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException("No urgent data.");
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        socket.setOOBInline(on);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    @Override
    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket.setSoLinger(on, linger);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        socket.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }

    @Override
    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    @Override
    public String toString() {
        return "NIOSocket::" + remoteSocketAddress + ", channel: " + channel.toString();
    }
}