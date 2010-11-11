package com.limegroup.gnutella.connection;

import java.net.Socket;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;

/**
 * An implementation of {@link RoutedConnectionFactory} that constructs {@link GnutellaConnection GnutellaConnections}.
 */
@Singleton
public class RoutedConnectionFactoryImpl implements RoutedConnectionFactory {

    private final Provider<ConnectionManager> connectionManager;

    private final NetworkManager networkManager;

    private final QueryRequestFactory queryRequestFactory;

    private final HeadersFactory headersFactory;

    private final HandshakeResponderFactory handshakeResponderFactory;

    private final QueryReplyFactory queryReplyFactory;

    private final Provider<MessageDispatcher> messageDispatcher;

    private final Provider<SearchResultHandler> searchResultHandler;

    private final CapabilitiesVMFactory capabilitiesVMFactory;

    private final Provider<SocketsManager> socketsManager;

    private final Provider<Acceptor> acceptor;

    private final MessagesSupportedVendorMessage supportedVendorMessage;

    private final Provider<ConnectionServices> connectionServices;

    private final GuidMapManager guidMapManager;

    private final SpamFilterFactory spamFilterFactory;

    private final MessageFactory messageFactory;

    private final MessageReaderFactory messageReaderFactory;

    private final ApplicationServices applicationServices;
    
    private final Provider<SecureMessageVerifier> secureMessageVerifier;
    
    private final OutOfBandStatistics outOfBandStatistics;
    
    private final NetworkInstanceUtils networkInstanceUtils;

    @Inject
    public RoutedConnectionFactoryImpl(Provider<ConnectionManager> connectionManager,
            NetworkManager networkManager, QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory, HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, Provider<MessageDispatcher> messageDispatcher,
            Provider<SearchResultHandler> searchResultHandler,
            CapabilitiesVMFactory capabilitiesVMFactory, Provider<SocketsManager> socketsManager,
            Provider<Acceptor> acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageFactory messageFactory,
            MessageReaderFactory messageReaderFactory, ApplicationServices applicationServices,
            Provider<SecureMessageVerifier> secureMessageVerifier, OutOfBandStatistics outOfBandStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.applicationServices = applicationServices;
        this.searchResultHandler = searchResultHandler;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.connectionServices = connectionServices;
        this.guidMapManager = guidMapManager;
        this.spamFilterFactory = spamFilterFactory;
        this.messageFactory = messageFactory;
        this.messageReaderFactory = messageReaderFactory;
        this.secureMessageVerifier = secureMessageVerifier;
        this.outOfBandStatistics = outOfBandStatistics;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    public RoutedConnection createRoutedConnection(String host, int port) {
        return createRoutedConnection(host, port, ConnectType.PLAIN);
    }

    public RoutedConnection createRoutedConnection(String host, int port, ConnectType type) {
        return new GnutellaConnection(host, port, type, connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), searchResultHandler,
                capabilitiesVMFactory, socketsManager.get(), acceptor.get(),
                supportedVendorMessage, connectionServices,
                guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                applicationServices, secureMessageVerifier.get(), outOfBandStatistics, networkInstanceUtils);
    }

    public RoutedConnection createRoutedConnection(Socket socket) {
        return new GnutellaConnection(socket, connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), searchResultHandler, 
                capabilitiesVMFactory, acceptor.get(), supportedVendorMessage,
                connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier
                        .get(), outOfBandStatistics, networkInstanceUtils);
    }

}
