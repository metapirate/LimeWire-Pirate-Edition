package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.Function;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AltLocUtils;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.uploader.HTTPUploader;

/**
 * Processes alternate location headers from an {@link HttpRequest} and updates
 * a corresponding {@link HTTPUploader}.
 */
public class AltLocHeaderInterceptor implements HeaderInterceptor {

    private final HTTPUploader uploader;
    private final AltLocManager altLocManager;
    private final AlternateLocationFactory alternateLocationFactory;

    public AltLocHeaderInterceptor(HTTPUploader uploader, AltLocManager altLocManager, AlternateLocationFactory alternateLocationFactory) {
        this.uploader = uploader;
        this.altLocManager = altLocManager;
        this.alternateLocationFactory = alternateLocationFactory;
    }

    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        if (HTTPHeaderName.ALT_LOCATION.matches(header)) {
            parseAlternateLocations(uploader.getAltLocTracker(), header.getValue(),
                    true, true);
        } else if (HTTPHeaderName.NALTS.matches(header)) {
            parseAlternateLocations(uploader.getAltLocTracker(), header.getValue(),
                    false, false);
        } else if (HTTPHeaderName.FALT_LOCATION.matches(header)) {
            AltLocTracker tracker = uploader.getAltLocTracker();
            parseAlternateLocations(tracker, header.getValue(), true, false);
            tracker.setWantsFAlts(true);
        } else if (HTTPHeaderName.BFALT_LOCATION.matches(header)) {
            AltLocTracker tracker = uploader.getAltLocTracker();
            parseAlternateLocations(tracker, header.getValue(), false, false);
            tracker.setWantsFAlts(false);
        }
    }

    /**
     * Parses the alternate location header. The header can contain only one
     * alternate location, or it can contain many in the same header. This
     * method will notify DownloadManager of new alternate locations if the
     * FileDesc is an IncompleteFileDesc.
     * 
     * @param altLocTracker the tracker that stores locations
     * @param altHeader the full alternate locations header
     */
    private void parseAlternateLocations(final AltLocTracker tracker,
            String alternateLocations, final boolean isGood, boolean allowTLS) {
        AltLocUtils.parseAlternateLocations(tracker.getUrn(), alternateLocations, allowTLS, alternateLocationFactory, new Function<AlternateLocation, Void>() {
            public Void apply(AlternateLocation location) {
                if (location instanceof PushAltLoc)
                    ((PushAltLoc) location).updateProxies(isGood);
                // Note: if this thread gets preempted at this point,
                // the AlternateLocationCollectioin may contain a PE
                // without any proxies.
                if (isGood)
                    altLocManager.add(location, null);
                else
                    altLocManager.remove(location, null);

                tracker.addLocation(location);
                return null;
            }
        });
    }

}
