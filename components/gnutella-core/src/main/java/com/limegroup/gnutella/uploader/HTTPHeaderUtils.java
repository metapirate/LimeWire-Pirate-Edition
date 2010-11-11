package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.AltLocTracker;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;

/**
 * Provides methods to add commonly used headers to {@link HttpResponse}s.
 */
@Singleton
public class HTTPHeaderUtils {

    private final NetworkManager networkManager;
    private final FeaturesWriter featuresWriter;
    private final Provider<ConnectionManager> connectionManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    
    @Inject
    public HTTPHeaderUtils(FeaturesWriter featuresWriter, NetworkManager networkManager,
            Provider<ConnectionManager> connectionManager, NetworkInstanceUtils networkInstanceUtils) {
        this.networkManager = networkManager;
        this.featuresWriter = featuresWriter;
        this.connectionManager = connectionManager;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /**
     * Adds the <code>X-Available-Ranges</code> header to
     * <code>response</code> if available.
     */
    public void addRangeHeader(HttpResponse response,
            HTTPUploader uploader, FileDesc fd) {
        if (fd instanceof IncompleteFileDesc) {
            URN sha1 = uploader.getFileDesc().getSHA1Urn();
            if (sha1 != null) {
                IncompleteFileDesc ifd = (IncompleteFileDesc) fd;
                response.addHeader(HTTPHeaderName.AVAILABLE_RANGES.create(ifd));
            }
        }
    }
    
    /**
     * Encodes a collection of push proxies as an HTTP value string. Checks if the
     * push proxies implement {@link Connectable} and encodes their TLS capabilities. 
     * 
     * @param proxies collection of push proxies
     * @param separator separator between individual proxy entries
     * @param max the maximum number of push proxies to encode, the first <code>max</code>
     * push proxies will be encoded
     * 
     * @throws IllegalArgumentException if collection is empty
     */
    public static String encodePushProxies(Collection<? extends IpPort> proxies, String separator, int max) {
        if (proxies.isEmpty()) {
            throw new IllegalArgumentException("Can't encode empty set of proxies");
        }
        StringBuilder buf = new StringBuilder();
        int proxiesWritten = 0;
        BitNumbers bn = getTLSIndices(proxies, max);
        for(IpPort current : proxies) {
            if(proxiesWritten >= max)
                break;
            buf.append(current.getAddress())
               .append(":")
               .append(current.getPort())
               .append(separator);

            proxiesWritten++;
        }
        
        if(!bn.isEmpty())
            buf.insert(0, PushEndpoint.PPTLS_HTTP + "=" + bn.toHexString() + separator);

        buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }
    
    /**
     * Decodes an http value string into a set of push proxies.
     * 
     * @param httpValue the http value to be decoded
     * @param separator the separator that was used for encoding it
     * 
     * @return an empty set if no pushproxies could not be decoded
     */
    public Set<Connectable> decodePushProxies(String httpValue, String separator) {
        Set<Connectable> newSet = new HashSet<Connectable>();
        StringTokenizer tok = new StringTokenizer(httpValue, separator);
        BitNumbers tlsProxies = null;
        while(tok.hasMoreTokens()) {
            String proxy = tok.nextToken().trim();
            // only read features when we haven't read proxies yet.
            if(newSet.size() == 0 && proxy.startsWith(PushEndpoint.PPTLS_HTTP)) {
                try {
                    String value = HTTPUtils.parseValue(proxy);
                    if(value != null) {
                        try {
                            tlsProxies = new BitNumbers(value);
                        } catch(IllegalArgumentException ignored) {}
                    }
                } catch(IOException invalid) {}
                continue;
            }
            
            boolean tlsCapable = tlsProxies != null && tlsProxies.isSet(newSet.size());
            try {
                Connectable ipp = NetworkUtils.parseIpPort(proxy, tlsCapable);
                if(!networkInstanceUtils.isPrivateAddress(ipp.getInetAddress()))
                    newSet.add(ipp);
            } catch(IOException ohWell){
                tlsProxies = null; // unset, since index may be off.
            }
        }
        return newSet;
    }

    /**
     * Returns a bit numbers object that encodes the indices of 
     * of {@link IpPort}s in <code>ipPorts</code> that support TLS. 
     */
    public static BitNumbers getTLSIndices(Collection<? extends IpPort> ipPorts) {
        return getTLSIndices(ipPorts, ipPorts.size());
    }
    
    /**
     * Returns a bit numbers object that encodes the indices of 
     * of {@link IpPort}s in <code>ipPorts</code> that support TLS.
     * 
     * @param max stop encoding indices after max elements have been seen
     */
    public static BitNumbers getTLSIndices(Collection<? extends IpPort> ipPorts, int max) {
        BitNumbers bn = new BitNumbers(max);
        int i = 0;
        for (IpPort ipp : ipPorts) {
            if (i >= max)
                break;
            if (ipp instanceof Connectable && ((Connectable) ipp).isTLSCapable())
                bn.set(i);
            i++;
        }
        return bn; 
    }
    
    /**
     * Encodes string of push proxies.
     * 
     * @return null when host is not firewalled.
     */
    private String encodePushProxies() {
        if (networkManager.acceptedIncomingConnection())
            return null;
        Set<? extends Connectable> proxies = connectionManager.get().getPushProxies();
        return !proxies.isEmpty() ? encodePushProxies(proxies, ",", PushEndpoint.MAX_PROXIES) : null;
    }

    /**
     * Returns the http headers used for firewalled transfers, include
     * push proxies and port for fw-fw transfers.
     * 
     * @return empty list if this host is not firewalled and there is no
     * need for push proxies.
     */
    public List<Header> getFirewalledHeaders() {
        String proxies = encodePushProxies();
        if (proxies != null) {
            Header proxiesHeader = HTTPHeaderName.PROXIES.create(proxies);
            // write out X-FWPORT if we support firewalled transfers, so the other side gets our port
            // for future fw-fw transfers
            if (networkManager.canDoFWT()) {
                return Arrays.asList(proxiesHeader, HTTPHeaderName.FWTPORT.create(networkManager.getStableUDPPort() + "")); 
            } else {
                return Arrays.asList(proxiesHeader);
            }
        }
        return Collections.emptyList();
    }
    
    /**
     * Writes out the X-Push-Proxies header as specified by section 4.2 of the
     * Push Proxy proposal, v. 0.7
     */
    public void addProxyHeader(HttpResponse response) {
        for (Header header : getFirewalledHeaders()) {
            response.addHeader(header);
        }
    }

    /**
     * Adds alternate locations for <code>fd</code> to <code>response</code>
     * if available.
     */
    public void addAltLocationsHeader(HttpResponse response, AltLocTracker altLocTracker, AltLocManager altLocManager) {
        response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(altLocTracker.getUrn()));
        Collection<DirectAltLoc> direct = altLocTracker.getNextSetOfAltsToSend(altLocManager);
        if (direct.size() > 0) {
            List<HTTPHeaderValue> ordered = new ArrayList<HTTPHeaderValue>(
                    direct.size());
            final BitNumbers bn = new BitNumbers(direct.size());
            for (DirectAltLoc al : direct) {
                IpPort ipp = al.getHost();
                if (ipp instanceof Connectable
                        && ((Connectable) ipp).isTLSCapable())
                    bn.set(ordered.size());
                ordered.add(al);
            }

            if (!bn.isEmpty()) {
                ordered.add(0, new HTTPHeaderValue() {
                    public String httpStringValue() {
                        return DirectAltLoc.TLS_IDX + bn.toHexString();
                    }
                });
            }

            response.addHeader(HTTPHeaderName.ALT_LOCATION
                    .create(new HTTPHeaderValueCollection(ordered)));
        }

        if (altLocTracker.wantsFAlts()) {
            Collection<PushAltLoc> pushes = altLocTracker.getNextSetOfPushAltsToSend(altLocManager);
            if (pushes.size() > 0) {
                response.addHeader(HTTPHeaderName.FALT_LOCATION
                        .create(new HTTPHeaderValueCollection(pushes)));
            }
        }
    }

//    public void addAltLocationsHeaders(HttpResponse response, HTTPUploader uploader, URN urn) {
//        response.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(urn));
//        Collection<? extends AlternateLocation> alts = uploader.getAltLocTracker().getNextSetOfAltsToSend();
//        if(alts.size() > 0) {
//            response.addHeader(HTTPHeaderName.ALT_LOCATION.create(new HTTPHeaderValueCollection(alts)));
//        }
//
//        if (uploader.getAltLocTracker().wantsFAlts) {
//            alts = getNextSetOfPushAltsToSend();
//            if (alts.size() > 0) {
//                response.addHeader(HTTPHeaderName.FALT_LOCATION.create(new HTTPHeaderValueCollection(alts)));
//            }
//        }
//    }

    /**
     * Adds an <code>X-Features</code> header to <code>response</code>.
     */
    public void addFeatures(HttpResponse response) {
        Set<HTTPHeaderValue> features = featuresWriter.getFeaturesValue();
        if (features.size() > 0) {
            response.addHeader(HTTPHeaderName.FEATURES.create(
                    new HTTPHeaderValueCollection(features)));
        }
    }

    

}
