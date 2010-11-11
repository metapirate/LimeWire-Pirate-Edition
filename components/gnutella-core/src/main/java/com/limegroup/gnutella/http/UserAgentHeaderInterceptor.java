package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.HeaderInterceptor;

import com.limegroup.gnutella.uploader.HTTPUploader;

/**
 * Processes the User-Agent headers from an {@link HttpRequest} and updates a
 * corresponding {@link HTTPUploader}.
 */
public class UserAgentHeaderInterceptor implements HeaderInterceptor {

    private HTTPUploader uploader;

    public UserAgentHeaderInterceptor(HTTPUploader uploader) {
        this.uploader = uploader;
    }

    public void process(Header header, HttpContext context)
            throws HttpException, IOException {
        if (!HTTPHeaderName.USER_AGENT.matches(header)) {
            return;
        }

        String userAgent = header.getValue();
        if (uploader != null) {
            uploader.setUserAgent(userAgent);
        }
    }

    public static boolean isFreeloader(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        
        return ((userAgent.indexOf("Mozilla") != -1)
                || (userAgent.indexOf("Morpheus") != -1)
                || (userAgent.indexOf("DA") != -1)
                || (userAgent.indexOf("Download") != -1)
                || (userAgent.indexOf("FlashGet") != -1)
                || (userAgent.indexOf("GetRight") != -1)
                || (userAgent.indexOf("Go!Zilla") != -1)
                || (userAgent.indexOf("Inet") != -1)
                || (userAgent.indexOf("MIIxpc") != -1)
                || (userAgent.indexOf("MSProxy") != -1)
                || (userAgent.indexOf("Mass") != -1)
                || (userAgent.indexOf("MLdonkey") != -1)
                || (userAgent.indexOf("MyGetRight") != -1)
                || (userAgent.indexOf("NetAnts") != -1)
                || (userAgent.indexOf("NetZip") != -1)
                || (userAgent.indexOf("RealDownload") != -1)
                || (userAgent.indexOf("SmartDownload") != -1)
                || (userAgent.indexOf("Teleport") != -1)
                || (userAgent.indexOf("WebDownloader") != -1));
    }
}
