package org.limewire.xmpp.client.impl.messages.address;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.io.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AddressIQ extends IQ {
    
    private final Address address;
    private final AddressFactory factory;

    public AddressIQ(XmlPullParser parser, AddressFactory factory) throws IOException, XmlPullParserException, InvalidIQException {
        this.factory = factory;
        Address parsedAddress = null;
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("address")) {
                    if(!parser.isEmptyElementTag()) {
                        eventType = parser.next();
                        if(eventType == XmlPullParser.START_TAG) {
                            String type = parser.getName();
                            String value = parser.getAttributeValue(null, "value");
                            if (value == null) {
                                throw new InvalidIQException("no value attribute");
                            }
                            try {
                                parsedAddress = factory.deserialize(type,  Base64.decodeBase64(StringUtils.toUTF8Bytes(value)));
                            } catch (IOException ie) {
                                throw new InvalidIQException("invalid address: " + value, ie);
                            }
                        }
                    }
                }
            } 
            if (eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("address")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        
        if (parsedAddress == null) {
            throw new InvalidIQException("no address to be parsed"); 
        }
        this.address = parsedAddress;
    }
    
    public AddressIQ(Address address, AddressFactory factory) {
        this.address = address;
        this.factory = factory;
    }

    public Address getAddress() {
        return address;
    }

    @Override
    public String getChildElementXML() {        
        StringBuilder pushEndpoint = new StringBuilder("<address xmlns=\"jabber:iq:lw-address\">");        
        if(address != null) {
            try {
                AddressSerializer addressSerializer = factory.getSerializer(address);
                pushEndpoint.append("<" + addressSerializer.getAddressType());
                pushEndpoint.append(" value=\"").append(StringUtils.toUTF8String(Base64.encodeBase64(addressSerializer.serialize(address)))).append("\"/>");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        pushEndpoint.append("</address>");
        return pushEndpoint.toString();
    }    
}