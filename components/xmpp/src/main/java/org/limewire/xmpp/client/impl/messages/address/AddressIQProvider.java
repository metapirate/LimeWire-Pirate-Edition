package org.limewire.xmpp.client.impl.messages.address;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;

public class AddressIQProvider implements IQProvider {
        
    private static final Log LOG = LogFactory.getLog(AddressIQProvider.class);
    
    private final AddressFactory factory;

    public AddressIQProvider(AddressFactory factory){
        this.factory = factory;
    }

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        try {
            return new AddressIQ(parser, factory);
        } catch (InvalidIQException ie) {
            LOG.debug("invalid iq", ie);
            // throwing would close connection
            return null;
        }
    }
}
