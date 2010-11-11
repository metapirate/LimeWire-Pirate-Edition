package com.limegroup.gnutella.daap;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;

import com.google.inject.name.Named;

import de.kapsi.net.daap.DaapConfig;
import de.kapsi.net.daap.Library;
import de.kapsi.net.daap.nio.DaapConnectionNIO;
import de.kapsi.net.daap.nio.DaapServerNIO;

/**
 * A DAAP Server that uses LimeWire's I/O libraries for NIO.
 */
public class LimeDaapServerNIO extends DaapServerNIO {
    
    private static final Log LOG = LogFactory.getLog(LimeDaapServerNIO.class);
    
    private final Map<DaapConnectionNIO, DaapController> allConnections =
        new HashMap<DaapConnectionNIO, DaapController>();
    private ServerSocket serverSocket;
    private final ScheduledExecutorService backgroundExecutor;

    public LimeDaapServerNIO(Library library, DaapConfig config,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        super(library, config);
        this.backgroundExecutor = backgroundExecutor;
        scheduleServices();
    }
    
    /**
     * Schedules a repeated service that will process any connections
     * that should be timed out.
     */
    private void scheduleServices() {
        backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
                    public void run() {
                        synchronized (LimeDaapServerNIO.this) {
                            if (!running)
                                return;
                        }
                        processTimeout();
                    }
                });
            }
        }, 30000, 30000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Binds this server to the SocketAddress supplied by DaapConfig.
     * 
     * @throws IOException
     */    
    @Override
    public void bind() throws IOException {        
        SocketAddress bindAddr = config.getInetSocketAddress();
        int backlog = config.getBacklog();
        serverSocket = SocketFactory.newServerSocket(new DaapDispatcher());
        
        // BugID: 4546610
        // On Win2k, Mac OS X, XYZ it is possible to bind
        // the same address without rising a SocketException
        // (the Documentation lies)
        serverSocket.setReuseAddress(false);
        
        try {
            serverSocket.bind(bindAddr, backlog);
        } catch (SocketException err) {
            throw new BindException(err.getMessage());
        }            
    }
    
    /**
     * Starts this server.
     */
    @Override
    public synchronized void run() {
        running = true;
    }
    
    /**
     * Closes the channel this connection is based on.
     */
    @Override
    protected void cancelConnection(final DaapConnectionNIO connection) {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                Channel channel = connection.getChannel();
                try {
                    channel.close();
                } catch(IOException ignored) {}
            }
        });
    }

    /**
     * Disconnects all connections from this server.
     */
    @Override
    public void disconnectAll() {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                for(DaapConnectionNIO next : allConnections.keySet()) {
                    Channel channel = next.getChannel();
                    try {
                        channel.close();
                    } catch(IOException ignored) {}
                    synchronized(LimeDaapServerNIO.this) {
                        libraryQueue.clear();
                    }
                }
            }
        });
    }

    /**
     * Stops this server.
     */
    @Override
    public synchronized void stop() {
        try {
            serverSocket.close();
        } catch(IOException iox) {}
        
        disconnectAll();
        synchronized(this) {
            running = false;
        }
    }

    /**
     * Forces all connections to process an update.
     */
    @Override
    protected void update() {
        NIODispatcher.instance().getScheduledExecutorService().execute(new Runnable() {
            public void run() {
                synchronized (LimeDaapServerNIO.this) {
                    for(DaapConnectionNIO connection : getDaapConnections()) {
                        for (Library aLibraryQueue : libraryQueue) connection.enqueueLibrary(aLibraryQueue);

                        SelectableChannel channel = connection.getChannel();
                        try {
                            connection.update();
                            DaapController controller = allConnections.get(connection);
                            controller.setOps();
                        } catch (IOException ignored) {
                            try {
                                channel.close();
                            } catch (IOException ignoredToo) {
                            }
                        }
                    }

                    libraryQueue.clear();
                }
            }
        });
    }    
    
    /**
     * An observer for incoming connections.
     * This will only dispatch the connection if the server is
     * running.
     */
    private class DaapDispatcher implements AcceptObserver {
        public void handleIOException(IOException iox) {}
        public void shutdown() {}

        public void handleAccept(Socket socket) throws IOException {
            synchronized(this) {
                if(!running) {
                    IOUtils.close(socket);
                    return;
                }
            }
            
            DaapConnectionNIO connection = new DaapConnectionNIO(LimeDaapServerNIO.this, socket.getChannel());
            DaapController cont = new DaapController(connection);
            socket.setSoTimeout(0);
            allConnections.put(connection, cont);
            addPendingConnection(connection);
            ((NIOMultiplexor)socket).setReadObserver(cont);
            ((NIOMultiplexor)socket).setWriteObserver(cont);
        }        
    }
    
    /**
     * An observer for DAAP connections, to process reading & writing.
     */
    private class DaapController implements ChannelReadObserver, ChannelWriter {
        private DaapConnectionNIO conn;
        private InterestReadableByteChannel readChannel;
        private InterestWritableByteChannel writeChannel;
        private boolean shutdown;

        DaapController(DaapConnectionNIO dcn) {
            this.conn = dcn;
        }

        public void handleRead() throws IOException {
            if (!conn.read())
                throw new IOException("finished");
            else
                setOps();
        }

        public void handleIOException(IOException iox) {
        }

        public InterestReadableByteChannel getReadChannel() {
            return readChannel;
        }

        public void setReadChannel(InterestReadableByteChannel newChannel) {
            this.readChannel = newChannel;
            conn.setReadChannel(newChannel);
            setOps();
        }

        public void shutdown() {
            synchronized (this) {
                if (shutdown)
                    return;
                shutdown = true;
            }

            conn.close();
            allConnections.remove(conn);
            try {
                removeConnection(conn);
            } catch(IllegalStateException ise) {
                // Not a huge deal, just a bug in DAAP.
                LOG.error("ISE", ise);
            }
        }

        public InterestWritableByteChannel getWriteChannel() {
            return writeChannel;
        }

        public void setWriteChannel(InterestWritableByteChannel newChannel) {
            conn.setWriteChannel(newChannel);
            this.writeChannel = newChannel;
            setOps();
        }

        public boolean handleWrite() throws IOException {
            if (!conn.write())
                throw new IOException("finished");
            else
                return setOps();

        }

        /** Interests the right sink channels depending on what we can do. */
        private boolean setOps() {
            int ops = conn.interrestOps();
            boolean moreToWrite = false;

            if (writeChannel != null) {
                boolean write = (ops & SelectionKey.OP_WRITE) != 0;
                moreToWrite = write;
                writeChannel.interestWrite(this, write);
            }

            if (readChannel != null) {
                boolean read = (ops & SelectionKey.OP_READ) != 0;
                readChannel.interestRead(read);
            }

            return moreToWrite;
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }


}
