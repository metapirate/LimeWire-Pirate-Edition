package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.Tuple;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.SocketsManager;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QuerySettings;
import com.limegroup.gnutella.search.SearchResultHandler;

/**
 * This class is the message routing implementation for TCP messages.
 */
@EagerSingleton
public class StandardMessageRouter extends MessageRouterImpl {

    private static final Log LOG = LogFactory.getLog(StandardMessageRouter.class);

    /**
     * Maximum number of bytes we're sending in a single udp packet to avoid
     * ip fragmentation.
     */
    static final int UDP_MTU = 1400;
    /**
     * Gnutella message header size.
     */
    static final int GNUTELLA_HEADER_SIZE = 23;

    private final Statistics statistics;
    private final ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;
    private final SharedFilesKeywordIndex sharedFilesKeywordIndex;

    private OutgoingQueryReplyFactory outgoingQueryReplyFactory;

    @Inject
    public StandardMessageRouter(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            QueryHandlerFactory queryHandlerFactory,
            OnDemandUnicaster onDemandUnicaster,
            HeadPongFactory headPongFactory,
            PingReplyFactory pingReplyFactory,
            ConnectionManager connectionManager,
            @Named("forMeReplyHandler") ReplyHandler forMeReplyHandler,
            QueryUnicaster queryUnicaster,
            FileViewManager fileManager,
            DHTManager dhtManager,
            UploadManager uploadManager,
            DownloadManager downloadManager,
            UDPService udpService,
            Provider<SearchResultHandler> searchResultHandler,
            SocketsManager socketsManager,
            HostCatcher hostCatcher,
            QueryReplyFactory queryReplyFactory,
            StaticMessages staticMessages,
            Provider<MessageDispatcher> messageDispatcher,
            MulticastService multicastService,
            QueryDispatcher queryDispatcher,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            ApplicationServices applicationServices,
            @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            Provider<PongCacher> pongCacher,
            GuidMapManager guidMapManager, 
            UDPReplyHandlerCache udpReplyHandlerCache,
            Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
            Statistics statistics,
            ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
            PingRequestFactory pingRequestFactory,
            MessageHandlerBinder messageHandlerBinder,
            Provider<OOBHandler> oobHandlerFactory,
            Provider<MACCalculatorRepositoryManager> MACCalculatorRepositoryManager,
            Provider<LimeACKHandler> limeACKHandler,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            SharedFilesKeywordIndex sharedFilesKeywordIndex,
            QRPUpdater qrpUpdater,
            URNFilter urnFilter,
            SpamServices spamServices,
            QuerySettings querySettings) {
        super(networkManager, queryRequestFactory, queryHandlerFactory,
                onDemandUnicaster, headPongFactory, pingReplyFactory,
                connectionManager, forMeReplyHandler, queryUnicaster,
                fileManager, dhtManager, uploadManager, downloadManager,
                udpService, searchResultHandler, socketsManager, hostCatcher,
                queryReplyFactory, staticMessages, messageDispatcher,
                multicastService, queryDispatcher, activityCallback,
                connectionServices, applicationServices, backgroundExecutor,
                pongCacher, guidMapManager, udpReplyHandlerCache,
                udpCrawlerPingHandlerFactory, pingRequestFactory,
                messageHandlerBinder, oobHandlerFactory,
                MACCalculatorRepositoryManager, limeACKHandler,
                outgoingQueryReplyFactory, qrpUpdater, urnFilter, spamServices,
                querySettings);
        this.statistics = statistics;
        this.replyNumberVendorMessageFactory = replyNumberVendorMessageFactory;
        this.sharedFilesKeywordIndex = sharedFilesKeywordIndex;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
    }

    /**
     * Responds to a Gnutella ping with cached pongs. This does special handling
     * for both "heartbeat" pings that were sent to ensure that the connection
     * is still live as well as for pings from a crawler.
     * 
     * @param ping the <tt>PingRequest</tt> to respond to
     * @param handler the <tt>ReplyHandler</tt> to send any pongs to
     */
    @Override
    protected void respondToPingRequest(PingRequest ping,
            ReplyHandler handler) {
        // The ping has already been hopped
        byte hops = ping.getHops();
        byte ttl = ping.getTTL();
        // If hops + ttl > 2 this is not a heartbeat or a crawler ping. Check 
        // if we can accept an incoming connection for old-style unrouted
        // connections, ultrapeers, or leaves.
        if(hops + ttl > 2 && !connectionManager.allowAnyConnection()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Not responding to ordinary ping (1) " + ping);
            return;
        }

        // Only send pongs for ourself if we have a valid address & port.
        if(networkManager.isIpPortValid()) {    
            // If hops == 1 and ttl == 1 this is a crawler ping. We don't send
            // our own pong since the crawler already knows our address, but
            // we send the addresses of our leaves.
            if(hops == 1 && ttl == 1) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Responding to crawler ping " + ping);
                handleCrawlerPing(ping, handler);
                return;
            }

            // If hops == 1 and ttl == 0 this is a heartbeat ping. Bypass
            // the pong caching code and reply. TODO: why does this require a
            // valid address and port? Don't we want to respond to heartbeat
            // pings on LAN connections?
            if(ping.isHeartbeat()) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Responding to heartbeat ping " + ping);
                sendPingReply(pingReplyFactory.create(ping.getGUID(), (byte)1), 
                        handler);
                return;
            }

            // TODO: why would hops + ttl be less than 3 at this point? We've
            // already dealt with crawler and heartbeat pings. And why is the
            // ttl of the pong greater than the hop count of the ping?
            int newTTL = hops+1;
            if ( (hops+ttl) <=2)
                newTTL = 1;        

            // send our own pong if we have free slots or if our average
            // daily uptime is more than 1/2 hour
            if(connectionManager.hasFreeSlots()  ||
                    statistics.calculateDailyUptime() > 60*30) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Responding to ordinary ping " + ping);
                PingReply pr = 
                    pingReplyFactory.create(ping.getGUID(), (byte)newTTL);

                sendPingReply(pr, handler);
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug("Not responding to ordinary ping (2) " + ping);
            }
        }

        List<PingReply> pongs = pongCacher.get().getBestPongs(ping.getLocale());
        byte[] guid = ping.getGUID();
        InetAddress pingerIP = handler.getInetAddress();
        for(PingReply pr : pongs) {
            if(pr.getInetAddress().equals(pingerIP))
                continue;
            sendPingReply(pingReplyFactory.mutateGUID(pr, guid), handler);
        }
    }

    /**
     * Responds to a ping request received over a UDP port.  This is
     * handled differently from all other ping requests.  Instead of
     * responding with cached pongs, we respond with a pong from our node.
     *
     * @param request the <tt>PingRequest</tt> to service
     * @param addr the <tt>InetSocketAddress</tt> containing the IP
     *  and port of the client node
     * @param handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
     */
    @Override
    protected void respondToUDPPingRequest(PingRequest request, 
            InetSocketAddress addr,
            ReplyHandler handler) {
        if(!networkManager.isIpPortValid())
            return;

        List<IpPort> dhthosts = Collections.emptyList();
        int maxHosts = ConnectionSettings.NUM_RETURN_PONGS.getValue();

        if (request.requestsDHTIPP() && dhtManager.isRunning()) {
            dhthosts = dhtManager.getActiveDHTNodes(maxHosts);
        }

        int numDHTHosts = dhthosts.size();

        byte[] data = request.getSupportsCachedPongData();
        Collection<IpPort> gnuthosts = Collections.emptyList();
        if(data != null){
            boolean isUltrapeer =
                data.length >= 1 && 
                (data[0] & PingRequest.SCP_ULTRAPEER_OR_LEAF_MASK) ==
                    PingRequest.SCP_ULTRAPEER;

                int dhtFraction = ConnectionSettings.DHT_TO_GNUT_HOSTS_PONG.getValue();
                int maxDHTHosts = Math.round(((float)dhtFraction/100)*maxHosts);

                gnuthosts = connectionServices.getPreferencedHosts(
                        isUltrapeer, 
                        request.getLocale(),
                        maxHosts - Math.min(numDHTHosts, maxDHTHosts));
                //remove extra dht hosts
                dhthosts = dhthosts.subList(0, Math.min(numDHTHosts,maxHosts - gnuthosts.size()));
        } 

        PingReply reply;
        if (request.requestsIP())
            reply = pingReplyFactory.create(request.getGUID(), (byte)1, new IpPortImpl(addr), gnuthosts, dhthosts);
        else
            reply = pingReplyFactory.create(request.getGUID(), (byte)1, gnuthosts, dhthosts);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Responding to UDPPingRequest "+(request.requestsDHTIPP()?"with DHTIPP ":"") +
                    "from : "+ addr + " with Gnutella hosts: \n"+ gnuthosts 
                    + "\n and DHT hosts: \n" + dhthosts);
        }

        sendPingReply(reply, handler);

    }

    /**
     * Responds to a crawler ping with hops 0 and ttl 2 (before hopping) by
     * sending a pong for each leaf. Ultrapeer neighbours will send their own
     * pongs when the ping is forwarded to them. TODO: where is the ping
     * forwarded to them?
     * @param ping the ping request received
     * @param handler the <tt>ReplyHandler</tt> that should handle any
     *  replies
     */
    private void handleCrawlerPing(PingRequest ping, ReplyHandler handler) {
        //send the pongs for leaves
        List<RoutedConnection> leafConnections =
            connectionManager.getInitializedClientConnections();
        for(RoutedConnection connection : leafConnections) {
            //create the pong for this connection
            PingReply pr = 
                pingReplyFactory.createExternal(ping.getGUID(), (byte)2, 
                        connection.getPort(),
                        connection.getInetAddress().getAddress(),
                        false);
            //hop the message, as it is ideally coming from the connected host
            pr.hop();

            sendPingReply(pr, handler);
        }
    }

    @Override
    protected boolean respondToQueryRequest(QueryRequest queryRequest,
            byte[] clientGUID,
            ReplyHandler handler) {
        //Only respond if we understand the actual feature, if it had a feature.
        if(!FeatureSearchData.supportsFeature(queryRequest.getFeatureSelector()))
            return false;

        // Only send results if we're not busy.  Note that this ignores
        // queue slots -- we're considered busy if all of our "normal"
        // slots are full.  This allows some spillover into our queue that
        // is necessary because we're always returning more total hits than
        // we have slots available.
        if(!uploadManager.mayBeServiceable())
            return false;

        // Ensure that we have a valid IP & Port before we send the response.
        // Otherwise the QueryReply will fail on creation.
        if(!networkManager.isIpPortValid())
            return false;

        // Run the local query
        Response[] responses = sharedFilesKeywordIndex.query(queryRequest);
        return sendResponses(responses, queryRequest, handler);        
    }

    boolean sendResponses(Response[] responses, QueryRequest query,
            ReplyHandler handler) {
        // if either there are no responses or, the
        // response array came back null for some reason,
        // exit this method
        if ((responses == null) || (responses.length < 1))
            return false;

        if (!uploadManager.isServiceable())
            return false;

        // Here we can do a couple of things - if the query wants
        // out-of-band replies we should do things differently.  else just
        // send it off as usual.  only send out-of-band if you can
        // receive solicited udp AND not servicing too many
        // uploads AND not connected to the originator of the query
        if (query.desiresOutOfBandReplies() &&
                !isConnectedTo(query, handler) && 
                networkManager.canReceiveSolicited() &&
                NetworkUtils.isValidAddressAndPort(query.getReplyAddress(), query.getReplyPort())) {

            Tuple<Response[], List<QueryReply>> split = splitLargeResponses(query, responses);
            responses = split.getFirst();
            List<QueryReply> largeQueryReplies = split.getSecond();

            if (responses.length == 0) {
                assert !largeQueryReplies.isEmpty();
                sendQueryReplies(largeQueryReplies);
                return true;
            }

            // send the replies out-of-band - we need to
            // 1) buffer the responses
            // 2) send a ReplyNumberVM with the number of responses
            if (limeAckHandler.get().bufferResponsesForLaterDelivery(query, responses)) {
                // special out of band handling....
                InetAddress addr = null;
                try {
                    addr = InetAddress.getByName(query.getReplyAddress());
                } catch (UnknownHostException uhe) {}
                final int port = query.getReplyPort();

                if(addr != null) { 
                    // send a ReplyNumberVM to the host - he'll ACK you if he
                    // wants the whole shebang
                    int resultCount = 
                        (responses.length > 255) ? 255 : responses.length;
                    final ReplyNumberVendorMessage vm = query.desiresOutOfBandRepliesV3() ?
                            replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount) :
                                replyNumberVendorMessageFactory.createV2ReplyNumberVendorMessage(new GUID(query.getGUID()), resultCount);
                            udpService.send(vm, addr, port);
                            if (MessageSettings.OOB_REDUNDANCY.getValue() && 
                                    query.desiresOutOfBandRepliesV3()) {
                                final InetAddress addrf = addr;
                                backgroundExecutor.schedule(new Runnable() {
                                    public void run () {
                                        udpService.send(vm, addrf, port);
                                    }
                                }, 100, TimeUnit.MILLISECONDS);
                            }
                            if (!largeQueryReplies.isEmpty()) {
                                sendQueryReplies(largeQueryReplies);
                            }
                            return true;
                }
            } else {
                // else i couldn't buffer the responses due to busy-ness, oh, scrap
                // them.....
                return false;
            }
        }

        // send the replies in-band
        // -----------------------------

        //convert responses to QueryReplies
        Iterable<QueryReply> iterable = responsesToQueryReplies(responses,
                query);
        //send the query replies
        sendQueryReplies(iterable);
        // -----------------------------

        return true;

    }

    private void sendQueryReplies(Iterable<QueryReply> queryReplies) {
        try {
            for(QueryReply queryReply : queryReplies)
                sendQueryReply(queryReply);
        }  catch (IOException e) {
            LOG.debug("error sending", e);
            // if there is an error, do nothing..
        }
    }

    /**
     * Splits reponses into a tuple of small responses that can be sent later 
     * in an out-of-band fashion and into large single response query replies
     * whose payload including Gnutella message header is too long to be sent
     * in single udp packet without risking ip fragmentation. 
     */
    Tuple<Response[], List<QueryReply>> splitLargeResponses(QueryRequest queryRequest, Response...responses) {
        List<Response> smallResponses = null;
        List<QueryReply> largeResponses = null;
        for (int i = 0; i < responses.length; i++) {
            Response response = responses[i];
            if (couldFragment(response)) {
                LOG.debugf("could fragment: {0}", response);
                QueryReply queryReply = createSingleResponseQueryReply(response, queryRequest);
                if (fragments(queryReply)) {
                    LOG.debugf("fragments: {0}", queryReply);
                    if (largeResponses == null) {
                        smallResponses = new ArrayList<Response>(i);
                        for (int j = 0; j < i; j++) {
                            smallResponses.add(responses[j]);
                        }
                        largeResponses = new ArrayList<QueryReply>(4);
                    }
                    largeResponses.add(queryReply);
                } else if (smallResponses != null) {
                    LOG.debugf("false positive {0}", queryReply);
                    smallResponses.add(response);
                } else {
                    LOG.debugf("false positive {0}", queryReply);
                }
            } else if (smallResponses != null) {
                smallResponses.add(response);
            }
        }
        if (smallResponses != null) {
            return new Tuple<Response[], List<QueryReply>>(smallResponses.toArray(new Response[smallResponses.size()]), largeResponses); 
        } else {
            return new Tuple<Response[], List<QueryReply>>(responses, Collections.<QueryReply>emptyList());
        }
    }

    /**
     * @return true if the response is large enough to be potentially ip fragmented
     */
    boolean couldFragment(Response response) {
        byte[] compressedXmlBytes = outgoingQueryReplyFactory.getCompressedXmlBytes(response);
        int queryReplySize = response.getWireSize() + compressedXmlBytes.length + 200; // 200: approximate size of data in QueryReplyImpl
        if (queryReplySize >= UDP_MTU) {
            return true;
        }
        return false;
    }

    /**
     * @return true if query reply gets ip fragmented
     */
    boolean fragments(QueryReply queryReply) {
        return queryReply.getPayload().length + GNUTELLA_HEADER_SIZE >= UDP_MTU;
    }

    /**
     * @return a query reply with a single response
     */
    QueryReply createSingleResponseQueryReply(Response response, QueryRequest queryRequest) {
        Iterable<QueryReply> queryReplies = responsesToQueryReplies(new Response[] { response }, queryRequest, 1, null);
        return queryReplies.iterator().next();
    }

    /** Returns whether or not we are connected to the originator of this query.
     *  PRE: assumes query.desiresOutOfBandReplies == true
     */
    private final boolean isConnectedTo(QueryRequest query, 
            ReplyHandler handler) {
        return query.matchesReplyAddress(handler.getInetAddress().getAddress());
    }
}