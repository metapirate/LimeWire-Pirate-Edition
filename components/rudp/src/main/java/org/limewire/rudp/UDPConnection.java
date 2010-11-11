package org.limewire.rudp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

/** 
 *  A reliable UDP connection.
 */
class UDPConnection extends AbstractNBSocket implements RUDPSocket {
    
    /** Channel backing the socket. */
    private final UDPSocketChannel channel;

    /** The default read timeout. */
    private int soTimeout = 1 * 60 * 1000; // default to 1 minute.
    
    /** The context surrounding this connection . */
    private final RUDPContext context;

    /**
     * Creates an unconnected <code>UDPConnection</code>. You must call {@link #connect(SocketAddress) connect(...)} to connect.
     */
    UDPConnection(RUDPContext context, UDPSocketChannel channel) {
        this.context = context;
        this.channel = channel;
        setInitialReader();
        setInitialWriter();  
    }

    /** Returns the <code>UDPSocketChannel</code>, since it already implements 
     * <code>InterestReadChannel</code>. */
    @Override
    protected InterestReadableByteChannel getBaseReadChannel() {
        return channel;
    }
    
    /** Returns the <code>UDPSocketChannel</code>, since it already implements 
     * <code>InterestWriteChannel</code>. */
    @Override
    protected InterestWritableByteChannel getBaseWriteChannel() {
        return channel;
    }

    /** Doesn't do anything. */
    @Override
    protected void shutdownImpl() {
    }

    /** Sets the read timeout this socket should use. */
	@Override
    public void setSoTimeout(int timeout) {
        soTimeout = timeout;
	}
	
	/** Returns the timeout this socket uses when reading. */
	@Override
    public int getSoTimeout() {
	    return soTimeout;
	}

    /** Returns the local address this socket uses. */
    @Override
    public InetAddress getLocalAddress() {
        return context.getUDPService().getStableListeningAddress();
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        return channel.getRemoteSocketAddress();
    }
    
    @Override
    public InetAddress getInetAddress() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getAddress();
    }

    @Override
    public int getPort() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getPort();
    }

    @Override
    public int getLocalPort() {
        return context.getUDPService().getStableListeningPort();
    }
    
    @Override
    public SocketAddress getLocalSocketAddress() {
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }
    
    @Override
    public SocketChannel getChannel() {
        return channel;
    }    

    @Override
    public String toString() {
        return "UDPConnection:" + channel;
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    @Override
    public boolean isInputShutdown() {
        return !channel.isOpen();
    }

    @Override
    public boolean isOutputShutdown() {
        return !channel.isOpen();
    }
    
    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        // does nothing
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return true;
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        // does nothing
    }

    @Override
    public int getSoLinger() throws SocketException {
        return -1;
    }

    @Override
    public void sendUrgentData(int data) throws IOException  {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return UDPConnectionProcessor.DATA_CHUNK_SIZE * UDPConnectionProcessor.DATA_WINDOW_SIZE;
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return UDPConnectionProcessor.MAX_DATA_SIZE * UDPConnectionProcessor.DATA_WINDOW_SIZE;
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        // ignore
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public int getTrafficClass() throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void shutdownInput() throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void shutdownOutput() throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }
    
}
