package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.NBSocket;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.http.HTTPConnectionData;

/**
 * Manages state for push upload requests.
 */
@EagerSingleton
public final class PushManager implements EventListener<ConnectBackRequestedEvent> {
    
    private static final Log LOG = LogFactory.getLog(PushManager.class);

    /**
     * The timeout for the connect time while establishing the socket. Set to
     * the same value as NORMAL_CONNECT_TIME is ManagedDownloader.
     */
    private static final int CONNECT_TIMEOUT = 10000;//10 secs
    
    private final Provider<SocketsManager> socketsManager;
    private final Provider<HTTPAcceptor> httpAcceptor;
    private final Provider<UDPSelectorProvider> udpSelectorProvider;
    private final Provider<NetworkManager> networkManager;

    /**
     * @param socketsManager
     * @param httpAcceptor
     */
    @Inject
    public PushManager(Provider<SocketsManager> socketsManager,
            Provider<HTTPAcceptor> httpAcceptor,
            Provider<UDPSelectorProvider> udpSelectorProvider,
            Provider<NetworkManager> networkManager) {
        this.socketsManager = socketsManager;
        this.httpAcceptor = httpAcceptor;
        this.udpSelectorProvider = udpSelectorProvider;
        this.networkManager = networkManager;
    }    
    
    @Inject
    void register(ListenerSupport<ConnectBackRequestedEvent> connectBackRequestedEventListenerSupport) {
        // listener is leaked, but both are singleton scope, so it's fine
        connectBackRequestedEventListenerSupport.addListener(this);
    }

	/**
	 * Accepts a new push upload asynchronously.
	 * <p>
     * Essentially, this is a reverse-Acceptor.
     * <p>
     * No file and index are needed since the GET/HEAD will include that
     * information.  Just put in our first file and filename to create a
     * well-formed.
	 * @param address the connectable that will be connected to
	 * @param guid the unique identifying client guid of the push-connecting client
     * @param lan whether or not this is a request over a local network (
     * (force the UploadManager to accept this request when it comes back)
     * @param isFWTransfer whether or not to use a UDP pipe to service this
     * upload.
	 */
	public void acceptPushUpload(Connectable address,
	                             final GUID guid,
                                 final boolean lan,
                                 final boolean isFWTransfer) {
        if (LOG.isDebugEnabled())
            LOG.debug("Accepting Push Upload from host:" + address  + " FW:" + isFWTransfer);
                                    
        if( address == null )
            throw new NullPointerException("null host");
        if( !NetworkUtils.isValidIpPort(address) )
            throw new IllegalArgumentException("invalid  ip port: " + address);
        if( guid == null )
            throw new NullPointerException("null guid");
        
        // We used to have code here that tested if the guy we are pushing to is
        // 1) hammering us, or 2) is actually firewalled.  1) is done above us
        // now, and 2) isn't as much an issue with the advent of connectback
        
        PushData data = new PushData(address, guid, lan);
        
        // If the transfer is to be done using FW-FW, then immediately start a new thread
        // which will connect using FWT.  Otherwise, do a non-blocking connect and have
        // the observer spawn the thread only if it succesfully connected.
        if(isFWTransfer) {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding push observer FW-FW to host: " + address);
            // TODO: should FW-FW connections also use TLS?
            NBSocket socket = udpSelectorProvider.get().openAcceptorSocketChannel().socket();
            socket.connect(address.getInetSocketAddress(), CONNECT_TIMEOUT*2, new PushConnectObserver(data, true, httpAcceptor.get()));
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding push observer to host: " + address);
            try {
                ConnectType type = address.isTLSCapable() && networkManager.get().isOutgoingTLSEnabled() ? ConnectType.TLS : ConnectType.PLAIN;
                socketsManager.get().connect(address.getInetSocketAddress(), CONNECT_TIMEOUT, new PushConnectObserver(data, false, httpAcceptor.get()), type);
            } catch(IOException iox) {
            }
        }
    }
    
    /** A simple collection of Push information */
    private static class PushData {
        
        private final Connectable address;
        private final GUID guid;
        private final boolean lan;
        
        PushData(Connectable address, GUID guid, boolean lan) {
            this.address = address;
            this.guid = guid;
            this.lan = lan;
        }
        
        public boolean isLan() {
            return lan;
        }
        public GUID getGuid() {
            return guid;
        }
        
        public Connectable getAddress() {
            return address;
        }
    }
    
    /** Non-blocking observer for connect events related to pushing. */
    private static class PushConnectObserver implements ConnectObserver {
        private final PushData data;
        private final boolean fwt;
        private final HTTPAcceptor httpAcceptor;
        
        PushConnectObserver(PushData data, boolean fwt, HTTPAcceptor httpAcceptor) {
            this.data = data;
            this.fwt = fwt;
            this.httpAcceptor = httpAcceptor;
        }        
        
        public void handleIOException(IOException iox) {}

        /** Increments the PUSH_FAILED stat and does nothing else. */
        public void shutdown() {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getAddress() + " failed");
        }

        /** Starts a new thread that'll do the pushing. */
        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getAddress() + " succeeded");
            ((NIOMultiplexor) socket).setWriteObserver(new GivLineWriter(socket, data, fwt, httpAcceptor));
        }
    }    

    /** Non-blocking writer that writes out the give line after a socket connection has been established. */
    private static class GivLineWriter implements ChannelWriter {
        
        private InterestWritableByteChannel channel;
        private final ByteBuffer buffer;
        private final Socket socket;
        private HTTPConnectionData data;
        private HTTPAcceptor httpAcceptor;

        public GivLineWriter(Socket socket, PushData data, boolean fwTransfer,
                HTTPAcceptor httpAcceptor) throws IOException {
            this.socket = socket;
            this.data = new HTTPConnectionData();
            this.data.setPush(true);
            this.data.setLocal(data.isLan());
            this.data.setFirewalled(fwTransfer);
            this.httpAcceptor = httpAcceptor;
            
            socket.setSoTimeout(30 * 1000);
            
            String giv = "GIV 0:" + data.getGuid() + "/file\n\n";
            this.buffer = ByteBuffer.wrap(StringUtils.toAsciiBytes(giv));
        }

        public boolean handleWrite() throws IOException {
            if (!buffer.hasRemaining()) {
                return false;
            }

            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    return true;
                }
            }

            httpAcceptor.acceptConnection(socket, data);
            return false;
        }

        public void handleIOException(IOException iox) {
            throw new RuntimeException();
        }

        public void shutdown() {
            // ignore
    }

        public InterestWritableByteChannel getWriteChannel() {
            return channel;
        }

        public void setWriteChannel(InterestWritableByteChannel newChannel) {
            this.channel = newChannel;

            if (newChannel != null) {
                newChannel.interestWrite(this, true);
        }
    }
    }

    @Override
    public void handleEvent(ConnectBackRequestedEvent event) {
        ConnectBackRequest request = event.getData();
        // can assume false for lan, since same NAT resolver would have spotted that and opened a direct connection
        acceptPushUpload(request.getAddress(), request.getClientGuid(), false, request.getSupportedFWTVersion() > 0);
    }
}
