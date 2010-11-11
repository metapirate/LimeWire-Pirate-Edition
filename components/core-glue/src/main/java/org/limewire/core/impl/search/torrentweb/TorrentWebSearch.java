package org.limewire.core.impl.search.torrentweb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.TorrentFactory;
import org.limewire.core.impl.download.DownloadItemFactory;
import org.limewire.core.impl.download.DownloadItemFactoryRegistry;
import org.limewire.core.settings.SearchSettings;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IOUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.filters.response.FilterFactory;
import com.limegroup.gnutella.filters.response.SearchResultFilter;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Performs a websearch for torrents.
 */
public class TorrentWebSearch implements Search {
    
    private static final Log LOG = LogFactory.getLog(TorrentWebSearch.class);
    
    /**
     * Content type of torrent files.
     */
    private static final String BITTORENT_CONTENT_TYPE = "application/x-bittorrent";
    /**
     * Conent type of html files.
     */
    private static final String HTML_CONTENT_TYPE = "text/html";
    
    private final HttpExecutor httpExecutor;
    /**
     * Template for search uri.
     */
    private final String searchUriTemplate = SearchSettings.TORRENT_WEB_SEARCH_URI_TEMPLATE.get();
    
    private final Provider<LimeHttpClient> httpClient;
    /**
     * The query as entered by the user.
     */
    private final String query;

    private final TorrentUriPrioritizerFactory torrentUriPrioritizerFactory;

    private final MetaDataReader metaDataReader;

    private final TorrentFactory torrentFactory;

    private final SearchResultFilter filter;

    private final TorrentRobotsTxt torrentRobotsTxt;
    
    private volatile boolean stopped = false;
    
    private volatile SearchListener searchListener;
    
    @Inject
    public TorrentWebSearch(HttpExecutor httpExecutor, Provider<LimeHttpClient> httpClient,
            TorrentUriPrioritizerFactory torrentUriPrioritizerFactory,
            MetaDataReader metaDataReader,
            TorrentFactory torrentFactory,
            @Assisted String query,
            FilterFactory responseFilterFactory,
            TorrentRobotsTxt torrentRobotsTxt) {
        this.httpExecutor = httpExecutor;
        this.httpClient = httpClient;
        this.torrentUriPrioritizerFactory = torrentUriPrioritizerFactory;
        this.metaDataReader = metaDataReader;
        this.torrentFactory = torrentFactory;
        this.query = query;
        this.torrentRobotsTxt = torrentRobotsTxt;
        this.filter = responseFilterFactory.createResultFilter();
    }
    
    @Inject
    void register(DownloadItemFactoryRegistry registry, DownloadListManager downloadListManager) {
        registry.register(new TorrentWebSearchCoreDownloadItemFactory(downloadListManager));
    }

    @Override
    public void start() {
        try {
            HttpGet get = new HttpGet(MessageFormat.format(searchUriTemplate, URIUtils.encodeUriComponent(query)));
            get.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
            httpExecutor.execute(get, new GoogleJsonResponseHandler(query));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void stop() {
        LOG.debug("stop");
        stopped = true;
    }

    @Override
    public void addSearchListener(SearchListener searchListener) {
        assert this.searchListener == null;
        this.searchListener = searchListener;
    }

    @Override
    public SearchCategory getCategory() {
        return SearchCategory.TORRENT;
    }

    @Override
    public void removeSearchListener(SearchListener searchListener) {
        assert this.searchListener == searchListener;
        this.searchListener = null;
    }

    @Override
    public void repeat() {
    }
    
    private void handleTorrentResult(File torrentFile, URI uri, URI referrer) {
        BTData torrentData = TorrentWebSearchUtils.parseTorrentFile(torrentFile);
        if (torrentData != null) {
            Torrent torrent = null;
            LimeXMLDocument xmlDocument = null;
            try {
                xmlDocument = metaDataReader.readDocument(torrentFile);
                if (xmlDocument != null) {
                    if (!matchesQuery(xmlDocument)) {
                        LOG.debugf("query {0} does not match doc {1}", query, xmlDocument);
                        return;
                    }
                    torrent = torrentFactory.createTorrentFromXML(xmlDocument);
                    if (torrent != null) {
                        SearchResult result = new TorrentWebSearchResult(torrentData, referrer, torrentFile, torrent);
                        if (filter.allow(result, xmlDocument)) {
                            LOG.debugf("result accepted: {0}", torrent);
                            searchListener.handleSearchResult(this, result);
                        } else{
                            LOG.debugf("result rejected: {0}", torrent);
                        }
                    } else {
                        LOG.debug("torrent null");
                    }
                }
            } catch (IOException ie) {
                LOG.debug("error parsing torrent file", ie);
            }
        } else {
            LOG.debug("torrent data null");
        }
    }
    
    boolean matchesQuery(LimeXMLDocument xmlDocument) {
        Set<String> queryTokens = QueryUtils.extractKeywords(query, true);
        for (Entry<String, String> entry : xmlDocument.getNameValueSet()) {
            Set<String> valueTokens = QueryUtils.extractKeywords(entry.getValue(), true);
            if (valueTokens.containsAll(queryTokens)) {
                return true;
            }
        }
        return false;
    }
     
    private boolean isTorrentFile(File file) {
        return FileUtils.getFileExtension(file).equals("torrent");
    }
    
    private boolean isHtmlFile(File file) {
        return FileUtils.getFileExtension(file).equals("html");
    }
         
    private void handleGoogleResults(List<URI> uris, String query) {
        LOG.debugf("results: {0}", uris);
        for (URI uri : uris) {
            if (stopped) {
                LOG.debug("stopping");
                break;
            }
            if (!torrentRobotsTxt.isAllowed(uri)) {
                LOG.debugf("not allowed by robots.txt {0}", uri);
                continue;
            }
            File file = getContent(uri);
            if (file == null) {
                continue;
            }
            if (isTorrentFile(file)) {
                handleTorrentResult(file, uri, null);
            } else if (isHtmlFile(file)) {
                try {
                    List<URI> candidates = extractTorrentUriCandidates(file, uri);
                    TorrentUriPrioritizer prioritizer = torrentUriPrioritizerFactory.create(query, uri);
                    checkForTorrents(prioritizer.prioritize(candidates), prioritizer, uri);
                } catch (IOException e) {
                    LOG.debug("error parsing html", e);
                }
            }
        }
    }
    
    /**
     * Extracts all uris from <code>htmlFile</code> that are the targets of anchor
     * elements and could be potential torrent uris.
     */
    List<URI> extractTorrentUriCandidates(File htmlFile, URI referrer) throws IOException {
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode tagNode = cleaner.clean(htmlFile);
        @SuppressWarnings("unchecked")
        List<TagNode> anchors = tagNode.getElementListHavingAttribute("href", true);
        List<URI> candidates = new ArrayList<URI>(anchors.size());
        for (TagNode node : anchors) {
            if (!"a".equalsIgnoreCase(node.getName())) {
                continue;
            }
            String href = node.getAttributeByName("href");
            LOG.debugf("resolving: {0} with {1}", href, referrer);
            try {
                URI link = URIUtils.toURI(href);
                if (canBeTorrentUri(link)) {
                    candidates.add(link);
                } else {
                    link = org.apache.http.client.utils.URIUtils.resolve(referrer, link);
                    if (canBeTorrentUri(link)) {
                        candidates.add(link);
                    } else {
                        LOG.debugf("not a potential torrent link: {0}", link);
                    }
                }
            } catch (URISyntaxException e) {
                LOG.debug("error parsing", e);
            }
        }
        return candidates;
    }
    
    private void checkForTorrents(List<URI> candidates, TorrentUriPrioritizer prioritizer,
            URI referrer) {
        int count = 0;
        for (URI uri : candidates) {
            if (stopped) {
                LOG.debug("stopping");
                break;
            }
            if (!torrentRobotsTxt.isAllowed(uri)) {
                LOG.debugf("not allowed by robots.txt: {0}", uri);
                continue;
            }
            ++count;
            try {
                if (isTorrent(uri)) {
                    LOG.debugf("found torrent after {0} checks", count);
                    prioritizer.setIsTorrent(uri, true);
                    File file = getContent(uri);
                    if (file != null && isTorrentFile(file)) {
                        handleTorrentResult(file, uri, referrer);
                        break;
                    }
                } else {
                    prioritizer.setIsTorrent(uri, false);
                }
            } catch (IOException ie) {
                LOG.debugf(ie, "couldn't head {0}", uri);
            }
        }
    }
    
    private boolean canBeTorrentUri(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        return scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("magnet");
    }

    private boolean isTorrent(URI uri) throws IOException {
        LOG.debugf("torrent verification request: {0}", uri);
        if ("magnet".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        HttpHead head = new HttpHead(uri);
        HttpResponse response = null;
        LimeHttpClient client = httpClient.get();
        try {
            response = client.execute(head);
            Header header = response.getFirstHeader(HTTP.CONTENT_TYPE);
            if (header != null) {
                LOG.debugf("content type: {0}", header);
                if (BITTORENT_CONTENT_TYPE.equals(header.getValue())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            LOG.error("error with head request", e);
            throw e;
        } finally {
            client.releaseConnection(response);
        }
    }
    
    private static File createTmpFile(URI uri, String contentType) throws IOException {
        try {
            String prefix = URIUtils.encodeUriComponent(uri.toASCIIString());
            File file;
            if (BITTORENT_CONTENT_TYPE.equals(contentType)) {
                file = File.createTempFile(prefix, ".torrent");
            } else if (HTML_CONTENT_TYPE.equals(contentType)) {
                file = File.createTempFile(prefix, ".html");
            } else {
                return null;
            }
            file.deleteOnExit();
            return file;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    private File getContent(URI uri) {
        LOG.debugf("get content: {0}", uri);
        HttpGet get = new HttpGet(uri);
        HttpResponse response = null;
        BufferedOutputStream out = null;
        LimeHttpClient client = httpClient.get();
        try  {
            response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                LOG.debug("no entity");
                return null;
            }
            Header contentType = entity.getContentType();
            if (contentType == null) {
                LOG.debug("no content type");
                return null;
            }
            File file = createTmpFile(uri, contentType.getValue());
            if (file == null) {
                LOG.debugf("not file for content type: {0}", contentType);
                return null;
            }
            out = new BufferedOutputStream(new FileOutputStream(file));
            entity.writeTo(out);
            return file;
        } catch (IOException ie) {
            LOG.debug("error with GET request", ie);
        } finally {
            IOUtils.close(out);
            client.releaseConnection(response);
        }
        return null;
    }

    private class GoogleJsonResponseHandler implements HttpClientListener {

        private final String query;

        public GoogleJsonResponseHandler(String query) {
            this.query = query;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }

        @Override
        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    String json = EntityUtils.toString(entity);
                    JSONObject object = new JSONObject(json);
                    JSONArray results = object.getJSONObject("responseData").getJSONArray("results");
                    List<URI> uris = new ArrayList<URI>(results.length());
                    for (int i = 0; i < results.length(); i++) {
                        try {
                            uris.add(URIUtils.toURI(results.getJSONObject(i).getString("url")));
                        } catch (URISyntaxException e) {
                            LOG.error("couldn't parse url", e);
                        }
                    }
                    handleGoogleResults(uris, query);
                } catch (IOException e) {
                    LOG.error("error getting enitity", e);
                } catch (JSONException e) {
                    LOG.error("error parsing json", e);
                } finally {
                    HttpClientUtils.releaseConnection(response);
                }
            }
            return false;
        }

        @Override
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.debugf(exc, "request failed: {0}, {1}", request, response);
            return false;
        }
    }
    
    private static class TorrentWebSearchCoreDownloadItemFactory implements DownloadItemFactory {
        
        private final DownloadListManager downloadListManager;

        public TorrentWebSearchCoreDownloadItemFactory(DownloadListManager downloadListManager) {
            this.downloadListManager = downloadListManager;
        }

        @Override
        public DownloadItem create(Search search, List<? extends SearchResult> searchResults,
                File saveFile, boolean overwrite) throws DownloadException {
            if (searchResults.get(0) instanceof TorrentWebSearchResult) {
                TorrentWebSearchResult result = (TorrentWebSearchResult)searchResults.get(0);
                return downloadListManager.addTorrentDownload(result.getTorrentFile(), saveFile, overwrite);
            }
            return null;
        }
        
    }
    
}
