package org.limewire.xmpp.client.impl.messages.authtoken;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;

public class AuthTokenIQProvider implements IQProvider {

    private static Log LOG = LogFactory.getLog(AuthTokenIQProvider.class);
    
    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            return new AuthTokenIQ(parser);
        } catch (InvalidIQException ie) {
            LOG.debug("invalid iq", ie);
            // throwing would close connection
            return null;
        }
    }

}
