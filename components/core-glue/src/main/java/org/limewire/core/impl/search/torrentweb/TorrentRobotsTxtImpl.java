package org.limewire.core.impl.search.torrentweb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.robots.RobotsDirectives;
import org.limewire.http.httpclient.robots.RobotsTxt;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Implements {@link TorrentRobotsTxt} by first doing a local lookup for
 * a cached robots.txt file in {@link TorrentRobotsTxtStore}, otherwise downloads
 * robotst.txt from webserver and stores it in {@link TorrentRobotsTxtStore}.
 */
public class TorrentRobotsTxtImpl implements TorrentRobotsTxt {

    private final static Log LOG = LogFactory.getLog(TorrentRobotsTxtImpl.class);
    
    private final Provider<LimeHttpClient> limeHttpClient;
    private final TorrentRobotsTxtStore torrentRobotsTxtStore;

    @Inject
    public TorrentRobotsTxtImpl(Provider<LimeHttpClient> limeHttpClient,
            TorrentRobotsTxtStore torrentRobotsTxtStore) {
        this.limeHttpClient = limeHttpClient;
        this.torrentRobotsTxtStore = torrentRobotsTxtStore;
    }
    
    @Override
    public boolean isAllowed(URI uri) {
        String host = URIUtils.getCanonicalHost(uri);
        if (host == null || uri.getPath() == null) {
            return true;
        }
        String robotsTxt = torrentRobotsTxtStore.getRobotsTxt(host);
        if (robotsTxt == null) {
            robotsTxt = getRobotsTxt(uri);
            torrentRobotsTxtStore.storeRobotsTxt(host, robotsTxt);
        }
        try {
            RobotsTxt parser = new RobotsTxt(robotsTxt);
            return isAllowed(parser, uri);
        } catch (InvalidDataException e) {
            LOG.debug("error parsing robots txt", e);
            return true;
        }
    }
    
    /**
     * @return empty string on error to avoid future requests for robots.txt
     */
    private String getRobotsTxt(URI uri) {
        LimeHttpClient httpClient = limeHttpClient.get();
        HttpGet get = new HttpGet(org.apache.http.client.utils.URIUtils.resolve(uri, "/robots.txt"));
        HttpResponse response = null;
        BufferedOutputStream out = null;
        try {
            response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return "";
                }
                File file = createTmpFile(uri);
                out = new BufferedOutputStream(new FileOutputStream(file));
                entity.writeTo(out);
                out.close();
                if (file.length() < TorrentRobotsTxtStore.MAX_ROBOTS_TXT_SIZE) {
                    byte[] contents = FileUtils.readFileFully(file);
                    if (contents != null) {
                        return StringUtils.toUTF8String(contents);
                    }
                }
            }
        } catch (IOException e) {
            LOG.debug("error getting robots.txt", e);
        } finally {
            httpClient.releaseConnection(response);
            IOUtils.close(out);
        }
        return "";
    }

    private boolean isAllowed(RobotsTxt robotsTxt, URI uri) {
        RobotsDirectives robotsDirectives = robotsTxt.getDirectivesFor(LimeWireUtils.getHttpServer());
        LOG.debugf("directives for {0}: {1}", uri, robotsDirectives);
        return robotsDirectives.allows(uri.getPath());
    }

    private File createTmpFile(URI uri) throws IOException {
        File file = File.createTempFile(URIUtils.getCanonicalHost(uri), ".robots.txt");
        file.deleteOnExit();
        return file;
    }
}
