package com.limegroup.bittorrent;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentTrackerScraper;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.core.settings.SearchSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Reimplementation of libtorrents scrape code in java that detaches if 
 *  from the torrent manager control logic.  This class does not perform any scheduling.
 *  Each request will open a new socket.  For usage in batch jobs look to TorrentScrapeScheduler.
 *  
 * <p> Only supports HTTP scrape right now but UDP scrape is possible
 *      TODO: decouple udp_tracker_connection::send_udp_scrape()
 */
public class TorrentTrackerScraperImpl implements TorrentTrackerScraper {

    private static final Log LOG = LogFactory.getLog(TorrentTrackerScraperImpl.class);
    
    /**
     * Timeout before cancelling HTTP requests.
     */
    private static final int HTTP_TIMEOUT = 1500;
    
    /**
     *  Subset of the characters from escape_string.cpp in libtorrent that
     *   work in java.
     */
    private static final String UNRESERVED_CHARS =
        "-_.!~*(),"
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        + "0123456789";
    
    private static final String ANNOUNCE_PATH = "/announce";
    private static final String SCRAPE_PATH = "/scrape";
    
    private final HttpExecutor httpExecutor;
    private final Provider<HttpParams> defaultParamsProvider;

    @Inject
    public TorrentTrackerScraperImpl(HttpExecutor httpExecutor,
            @Named("defaults") Provider<HttpParams> defaultParamsProvider) {
        this.httpExecutor = httpExecutor;
        this.defaultParamsProvider = defaultParamsProvider;
    }

    /**
     * Submit the scrape request.  Notification will be returned through the callback
     *
     * @return the shutdownable for the connection, or null if no 
     *          connection was supported.
     */
    @Override
    public RequestShutdown submitScrape(URI trackerAnnounceUri, String urn,
            final ScrapeCallback callback) {

        if (!SearchSettings.USE_TORRENT_SCRAPER.get()) {
            LOG.debugf("scraping has been disabled");
            return null;
        }
        
        LOG.debugf("attempting: {0}", trackerAnnounceUri);
        
        if (!canHTTPScrape(trackerAnnounceUri)) {
            LOG.debugf("scraping not available for the uri");
            
            // Tracker does not support scraping so don't attempt
            return null;
        }
        
        URI uri;
        try {
            uri = createScrapingRequest(trackerAnnounceUri, urn);
        } catch (URISyntaxException e) {
            LOG.debugf("no valid URI could be created from the URN and announce URI so giving up");
            
            // URI could not be generated for the scrape request so don't try
            return null;
        }

        final HttpGet get = new HttpGet(uri);

        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, HTTP_TIMEOUT);
        params = new DefaultedHttpParams(params, defaultParamsProvider.get());
        
        LOG.debugf("submitting: {0}", uri);

        final Shutdownable shutdown = httpExecutor.execute(get, params, new HttpClientListener() {
            @Override
            public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
                get.abort();
                callback.failure("request failed");
                return false;
            }
            @Override
            public boolean requestComplete(HttpUriRequest request, HttpResponse response) {

                try {
                    HttpEntity entity = response.getEntity();

                    Object decoded = null;
                    try {
                        decoded = Token.parse(Channels.newChannel(entity.getContent()), "UTF-8");
                    } catch (IOException e) {
                        callback.failure(e.getMessage());
                        return false;
                    }

                    if(decoded == null || !(decoded instanceof Map<?,?>)) {
                        callback.failure("no scrape data in results downloaded");
                        return false;
                    }

                    Map<?,?> baseMap = (Map) decoded;

                    Object filesElement = baseMap.get("files");

                    if (!(filesElement instanceof Map<?,?>)) {
                        callback.failure("scrape results had bad structure");
                        return false;
                    }

                    Map<?,?> torrentsMap = (Map) filesElement;

                    if (torrentsMap.size() != 1) {
                        callback.failure("wrong number of elements in scrape results");
                        return false;
                    }


                    TorrentScrapeData data = parseResponseMap(torrentsMap.entrySet().iterator().next().getValue());
                    
                    if (data != null) {
                        callback.success(data);
                    } else {
                        callback.failure("Could not correctly parse the files entry of the scrape return");
                    }

                } finally {
                    // Ensure the connection is closed
                    get.abort();
                }
                
                return false;
            }

            @Override
            public boolean allowRequest(HttpUriRequest request) {
                return true;
            }
        });
        
        return new RequestShutdown() {
            @Override
            public void shutdown() {
                shutdown.shutdown();
            }
        };
    }
    
    /**
     * Attempt to parse out the scrape data from the element returned from
     *  the files key. 
     *  
     * @return the scrape data parsed or null if the map was not well formed. 
     */
    static TorrentScrapeData parseResponseMap(Object data) {
        
        if (!(data instanceof Map<?,?>)) {
            return null;
        }
        
        Map<?,?> torrentScrapeEntryMap = (Map) data;
        
        Object complete = torrentScrapeEntryMap.get("complete");
        Object incomplete = torrentScrapeEntryMap.get("incomplete");
        Object downloaded = torrentScrapeEntryMap.get("downloaded");
        
        if (!(complete instanceof Long)) {
            return null;
        }
        
        if (!(incomplete instanceof Long)) {
            return null;
        }
        
        if (!(downloaded instanceof Long)) {
            return null;
        }
        
        return new TorrentScrapeData((Long)complete, 
                (Long)incomplete, 
                (Long)downloaded);

    }

    private static boolean canHTTPScrape(URI trackerAnnounceUri) {
        String announceString = trackerAnnounceUri.toString();
        return announceString.toLowerCase(Locale.US).startsWith("http") && announceString.indexOf(ANNOUNCE_PATH) > 0;
    }
    
    private static URI createScrapingRequest(URI trackerAnnounceUri, String urn) throws URISyntaxException {
        String scrapeUriString = trackerAnnounceUri.toString().replaceFirst(ANNOUNCE_PATH, SCRAPE_PATH);
        StringBuffer buffer = new StringBuffer(scrapeUriString);

        if (scrapeUriString.endsWith(SCRAPE_PATH)) {
            buffer.append('?');
        } else {
            buffer.append('&');
        }
        
        buffer.append("info_hash=");
        buffer.append(httpEncodeURN(urn));
        
        return new URI(buffer.toString());
    }
    
    private static String httpEncodeURN(String urn) {
        StringBuffer sb = new StringBuffer();
        
        for ( byte b : StringUtils.fromHexString(urn) ) {
            if (UNRESERVED_CHARS.indexOf((char)b) > -1) {
                sb.append((char)b);
            } else {
                sb.append('%');
                sb.append(Integer.toString((b & 0xff)+0x100, 16).substring(1));
            }
        }
        
        return sb.toString();
    }


}

