package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.io.ByteBufferOutputStream;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestWritableByteChannel;

import com.limegroup.gnutella.messages.Message;

/** 
 * Writes messages using non-blocking I/O.
 * <p>
 * Messages are queued via send(Message).  When a write can happen, this is notified
 * via handleWrite(), which will pull any non-expired messages from the queue, writing
 * them.  ConnectionStats are kept updated for all should-be-sent messages as well
 * as dropped messages (from expiry or buffer overflow), and the SentMessageHandler
 * is notified of all successfully sent messages.
 */
public class MessageWriter implements ChannelWriter, OutputRunner {
    
    /**
     * The queue that holds the messages to write.  The queue internally can
     * expire messages which are old, or purge messages if many become buffered.
     */
    private final MessageQueue queue;
    
    /**
     * The OutputStream that messages are written to.  For efficiency, the stream
     * internally uses a ByteBuffer and we get the buffer directly to write to
     * our sink channel.  This prevents recreation of many byte[]s.
     */
    private final ByteBufferOutputStream out;
    
    /**
     * The statistics object that keeps track of how many messages were sent,
     * how many tried to be sent, how many dropped, etc...
     */
    private final ConnectionStats stats;
    
    /**
     * A callback for handlers who wish to process messages we successfully sent.
     */
    private final SentMessageHandler sendHandler;
    
    /**
     * The sink channel we write to & interest ourselves on.
     */
    private InterestWritableByteChannel channel;
    
    /**
     * Whether or not we've flipped the data.  This is an optimization so
     * we don't have to compact (which does array copies) as much.
     */
    private boolean flipped = false;
    
    /**
     * Whether or not we've shut down.  If we have, stop accepting incoming
     * messages & stop writing them.
     */
    private boolean shutdown = false;
    
    private Message message;
    /**
     * Constructs a new MessageWriter with the given stats, queue & sendHandler.
     * You MUST call setWriteChannel prior to handleWrite.
     */
    public MessageWriter(ConnectionStats stats, MessageQueue queue, SentMessageHandler sendHandler) {
        this(stats, queue, sendHandler, null);
    }
    
    /**
     * Constructs a new MessageWriter that writes to the given sink.
     */
    public MessageWriter(ConnectionStats stats, MessageQueue queue,
                         SentMessageHandler sendHandler, InterestWritableByteChannel sink) {
        this.stats = stats;
        this.queue = queue;
        this.sendHandler = sendHandler;
        this.channel = sink;
        out = new ByteBufferOutputStream();
    }
    
    /** The channel we're writing to. */
    public synchronized InterestWritableByteChannel getWriteChannel() {
        return channel;
    }
    
    /** The channel we're writing to. */
    public synchronized void setWriteChannel(InterestWritableByteChannel channel) {
        this.channel = channel;
        channel.interestWrite(this, true);
    }
    
    /**
     * Adds a new message to the queue.
     * <p>
     * Any messages that were dropped because this was added are calculated
     * into the ConnectionStats.  The sink channel is notified that we're
     * interested in writing.
     */
    public synchronized void send(Message m) {
        if(shutdown)
            return;
        
        stats.addSent(m);
        queue.add(m);
        int dropped = queue.resetDropped();
        stats.addSentDropped(dropped);
            
        if(channel != null) 
            channel.interestWrite(this, true);
    }
        
    /**
     * Writes as many messages as possible to the sink.
     */
    public synchronized boolean handleWrite() throws IOException {
        if(channel == null)
            throw new IllegalStateException("writing with no source.");
            
        // first try to write any leftover data.
        if(writeRemaining()) { //still have data to send.
            return true;
        } else {
            if (message != null) {
                sendHandler.processSentMessage(message);
                message = null;
            }
        }
        
            
        // then loop through and write to the channel till we can't anymore.
        while(true) {
            Message m = queue.removeNext();
            int dropped = queue.resetDropped();
            stats.addSentDropped(dropped);
            
            // no more messages to send.
            if(m == null) {
                channel.interestWrite(this, false);
                return false;
            }
            
            m.writeQuickly(out);
            if(writeRemaining()) {// still have data to send.
                message = m;
                return true;
            } else {
                sendHandler.processSentMessage(m);
                message = null;
            }
        }
    }
    
    /**
     * Writes any data that was left in the buffer.  As an optimization,
     * we do not recompact the buffer if more data can be written.  Instead,
     * we just wait till we can completely write the buffer & then clear it
     * entirely.  This prevents the need to compact the buffer.
     */
    private boolean writeRemaining() throws IOException {
        if(shutdown)
            throw new IOException("connection shut down.");
        
        // if there was data left in the stream, try writing it.
        ByteBuffer buffer = out.getBuffer();
        
        // write any data that was leftover in the buffer.
        if(flipped || buffer.position() > 0) {
            // prepare for writing...
            if(!flipped) {
                buffer.flip();
                flipped = true;
            }

            // write.
            channel.write(buffer);
            
            // if we couldn't write everything, exit.
            if(buffer.hasRemaining())
                return true; // still have data to write.
                
            flipped = false;
            buffer.clear();
        }
        return false; // wrote everything.
    }
    
    /**
     * Ignored -- we'll shut down from reading.
     * <p>
     * THIS MUST NOT CLOSE THE CONNECTION.  (Connection.close calls this.)
     */
    public synchronized void shutdown() {
        shutdown = true;
    }
    
    /** Unused, unsupported. */
    public void handleIOException(IOException x) {
        throw new RuntimeException("Unsupported", x);
    }
}