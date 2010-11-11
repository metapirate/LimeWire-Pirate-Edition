/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.MessageFormatException;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.Verifier;

/**
 * This is a stand alone/reference implementation of <code>MessageDispatcher</code>.
 */
public class MessageDispatcherImpl extends MessageDispatcher implements Runnable {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    /**
     * The maximum time to wait on 'lock'
     */
    private static final long WAIT_ON_LOCK = 5000L;
    
    /**
     * The receive buffer size for the Socket.
     */
    private static final int RECEIVE_BUFFER_SIZE 
        = NetworkSettings.RECEIVE_BUFFER_SIZE.getValue();
    
    /**
     * The send buffer size for the Socket.
     */
    private static final int SEND_BUFFER_SIZE 
        = NetworkSettings.SEND_BUFFER_SIZE.getValue();
    
    /**
     * Sleep timeout of the Selector
     */
    private static final long SELECTOR_SLEEP = 50L;
    
    /**
     * A flag whether or not this MessageDispatcher is running.
     */
    private volatile boolean running = false;
    
    /**
     * A flag whether or not this MessageDispatcher is accepting incoming 
     * Requests and Responses.
     */
    private volatile boolean accepting = false;
    
    /**
     * The DatagramChannel's Selector.
     */
    private Selector selector;
    
    /**
     * The DatagramChanel
     */
    private DatagramChannel channel;
    
    /**
     * The DatagramChannel lock Object.
     */
    private final Object lock = new Object();

    /**
     * The Thread this MessageDispatcher is running on.
     */
    private Thread thread;
    
    /**
     * Buffer for incoming Messages.
     */
    private final ByteBuffer receiveBuffer;
    
    /**
     * Lists of tasks we've to execute.
     */
    private List<Runnable> tasks = new ArrayList<Runnable>();
    
    /** 
     * Queue of things we have to send. 
     */
    private List<Tag> outputQueue = new LinkedList<Tag>();
    
    /**
     * Whether or not a new ByteBuffer should be allocated for
     * every message we receive.
     */
    private volatile boolean allocateNewByteBuffer 
        = NetworkSettings.ALLOCATE_NEW_BUFFER.getValue();
    
    public MessageDispatcherImpl(Context context) {
        super(context);
        
        receiveBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
    }
    
    /**
     * Sets whether or not a new ByteBuffer should be allocated.
     */
    public void setAllocateNewByteBuffer(boolean allocateNewByteBuffer) {
        this.allocateNewByteBuffer = allocateNewByteBuffer;
    }
    
    /**
     * Returns whether or not a new ByteBuffer is allocated for
     * every message.
     */
    public boolean getAllocateNewByteBuffer() {
        return allocateNewByteBuffer;
    }
    
    @Override
    public void bind(SocketAddress address) throws IOException {
        synchronized (lock) {
            if (isBound()) {
                throw new IOException("DatagramChannel is already bound");
            }
            
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            
            DatagramSocket socket = channel.socket();
            socket.setReuseAddress(false);
            socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            socket.setSendBufferSize(SEND_BUFFER_SIZE);
            
            socket.bind(address);
        }
    }
    
    /**
     * Returns true if the DatagramChannel is open.
     */
    public boolean isOpen() {
        synchronized (lock) {
            return channel != null && channel.isOpen();
        }
    }
    
    @Override
    public boolean isBound() {
        synchronized (lock) {
            return channel != null && channel.socket().isBound();
        }
    }
    
    /**
     * Returns the DatagramChannel.
     */
    public DatagramChannel getDatagramChannel() {
        synchronized (lock) {
            return channel;
        }
    }
    
    /**
     * Returns the DatagramChannel Socket's local SocketAddress.
     */
    public SocketAddress getLocalSocketAddress() {
        synchronized (lock) {
            if (channel != null && channel.isOpen()) {
                return channel.socket().getLocalSocketAddress();
            }
            return null;
        }
        
    }
    
    @Override
    public void start() {
        synchronized (lock) {
            if (!isBound()) {
                throw new IllegalStateException("MessageDispatcher is not bound");
            }
            
            if (!running) {
                accepting = true;
                running = true;
                
                thread = context.getDHTExecutorService().getThreadFactory().newThread(this);
                thread.setName(context.getName() + "-MessageDispatcherThread");
                thread.setDaemon(Boolean.getBoolean("com.limegroup.mojito.io.MessageDispatcherIsDaemon"));
                thread.start();
                
                Runnable startup = new Runnable() {
                    public void run() {
                        synchronized(lock) {
                            try {
                                MessageDispatcherImpl.super.start();
                            } finally {
                                lock.notifyAll();
                            }
                        }
                    }
                };
                
                process(startup);
                
                try {
                    lock.wait(WAIT_ON_LOCK);
                } catch (InterruptedException err) {
                    LOG.error("InterruptedException", err);
                }
            }
        }
    }
    
    @Override
    protected boolean submit(final Tag tag) {
        Runnable task = new Runnable() {
            public void run() {
                outputQueue.add(tag);
                interestWrite(true);
            }
        };
        
        process(task);
        return true;
    }
    
    /**
     * Writes all Messages (if possible) from the output
     * queue to the Network and returns whether or not some
     * Messages were left in the output queue.
     */
    private void handleWrite() throws IOException {
        
        Tag tag = null;
        while (!outputQueue.isEmpty()) {
            tag = outputQueue.get(0);

            if (tag.isCancelled()) {
                outputQueue.remove(0);
                continue;
            }

            try {
                SocketAddress dst = tag.getSocketAddress();
                ByteBuffer data = tag.getData();
                
                if (send(dst, data)) {
                    // Wohoo! Message was sent!
                    outputQueue.remove(0);
                    register(tag);
                } else {
                    // Dang! Re-Try next time!
                    break;
                }
            } catch (IOException err) {
                LOG.error("IOException", err);
                outputQueue.remove(0);
                handleError(tag, err);
            }
        }
        
        interestWrite(!outputQueue.isEmpty());
    }
    
    @Override
    public void stop() {
        synchronized (lock) {
            // Do not accept any new incoming Requests or Responses
            accepting = false;
            
            if (isRunning()) {
                Runnable shutdown = new Runnable() {
                    public void run() {
                        synchronized (lock) {
                            try {
                                running = false;
                                MessageDispatcherImpl.super.stop();
                            } finally {
                                lock.notifyAll();
                            }
                        }
                    }
                };

                process(shutdown);
                
                try {
                    lock.wait(WAIT_ON_LOCK);
                } catch (InterruptedException err) {
                    LOG.error("InterruptedException", err);
                }
                
                if (thread != null) {
                    thread.interrupt();
                    thread = null;
                }
                
                tasks.clear();
                outputQueue.clear();
            }
        }
    }
    
    @Override
    public void close() {
        super.close();
        
        synchronized (lock) {
            assert !isRunning(); // the call above should stop this.
            if (selector != null) {
                try {
                    selector.close();
                    selector = null;
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
            
            if (channel != null) {
                try {
                    channel.close();
                    channel = null;
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
        }
    }

    @Override
    public boolean isAccepting() {
        return accepting;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Reads all available Message from Network and processes them.
     */
    private void handleRead() throws IOException {
        while(isRunning()) {
            DHTMessage message = null;
            try {
                message = readMessage();
            } catch (MessageFormatException err) {
                LOG.error("Message Format Exception: ", err);
                continue;
            }
            
            if (message == null) {
                break;
            }
            
            handleMessage(message);
        }
        
        // We're always interested in reading!
        interestRead(true);
    }
    
    /**
     * Reads and returns a single DHTMessage from Network or null
     * if no Messages were in the input queue.
     */
    private DHTMessage readMessage() throws MessageFormatException, IOException {
        SocketAddress src = receive((ByteBuffer)receiveBuffer.clear());
        if (src != null) {
            receiveBuffer.flip();
            
            ByteBuffer data = null;
            if (getAllocateNewByteBuffer()) {
                int length = receiveBuffer.remaining();
                data = ByteBuffer.allocate(length);
                data.put(receiveBuffer);
                data.rewind();
            } else {
                data = receiveBuffer.slice();
            }
            
            DHTMessage message = deserialize(src, data/*.asReadOnlyBuffer()*/);
            return message;
        }
        return null;
    }
    
    @Override
    protected void process(Runnable runnable) {
        synchronized (lock) {
            if (isRunning()) {
                tasks.add(runnable);            
                selector.wakeup();
            }
        }        
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        // Verifying the signature is an expensive Task and should
        // be done on a different Thread than MessageDispatcher's
        // Executor Thread. On the other hand are the chances slim to none
        // that a Node will ever receive a SecureMessage. It's a trade off
        // at the end if it's really an issue or waste of resources...
        // NOTE: LimeDHTMessageDispatcher is using a different implementation!
        //       This is the stand alone implementation!
        
        final PublicKey pubKey = context.getPublicKey();
        if (pubKey == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping SecureMessage " 
                        + secureMessage + " because PublicKey is not set");
            }
            return;
        }
        
        Verifier verifier = new Verifier(secureMessage, smc) {
            @Override
            public String getAlgorithm() {
                return CryptoUtils.SIGNATURE_ALGORITHM;
            }

            @Override
            public PublicKey getPublicKey() {
                return pubKey;
            }
        };
        
        verify(verifier);
    }

    /**
     * Called by verify(SecureMessage, SecureMessageCallback) to execute
     * the Runnable that does the actual verification. You may override
     * this method to execute the Runnable on a different Thread.
     */
    protected void verify(Runnable verifier) {
        // See verify(SecureMessage, SecureMessageCallback)
        process(verifier);
    }
    
    private void interest(int ops, boolean on) {
        try {
            SelectionKey sk = channel.keyFor(selector);
            if (sk != null && sk.isValid()) {
                synchronized(channel.blockingLock()) {
                    if (on) {
                        sk.interestOps(sk.interestOps() | ops);
                    } else {
                        sk.interestOps(sk.interestOps() & ~ops);
                    }
                }
            }
        } catch (CancelledKeyException ignore) {}
    }
    
    /** 
     * Called to indicate an interest in reading something from
     * the Network. Override this method if you need this functionality!
     */
    private void interestRead(boolean on) {
        interest(SelectionKey.OP_READ, on);
    }
    
    /** 
     * Called to indicate an interest in writing something to
     * the Network. Override this method if you need this functionality!
     */
    private void interestWrite(boolean on) {
        interest(SelectionKey.OP_WRITE, on);
    }
    
    /**
     * The raw read-method.
     */
    private SocketAddress receive(ByteBuffer dst) throws IOException {
        return channel.receive(dst);
    }
    
    /**
     * The actual send method. Returns true if the data was
     * sent or false if there was insufficient space in the
     * output buffer (that means you'll have to re-try it later
     * again).
     * <p>
     * IMPORTANT: The expected behavior is the same as 
     * DatagramChannel.send(BytBuffer,SocketAddress). That means
     * if you are not able to send the data return false and 
     * leave the ByteBuffer untouched!
     */
    // We could pass a slice to this method to enforce the expected
    // behavior but there's maybe an use-case like Kademlia over TCP
    // where it makes sense to send the data piece-by-piece...
    private boolean send(SocketAddress dst, ByteBuffer data) throws IOException {
        return channel.send(data, dst) > 0;
    }
    
    private void processAll() {
        List<Runnable> process = null;
        synchronized (lock) {
            process = tasks;
            tasks = new ArrayList<Runnable>();
        }
        
        for (Runnable task : process) {
            task.run();
        }
    }
    
    public void run() {
        try {
            while (true) {
                
                processAll();
                
                if (!isRunning() || !isOpen()) {
                    break;
                }
                
                selector.select(SELECTOR_SLEEP);
                
                try {
                    // READ
                    handleRead();
                } catch (IOException err) {
                    LOG.error("IOException-READ", err);
                }
                
                try {
                    // WRITE
                    handleWrite();
                } catch (IOException err) {
                    LOG.error("IOException-WRITE", err);
                }
            }
        } catch (IOException err) {
            // Pass it to the UncaughtExceptionHandler
            Thread.currentThread().getUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), err);
        }
    }
}
