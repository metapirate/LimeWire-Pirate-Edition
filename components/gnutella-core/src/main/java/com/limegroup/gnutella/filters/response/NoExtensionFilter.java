package com.limegroup.gnutella.filters.response;

import org.limewire.util.FileUtils;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

/** A filter that disallows responses without file extensions. */
public class NoExtensionFilter implements ResponseFilter {

    @Override
    public boolean allow(QueryReply qr, Response response) {
        return !FileUtils.getFileExtension(response.getName()).isEmpty();
    }
}
