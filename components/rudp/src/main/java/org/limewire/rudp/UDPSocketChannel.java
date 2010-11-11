package org.limewire.rudp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.listener.EventBroadcaster;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.nio.observer.WriteObserver;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.SynMessage.Role;
import org.limewire.util.BufferUtils;


/**
 * Interface between reading channels & UDP's data.
 * Analogous to SocketChannel combined w/ a SocketInterestReadAdapter & SocketInterestWriteAdapter.
 * <p>
 * This class _is_ the SocketChannel for UDP, except because we wrote it,
 * we can make it implement InterestReadChannel & InterestWriteChannel, so
 * we don't need the additional InterestAdapter.
 */
public class UDPSocketChannel extends AbstractNBSocketChannel implements InterestReadableByteChannel,
                                                        InterestWritableByteChannel,
                                                        ChunkReleaser {
    
    private static final Log LOG = LogFactory.getLog(UDPSocketChannel.class);

    /** The processor this channel is writing to / reading from. */
    private final UDPConnectionProcessor processor;
    
    /** The <tt>RUDPContext</tt> containing the TransportListener to notify for pending events. */
    private final RUDPContext context;
    
    /** The Socket object this UDPSocketChannel is used for. */
    private final AbstractNBSocket socket;
    
    /** The DataWindow containing incoming read data. */
    private final DataWindow readData;
    
    /** The WriteObserver that last requested interest from us. */
    private volatile WriteObserver writer;
    
    /** The list of buffered chunks that need to be written out. */
    private final ArrayList<ByteBuffer> chunks;
    
    /** The current chunk we're writing to. */
    private ByteBuffer activeChunk;
    
    /** Whether or not we've handled one write yet. */
    private boolean writeHandled = false;
    
    /** A lock to hold while manipulating chunks or activeChunk. */
    private final Object writeLock = new Object();
    
    /** Whether or not we've propagated the shutdown to other writers. */
    private boolean shutdown = false;

    private final Role role;
    
    protected UDPSocketChannel(SelectorProvider provider,
                     RUDPContext context,
                     Role role,
                     EventBroadcaster<UDPSocketChannelConnectionEvent> connectionStateEventBroadcaster) {
        super(provider);
        this.context = context;
        this.role = role;
        this.processor = new UDPConnectionProcessor(this, context, role, connectionStateEventBroadcaster);
        this.readData = processor.getReadWindow();
        this.chunks = new ArrayList<ByteBuffer>(5);
        this.socket = new UDPConnection(context, this);
        allocateNewChunk();
        try {
            configureBlocking(false);
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
    }
    
    // for testing.
    UDPSocketChannel(UDPConnectionProcessor processor, Role role) {
        super(null);
        this.role = role;
        this.context = new DefaultRUDPContext();
        this.processor = processor;
        this.readData = processor.getReadWindow();
        this.chunks = new ArrayList<ByteBuffer>(5);
        this.socket = new UDPConnection(context, this);
        allocateNewChunk();
        try {
            configureBlocking(false);
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
    }
    
    public boolean isForMe(InetSocketAddress address, SynMessage message) {
        if(!getRemoteSocketAddress().equals(address)) {
            return false;
        } 
        if (!role.canConnectTo(message.getRole())) {
            return false;
        }
        byte theirConnectionId = processor.getTheirConnectionID();
        if (theirConnectionId != UDPMultiplexor.UNASSIGNED_SLOT 
                && theirConnectionId != message.getSenderConnectionID()) {
            return false;
        }
        return true;
    }
    
    UDPConnectionProcessor getProcessor() {
        return processor;
    }

    /**
     * Sets read interest on or off in the processor.
     */
    public void interestRead(boolean status) {
        NIODispatcher.instance().interestRead(this, status);
    }
    
    /// ********** reading ***************

    /**
     * Reads all possible data from the DataWindow into the ByteBuffer,
     * sending a keep alive if more space became available.
     */
    long last = 0;
    @Override
    public int read(ByteBuffer to) throws IOException {
        // It is possible that the channel is open but the processor
        // is closed.  In that case, this will return -1.
        // Once this closes, it throws CCE.
        if(!isOpen())
            throw new ClosedChannelException();
        
        synchronized (processor) {
            // Now that we've transferred all we can to the buffer, clear up
            // the space & send a keep-alive if necessary
            // Record how much space was previously available in the receive window
            int priorSpace = readData.getWindowSpace();

            int read = 0;
            DataRecord currentRecord = readData.getReadableBlock();
            while (currentRecord != null) {
                read += transfer(currentRecord, to);
                if (!to.hasRemaining())
                    break;

                // If to still has room left, we must have written
                // all we could from the record, so we assign a new one.
                // Fetch a block from the receiving window.
                currentRecord = readData.getReadableBlock();
            }

            // Remove all records we just read from the receiving window
            int cleared = readData.clearEarlyReadBlocks();

            // If the receive window opened up then send a special
            // KeepAliveMessage so that the window state can be
            // communicated.
            if ((cleared > 0 && priorSpace == 0)
                    /*|| (priorSpace <= UDPConnectionProcessor.SMALL_SEND_WINDOW &&
                        readData.getWindowSpace() > UDPConnectionProcessor.SMALL_SEND_WINDOW)*/) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending aritifial keep alive: cleared=" + cleared + ", priorSpace="+ priorSpace + ", read=" + read + ", windowSpace=" + readData.getWindowSpace()  + ", windowStart=" + readData.getWindowStart());

                processor.sendKeepAlive();
            }
        
            if(read == 0 && processor.isClosed())
                return -1;
            else
                return read;
        }
    }
    
    /**
     * Transfers the chunks in the DataRecord's msg to the ByteBuffer. Sets the record as being successfully read after
     * all data is read from it.
     */
    private int transfer(DataRecord record, ByteBuffer to) {
        DataMessage msg = record.msg;
        int read = 0;
        
        ByteBuffer chunk = msg.getData1Chunk();
        if(chunk.hasRemaining())
            read += BufferUtils.transfer(chunk, to, false);
        
        if(chunk.hasRemaining())
            return read;
        
        chunk = msg.getData2Chunk();
        read += BufferUtils.transfer(chunk, to, false);
        
        if(!chunk.hasRemaining())
            record.read = true;
        
        return read;
    }
    
    /// ********** writing ***************
    
    /**
     * Writes all data in src into a list of internal chunks.
     * This will notify the processor if we have no pending chunks prior
     * to writing, so that it will know to retrieve some data.
     * Chunks will be created until the processor tells us we're at the limit,
     * at which point this will forcibly will return the amount of data that
     * could be written so far.
     * If all data is emptied from src, this will return that amount of data.
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        // We cannot write if either the channel or the processor is closed.
        if(!isOpen() || processor.isClosed())
            throw new ClosedChannelException();
            
        synchronized(writeLock) {
            // If there was no data before this, then ensure a writer is awake
            if ( getNumberOfPendingChunks() == 0 ) {
                processor.wakeupWriteEvent(!writeHandled);
            }
            
            writeHandled = true;
            
            int wrote = 0;
            while (true) {
                if(src.hasRemaining()) {
                    if (activeChunk.hasRemaining()) {
                        wrote += BufferUtils.transfer(src, activeChunk, false);
                    } else if (chunks.size() < processor.getChunkLimit()) {
                        // If there is room for more chunks, allocate a new chunk
                        chunks.add(activeChunk);
                        allocateNewChunk();
                    } else {
                        return wrote;
                    }
                } else {
                    return wrote;
                }
            }
        }   
    }
    
    /**
     *  Allocates a chunk for writing to.
     */
    private void allocateNewChunk() {
        activeChunk = NIODispatcher.instance().getBufferCache().getHeap(UDPConnectionProcessor.DATA_CHUNK_SIZE);
    }
    
    /**
     * Releases a chunk.
     */
    public void releaseChunk(ByteBuffer chunk) {
        NIODispatcher.instance().getBufferCache().release(chunk);
    }

    /**
     *  Gets the first chunk of data that should be written to the wire.
     *  Returns null if no data.
     */
    ByteBuffer getNextChunk() {
        synchronized(writeLock) {
            ByteBuffer rChunk;
            if ( chunks.size() > 0 ) {
                // Return the oldest chunk 
                rChunk = chunks.remove(0);
                rChunk.flip();
            } else if (activeChunk.position() > 0) {
                rChunk = activeChunk;
                rChunk.flip();
                allocateNewChunk();
            } else {
                // If no data currently, return null
                rChunk = null;
            }            
            return rChunk;
        }
    }
    
    /**
     *  Return how many pending chunks are waiting on being written to the wire.
     */
    int getNumberOfPendingChunks() {
        synchronized(writeLock) { 
            // Add the number of list blocks
            int count = chunks.size();
            
            // Add one for the current block if data available.
            if (activeChunk.position() > 0)
                count++;
    
            return count;
        }
    }
    
    Object writeLock() {
        return writeLock;
    }

    /// ********** SelectableChannel methods. ***************

    /** Closes the processor. */
    @Override
    protected void implCloseSelectableChannel() throws IOException {        
        processor.close();
    }

    /// ********** InterestWriteChannel methods. ***************

    /**
     * Shuts down this channel & processor, notifying the last interested party
     * that we're now shutdown.
     */
    public void shutdown() {
        synchronized(this) {
            if(shutdown)
                return;
            shutdown = true;
        }
        
        Shutdownable chain = writer;
        if(chain != null)
            chain.shutdown();
        writer = null;
    }

    /** Sets interest on or off on the channel & stores the interested party for future writing. */
    public void interestWrite(WriteObserver observer, boolean status) {
        if(isOpen()) { 
            writer = observer;
            NIODispatcher.instance().interestWrite(this, status);
        }
    }

    /** Sends a write up the chain to the last interest party. */
    public boolean handleWrite() throws IOException {
        WriteObserver chain = writer;
        if(chain != null)
            return chain.handleWrite();
        else
            return false;
    }

    /** Unused. */
    public void handleIOException(IOException iox) {
        throw new UnsupportedOperationException();
    }
    
    public InetSocketAddress getRemoteSocketAddress() {
        return processor.getSocketAddress();
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        processor.connect((InetSocketAddress)remote);
        return false;
    }

    @Override
    public boolean finishConnect() throws IOException {
        return processor.prepareOpenConnection();
    }

    @Override
    public boolean isConnected() {
        return processor.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return processor.isConnecting();
    }

    @Override
    public AbstractNBSocket socket() {
        return socket;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        throw new IOException("unsupported");
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        // does nothing.
    }    
    
    void eventPending() {
    	context.getTransportListener().eventPending();
    }

    public boolean hasBufferedOutput() {
        return getNumberOfPendingChunks() > 0;
    }

    @Override
    public String toString() {
        InetSocketAddress addr = getRemoteSocketAddress();
        if(addr == null) {
            return "[disconnected]";
        } else {
            return addr.toString();
        }
    }
}
