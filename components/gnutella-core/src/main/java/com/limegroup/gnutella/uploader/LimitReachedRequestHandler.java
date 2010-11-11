package com.limegroup.gnutella.uploader;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.auth.ServerAuthState;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.library.FileDesc;

/**
 * Responds with an HTTP 503 error signaling that the limit of allowed uploads
 * has been reached.
 */
public class LimitReachedRequestHandler extends SimpleNHttpRequestHandler {

    /** Time to wait for a retry-after because we're validating the file.*/
    public static final String RETRY_AFTER_VALIDATING = 20 + "";

    /**
     * The time to wait for a normal retry after in minutes.
     */
    public static final int RETRY_AFTER_TIME = 60 * 15;

    /**
     * Number of seconds the remote host should wait before retrying in case we
     * don't have any alt-locs left to send. 20 minutes.
     */
    private static final String NO_ALT_LOCS_RETRY_AFTER = "" + (20 * 60);
    
    private static final String FRIEND_NO_ALT_LOCS_RETRY_AFTER = String.valueOf(30);

    /**
     * Number of seconds the remote host should wait before retrying in case we
     * still have alt-locs left to send. (15 minute).
     */
    private static final String NORMAL_RETRY_AFTER = "" + RETRY_AFTER_TIME;

    /**
     * The error message to send in the message body.
     */
    private static final String ERROR_MESSAGE = "Server busy.  Too many active uploads.";

    /** Error msg to use when validating. */
    private static final String VALIDATING_MSG = "Validating file.  One moment please.";

    /** True if this is a LimitReached state because we're validating the file. */
    private final boolean validating;

    private final HTTPUploader uploader;
    private final FileDesc fd;
    private final HTTPHeaderUtils httpHeaderUtils;
    private final AltLocManager altLocManager;

    /**
     * Creates a new <tt>LimitReachedUploadState</tt> with the specified
     * <tt>FileDesc</tt>.
     */
    LimitReachedRequestHandler(HTTPUploader uploader, HTTPHeaderUtils httpHeaderUtils, AltLocManager altLocManager) {
        this.uploader = uploader;
        this.validating = false; //Note: Never invoked with validating = true, see Uploader.NOT_VALIDATED.
        this.fd = uploader.getFileDesc();
        this.httpHeaderUtils = httpHeaderUtils;
        this.altLocManager = altLocManager;
    }
    
    public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
            HttpContext context) throws HttpException, IOException {
        return null;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response,
            HttpContext context) throws HttpException, IOException {
        httpHeaderUtils.addProxyHeader(response);
        httpHeaderUtils.addAltLocationsHeader(response, uploader.getAltLocTracker(), altLocManager);

        String errorMsg = ERROR_MESSAGE;
        if (fd != null) {
            URN sha1 = fd.getSHA1Urn();
            if (validating) {
                errorMsg = VALIDATING_MSG;
                response.addHeader(HTTPHeaderName.RETRY_AFTER
                        .create(RETRY_AFTER_VALIDATING));
            } else if (sha1 != null) {
                // write the Retry-After header, using different values
                // depending on if we had any alts to send or not.
                String retry = !altLocManager.hasAltlocs(sha1) || isFriendRequest(context) ? getRetryAfterNoAltLocs(context)
                        : NORMAL_RETRY_AFTER;
                response.addHeader(HTTPHeaderName.RETRY_AFTER.create(retry));
                httpHeaderUtils.addRangeHeader(response, uploader, fd);
            } else {
                response.addHeader(HTTPHeaderName.RETRY_AFTER.create(getRetryAfterNoAltLocs(context)));
            }
        }

        uploader.setState(UploadStatus.LIMIT_REACHED);
        response.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
        response.setEntity(new NStringEntity(errorMsg));
    }
    
    private boolean isFriendRequest(HttpContext context) {
        // it's enough to check if auth state is there, the guarding handler
        // already verified the credentials
        ServerAuthState serverAuthState = (ServerAuthState) context.getAttribute(ServerAuthState.AUTH_STATE);
        return serverAuthState != null && serverAuthState.getCredentials() != null;
    }
    
    /**
     * Returns smaller Retry-After value for authenticated friend downloads. 
     */
    // TODO breaks knowledge about friend downloads, this layer should not
    // be aware of friends stuff
    private String getRetryAfterNoAltLocs(HttpContext context) {
        // it's enough to check if auth state is there, the guarding handler
        // already verified the credentials
       if (isFriendRequest(context)) {
            return FRIEND_NO_ALT_LOCS_RETRY_AFTER;
        } else {
            return NO_ALT_LOCS_RETRY_AFTER;
        }
    }
}
