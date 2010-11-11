package com.limegroup.gnutella.filters.response;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.security.SecureMessage;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class SecureResultFilter implements ResponseFilter {

    @Override
    public boolean allow(QueryReply qr, Response response) {
        // If there was an action, only allow it if it's a secure message.
        LimeXMLDocument doc = response.getDocument();
        return !ApplicationSettings.USE_SECURE_RESULTS.getValue()
            || doc == null
            || "".equals(doc.getAction())
            || qr.getSecureStatus() == SecureMessage.Status.SECURE;
    }

}
