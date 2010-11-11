package com.limegroup.gnutella;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This is the class that goes in the route table when a request is
 * sent whose reply is for me.
 */
@EagerSingleton
public class ForMeReplyHandler implements ReplyHandler, SecureMessageCallback {
    
    private static final Log LOG = LogFactory.getLog(ForMeReplyHandler.class);
    
    /**
     * Keeps track of what hosts have sent us PushRequests lately.
     */
    private final Map<String, AtomicInteger> PUSH_REQUESTS = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap<String, AtomicInteger>(200));

    private final Map<GUID, GUID> GUID_REQUESTS = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap<GUID, GUID>(200));
    
    private final NetworkManager networkManager;
    private final SecureMessageVerifier secureMessageVerifier;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<SearchResultHandler> searchResultHandler;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<PushManager> pushManager;
    private final ApplicationServices applicationServices;
    private final ConnectionServices connectionServices;
    private final LimeXMLDocumentHelper limeXMLDocumentHelper;
    private final Provider<IPFilter> ipFilterProvider;
    private final SpamServices spamServices;

    @Inject
    ForMeReplyHandler(NetworkManager networkManager,
            SecureMessageVerifier secureMessageVerifier,
            Provider<ConnectionManager> connectionManager,
            Provider<SearchResultHandler> searchResultHandler,
            Provider<DownloadManager> downloadManager,
            Provider<Acceptor> acceptor, Provider<PushManager> pushManager,
            ApplicationServices applicationServices,
            ConnectionServices connectionServices,
            LimeXMLDocumentHelper limeXMLDocumentHelper,
            Provider<IPFilter> ipFilterProvider,
            SpamServices spamServices) {
        this.networkManager = networkManager;
        this.secureMessageVerifier = secureMessageVerifier;
        this.connectionManager = connectionManager;
        this.searchResultHandler = searchResultHandler;
        this.downloadManager = downloadManager;
        this.pushManager = pushManager;
        this.applicationServices = applicationServices;
        this.connectionServices = connectionServices;
        this.limeXMLDocumentHelper = limeXMLDocumentHelper;
        this.ipFilterProvider = ipFilterProvider;
        this.spamServices = spamServices;
    }
    
    @Inject
    public void register(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, ServiceScheduler serviceScheduler) {
        //Clear push requests every 30 seconds.
        Runnable clearPushRequests = new Runnable() {
            public void run() {
                PUSH_REQUESTS.clear();
            }
        };
        
        serviceScheduler.scheduleWithFixedDelay("ForMeReplyHandler.Clear Push Requests", clearPushRequests, 30, 30, TimeUnit.SECONDS, backgroundExecutor);
    }

	public void handlePingReply(PingReply pingReply, ReplyHandler handler) {
        //Kill incoming connections that don't share.  Note that we randomly
        //allow some freeloaders.  (Hopefully they'll get some stuff and then
        //share!)  Note that we only consider killing them on the first ping.
        //(Message 1 is their ping, message 2 is their reply to our ping.)
        if ((pingReply.getHops() <= 1)
			&& (handler.getNumMessagesReceived() <= 2)
			&& (!handler.isOutgoing())
			&& (handler.isKillable())
			&& (pingReply.getFiles() < SharingSettings.FREELOADER_FILES.getValue())
			&& ((int)(Math.random()*100.f) >
				SharingSettings.FREELOADER_ALLOWED.getValue())
			&& (handler instanceof RoutedConnection)
            && (handler.isStable())) {
            connectionManager.get().remove((RoutedConnection)handler);
        }
	}
	
	public void handleQueryReply(QueryReply reply, ReplyHandler handler) {
	    handleQueryReply(reply, handler, null);
	}
	
	/**
	 * Handles a query reply locally.
	 * 
	 * @param address can be null, if not null overrides the address info in <code>reply</code>
	 */
	public void handleQueryReply(QueryReply reply, ReplyHandler handler, Address address) {
        // do not allow a faked multicast reply.
        if(reply.isFakeMulticast()) {
            LOG.trace("Dropping fake multicast reply");
            return;
        }
        
		// Drop if it's a reply to mcast and conditions aren't met ...
        if(reply.isReplyToMulticastQuery()) {
            if(reply.isTCP()) {
                LOG.trace("Dropping TCP reply to multicast query");
                return; // shouldn't be on TCP.
            }
            if(reply.getHops() != 1 || reply.getTTL() != 0) {
                LOG.trace("Dropping multi-hop reply to multicast query");
                return; // should only have hopped once.
            }
        }
        
        // XML must be added to the response first, so that
        // whomever calls toRemoteFileDesc on the response
        // will create the cachedRFD with the correct XML.
        boolean validResponses = addXMLToResponses(reply, limeXMLDocumentHelper);
        // responses invalid?  exit.
        if(!validResponses) {
            LOG.trace("Dropping reply without valid responses");
            return;
        }

        // check for unwanted results after xml has been constructed
        if(spamServices.isPersonalSpam(reply)) {
            LOG.trace("Dropping spam reply");
            return;
        }
        
        if(reply.hasSecureData() && ApplicationSettings.USE_SECURE_RESULTS.getValue()) {
            LOG.trace("Verifying secure reply");
            secureMessageVerifier.verify(reply, this);
        } else {
            LOG.trace("Reply looks OK, routing it internally");
            routeQueryReplyInternal(reply, address);
        }
    }
    
    /** Notification that a message is secure.  Currently only possible for a QueryReply. */
    public void handleSecureMessage(SecureMessage sm, boolean passed) {
        if (passed)
            routeQueryReplyInternal((QueryReply) sm, null);
    }
    
    /** Passes the QueryReply off to where it should go. */
    private void routeQueryReplyInternal(QueryReply reply, Address address) {
        searchResultHandler.get().handleQueryReply(reply, address);
        downloadManager.get().handleQueryReply(reply, address);
    }
	
	/**
	 * Adds XML to the responses in a QueryReply.
	 */
    public static boolean addXMLToResponses(QueryReply qr, LimeXMLDocumentHelper limeXMLDocumentHelper) {
        // get xml collection string, then get dis-aggregated docs, then 
        // in loop
        // you can match up metadata to responses
        String xmlCollectionString = "";
        try {
            LOG.trace("Trying to do uncompress XML.....");
            byte[] xmlCompressed = qr.getXMLBytes();
            if (xmlCompressed.length > 1) {
                byte[] xmlUncompressed = LimeXMLUtils.uncompress(xmlCompressed);
                xmlCollectionString = new String(xmlUncompressed,"UTF-8");
            }
        } catch (UnsupportedEncodingException use) {
            //b/c this should never happen, we will show and error
            //if it ever does for some reason.
            //we won't throw a BadPacketException here but we will show it.
            //the uee will effect the xml part of the reply but we could
            //still show the reply so there shouldn't be any ill effect if
            //xmlCollectionString is ""
            ErrorService.error(use);
        } catch (IOException ignored) {
        }
        
        // valid response, no XML in EQHD.
        if(xmlCollectionString.equals(""))
            return true;
        
        Response[] responses;
        int responsesLength;
        try {
            responses = qr.getResultsArray();
            responsesLength = responses.length;
        } catch(BadPacketException bpe) {
            LOG.trace("Unable to get responses", bpe);
            return false;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug("xmlCollectionString = " + xmlCollectionString);

        List<LimeXMLDocument[]> allDocsArray = 
            limeXMLDocumentHelper.getDocuments(xmlCollectionString, 
                                               responsesLength);
        
        for(int i = 0; i < responsesLength; i++) {
            Response response = responses[i];
            LimeXMLDocument[] metaDocs;
            for(int schema = 0; schema < allDocsArray.size(); schema++) {
                metaDocs = allDocsArray.get(schema);
                // If there are no documents in this schema, try another.
                if(metaDocs == null)
                    continue;
                // If this schema had a document for this response, use it.
                if(metaDocs[i] != null) {
                    response.setDocument(metaDocs[i]);
                    break; // we only need one, so break out.
                }
            }
        }
        return true;
    }

    /**
     * If there are problems with the request, just ignore it.
     * There's no point in sending them a GIV to have them send a GET
     * just to return a 404 or Busy or Malformed Request, etc..
     */
	public void handlePushRequest(PushRequest pushRequest, ReplyHandler handler){
	    if (LOG.isDebugEnabled()) {
	        LOG.debug("push: " + pushRequest + "\nfrom: " + handler);
	    }
	    
        //Ignore push request from banned hosts.
        if (spamServices.isPersonalSpam(pushRequest)) {
            LOG.debug("discarded as personal spam");
            return;
        }
            
        byte[] ip = pushRequest.getIP();
        String host = NetworkUtils.ip2string(ip);

        // check whether we serviced this push request already
    	GUID guid = new GUID(pushRequest.getGUID());
    	if (GUID_REQUESTS.put(guid, guid) != null) {
    	    LOG.debug("already serviced");
    		return;
    	}

       // make sure the guy isn't hammering us
        AtomicInteger i = PUSH_REQUESTS.get(host);
        if(i == null) {
            i = new AtomicInteger(1);
            PUSH_REQUESTS.put(host, i);
        } else {
            i.addAndGet(1);
            // if we're over the max push requests for this host, exit.
            if(i.get() > UploadSettings.MAX_PUSHES_PER_HOST.getValue()) {
                LOG.debug("over max pushes per host");
                return;
            }
        }
        
        // if the IP is banned, don't accept it
        if (!ipFilterProvider.get().allow(ip)) {
            LOG.debug("blocked by ip filter");
            return;
        }

        int port = pushRequest.getPort();
        // if invalid port, exit
        if (!NetworkUtils.isValidAddressAndPort(host, port) ) {
            LOG.debug("invalid host or port");
            return;
        }
        
        
        try {
            Connectable address = new ConnectableImpl(host, port, pushRequest.isTLSCapable());
            pushManager.get().acceptPushUpload(address,
                    new GUID(pushRequest.getClientGUID()),
                    pushRequest.isMulticast(), // force accept
                    pushRequest.isFirewallTransferPush());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
	}
	
	public boolean isOpen() {
		//I'm always ready to handle replies.
		return true;
	}
	
	public int getNumMessagesReceived() {
		return 0;
	}
	
	
	public void countDroppedMessage() {}
	
	// inherit doc comment
	public boolean isSupernodeClientConnection() {
		return false;
	}
	
	public boolean isPersonalSpam(Message m) {
		return false;
	}
	
	public void updateHorizonStats(PingReply pingReply) {
        // TODO:: we should probably actually update the stats with this pong
    }
	
	public boolean isOutgoing() {
		return false;
	}
	

	// inherit doc comment
	public boolean isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt> interface.  Returns whether this
	 * node is a leaf or an Ultrapeer.
	 *
	 * @return <tt>true</tt> if this node is a leaf node, otherwise 
	 *  <tt>false</tt>
	 */
	public boolean isLeafConnection() {
		return !connectionServices.isSupernode();
	}

	/**
	 * Returns whether or not this connection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer connections.
	 * Because this connection really represents just this node, it always
	 * returns <tt>false</tt>/
	 *
	 * @return <tt>false</tt>, since this reply handler signifies only this
	 *  node -- its connections don't matter.
	 */
	public boolean isHighDegreeConnection() {
		return false;
	}	

    /**
     * Returns <tt>false</tt>, since this connection is me, and it's not
     * possible to pass query routing tables to oneself.
     *
     * @return <tt>false</tt>, since you cannot pass query routing tables
     *  to yourself
     */
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodUltrapeer() {
        return false;
    }

    /**
     * Returns <tt>false</tt>, as this node is not  a "connection"
     * in the first place, and so could never have sent the requisite
     * headers.
     *
     * @return <tt>false</tt>, as this node is not a real connection
     */
    public boolean isGoodLeaf() {
        return false;
    }

    /**
     * Returns <tt>true</tt>, since we always support pong caching.
     *
     * @return <tt>true</tt> since this node always supports pong 
     *  caching (since it's us)
     */
    public boolean supportsPongCaching() {
        return true;
    }

    /**
     * Returns whether or not to allow new pings from this <tt>ReplyHandler</tt>.
     * Since this ping is from us, we'll always allow it.
     *
     * @return <tt>true</tt> since this ping is from us
     */
    public boolean allowNewPings() {
        return true;
    }

    // inherit doc comment
    public InetAddress getInetAddress() {
        try {
            return InetAddress.
                getByName(NetworkUtils.ip2string(networkManager.getAddress()));
        } catch(UnknownHostException e) {
            // may want to do something else here if we ever use this!
            return null;
        }
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    public int getPort() {
        return networkManager.getPort();
    }
    
    public String getAddress() {
        return NetworkUtils.ip2string(networkManager.getAddress());
    }
    
    /**
     * Returns <tt>true</tt> to indicate that this node is always stable.
     * Simply the fact that this method is being called indicates that the
     * code is alive and stable (I think, therefore I am...).
     *
     * @return <tt>true</tt> since, this node is always stable
     */
    public boolean isStable() {
        return true;
    }

    public String getLocalePref() {
        return ApplicationSettings.LANGUAGE.get();
    }
    
    /**
     * drops the message
     */
    public void reply(Message m){}

    public byte[] getClientGUID() {
        return applicationServices.getMyGUID();
    }
}



