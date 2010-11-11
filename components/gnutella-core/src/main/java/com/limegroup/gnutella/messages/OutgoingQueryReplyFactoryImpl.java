package com.limegroup.gnutella.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.limewire.core.api.network.BandwidthCollector;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.core.settings.UploadSettings;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecurityToken;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLUtils;

@Singleton
public class OutgoingQueryReplyFactoryImpl implements OutgoingQueryReplyFactory {

    private final QueryReplyFactory queryReplyFactory;
    private final UploadManager uploadManager;
    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;
    private final ConnectionManager connectionManager;
    private final BandwidthCollector bandwidthCollector;

    @Inject
    public OutgoingQueryReplyFactoryImpl(QueryReplyFactory queryReplyFactory, 
            UploadManager uploadManager, NetworkManager networkManager,
            ApplicationServices applicationServices, ConnectionManager connectionManager, BandwidthCollector bandwidthCollector) {
        this.queryReplyFactory = queryReplyFactory;
        this.uploadManager = uploadManager;
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.connectionManager = connectionManager;
        this.bandwidthCollector = bandwidthCollector;
    }
    
    public List<QueryReply> createReplies(Response[] responses, QueryRequest queryRequest,
            SecurityToken securityToken, int responsesPerReply) {
        // We only want to return a "reply to multicast query" QueryReply
        // if the request travelled a single hop.
        boolean isMulticast = queryRequest.isMulticast() && (queryRequest.getTTL() + queryRequest.getHops()) == 1;
        byte ttl = isMulticast ? 1 : (byte)(queryRequest.getHops() + 1); 
        return createReplies(responses, responsesPerReply, securityToken, queryRequest.getGUID(),
                ttl, isMulticast, queryRequest.canDoFirewalledTransfer());
    }

    public List<QueryReply> createReplies(Response[] responses, int responsesPerReply,
            SecurityToken securityToken, byte[] guid, byte ttl, boolean isMulticast,
            boolean requestorCanDoFWT) {
        List<Response[]> splitResponses = splitResponses(responses, responsesPerReply);
        List<QueryReply> replies = new ArrayList<QueryReply>();
        for (Response[] bundle : splitResponses) {
            replies.addAll(createReplies(bundle, securityToken, guid, ttl, isMulticast, requestorCanDoFWT));
        }
        return replies;
    }
    
    static List<Response[]> splitResponses(Response[] responses, int responsesPerReply) {
        if (responses.length <= responsesPerReply) {
            return Collections.singletonList(responses);
        }
        List<Response[]> results = new ArrayList<Response[]>();
        for (int i = 0; i < responses.length; i += responsesPerReply) {
            Response[] copy = new Response[Math.min(responsesPerReply, responses.length - i)];
            System.arraycopy(responses, i, copy, 0, copy.length);
            results.add(copy);
        }
        return results;
    }
    
    /**
     * @param securityToken might be null, otherwise must be sent in GGEP
     * of QHD with header "SO"
     */
    public List<QueryReply> createReplies(Response[] responses,
            SecurityToken securityToken, byte[] guid, byte ttl, boolean isMulticast,
            boolean requestorCanDoFWT) {
        
        if (responses.length == 0) {
            return Collections.emptyList();
        }
        
        // We should mark our hits if the remote end can do a firewalled
        // transfer AND so can we AND we don't accept tcp incoming AND our
        // external address is valid (needed for input into the reply)
        boolean isFWTransfer = requestorCanDoFWT && networkManager.canDoFWT() && !networkManager.acceptedIncomingConnection();
        
        // see if there are any open slots
        // Note: if we are busy, non-metafile results would be filtered.
        // by this point.
        boolean isBusy = !uploadManager.mayBeServiceable();
        boolean hasUploaded = uploadManager.hadSuccesfulUpload();
        byte[] clientGUID = applicationServices.getMyGUID();

        long speed = uploadManager.measuredUploadSpeed();
        boolean measuredSpeed = true;
        
        if (speed == -1) {
            //measured speed in kilobits
            speed = bandwidthCollector.getMaxMeasuredTotalUploadBandwidth() * 8;
            if(speed <= 0) {
                //default to cable speed if no measurement have been done yet.
                //assume larger than modem to get better measurement stats.
                speed = SpeedConstants.CABLE_SPEED_INT;
            }
            measuredSpeed = false;
        }
        
        //max upload speed in kilobits
        int maxUploadSpeed = UploadSettings.MAX_UPLOAD_SPEED.getValue() / 1024 * 8;
        if(UploadSettings.LIMIT_MAX_UPLOAD_SPEED.getValue() && speed > maxUploadSpeed) {
            speed = maxUploadSpeed;
        }
        
        List<QueryReply> queryReplies = new ArrayList<QueryReply>();
        
        // pick the right address & port depending on multicast & fwtrans
        // if we cannot find a valid address & port, exit early.
        int port = -1;
        byte[] ip = null;
        // first try using multicast addresses & ports, but if they're
        // invalid, fallback to non multicast.
        if(isMulticast) {
            ip = networkManager.getNonForcedAddress();
            port = networkManager.getNonForcedPort();
            if(!NetworkUtils.isValidPort(port) || !NetworkUtils.isValidAddress(ip))
                isMulticast = false;
        }
        
        if(!isMulticast) {
            // see if we have a valid FWTrans address.  if not, fall back.
            if(isFWTransfer) {
                port = networkManager.getStableUDPPort();
                ip = networkManager.getExternalAddress();
                if(!NetworkUtils.isValidAddress(ip) 
                        || !NetworkUtils.isValidPort(port))
                    isFWTransfer = false;
            }
            
            // if we still don't have a valid address here, exit early.
            if(!isFWTransfer) {
                ip = networkManager.getAddress();
                port = networkManager.getPort();
                if(!NetworkUtils.isValidAddress(ip) ||
                        !NetworkUtils.isValidPort(port))
                    return Collections.emptyList();
            }
        }
        
        // get the *latest* push proxies if we have not accepted an incoming
        // connection in this session
        boolean notIncoming = !networkManager.acceptedIncomingConnection();
        Set<? extends IpPort> proxies = notIncoming ? connectionManager.getPushProxies() : null;
        
        // if sending a single response, see if xml bytes have been constructed before already
        if (responses.length == 1) {
            byte[] compressedXmlBytes = responses[0].getCompressedXmlBytes();
            if (compressedXmlBytes != null) {
                // create the new queryReply
                QueryReply queryReply = queryReplyFactory.createQueryReply(guid, ttl,
                        port, ip, speed, responses, clientGUID,
                        compressedXmlBytes, notIncoming, isBusy, hasUploaded, measuredSpeed,
                        false /* chat */, isMulticast, isFWTransfer, proxies, securityToken);
                queryReplies.add(queryReply);
                return queryReplies;
            }
        }

        byte[] xmlBytes = createXmlBytes(responses);
                
        // it may be too big....
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {
            // ok, need to partition responses up once again and send out
            // multiple query replies.....
            List<Response[]> splitResps = new ArrayList<Response[]>(2);
            splitAndAddResponses(splitResps, responses);

            while (!splitResps.isEmpty()) {
                Response[] currResps = splitResps.remove(0);
                byte[] currXMLBytes = createXmlBytes(currResps);
                if ((currXMLBytes.length > QueryReply.XML_MAX_SIZE) &&
                                                        (currResps.length > 1)) 
                    splitAndAddResponses(splitResps, currResps);
                else {
                    // create xml bytes if possible...
                    byte[] xmlCompressed = LimeXMLUtils.compress(currXMLBytes);
                    
                    // create the new queryReply
                    QueryReply queryReply = queryReplyFactory.createQueryReply(guid, ttl,
                            port, ip, speed, currResps, clientGUID,
                            xmlCompressed, notIncoming, isBusy, hasUploaded, measuredSpeed,
                            false /* chat */, isMulticast, isFWTransfer, proxies, securityToken);
                    queryReplies.add(queryReply);
                }
            }

        }
        else {  // xml is small enough, no problem.....
            byte[] xmlCompressed = LimeXMLUtils.compress(xmlBytes);
            // create the new queryReply
            QueryReply queryReply = queryReplyFactory.createQueryReply(guid, ttl, port,
                    ip, speed, responses, clientGUID, xmlCompressed, notIncoming,
                    isBusy, hasUploaded, measuredSpeed, false /* chat */, isMulticast, isFWTransfer,
                    proxies, securityToken);
            queryReplies.add(queryReply);
        }

        return queryReplies;
    }
    
    /** @return Simply splits the input array into two (almost) equally sized
     *  arrays.
     */
    private static Response[][] splitResponses(Response[] in) {
        int middle = in.length/2;
        Response[][] retResps = new Response[2][];
        retResps[0] = new Response[middle];
        retResps[1] = new Response[in.length-middle];
        for (int i = 0; i < middle; i++)
            retResps[0][i] = in[i];
        for (int i = 0; i < (in.length-middle); i++)
            retResps[1][i] = in[i+middle];
        return retResps;
    }

    private static void splitAndAddResponses(List<Response[]> addTo, Response[] toSplit) {
        Response[][] splits = splitResponses(toSplit);
        addTo.add(splits[0]);
        addTo.add(splits[1]);
    }

    private byte[] createXmlBytes(Response...responses) {
        String xmlString = LimeXMLDocumentHelper.getAggregateString(responses);
        if (xmlString.isEmpty()) {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
        return StringUtils.toUTF8Bytes(xmlString);
    }
    
    @Override
    public byte[] getCompressedXmlBytes(Response response) {
        byte[] compressedXmlBytes = response.getCompressedXmlBytes();
        if (compressedXmlBytes != null) {
            return compressedXmlBytes;
        }
        byte[] xmlBytes = createXmlBytes(response);
        if (xmlBytes.length > QueryReply.XML_MAX_SIZE) {
            response.setCompressedXmlBytes(DataUtils.EMPTY_BYTE_ARRAY);
            return DataUtils.EMPTY_BYTE_ARRAY;
        } else {
            xmlBytes = LimeXMLUtils.compress(xmlBytes);
            response.setCompressedXmlBytes(xmlBytes);
            return xmlBytes; 
        }
    }
        
}
