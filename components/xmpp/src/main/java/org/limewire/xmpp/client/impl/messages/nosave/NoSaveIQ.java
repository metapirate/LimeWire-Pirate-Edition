package org.limewire.xmpp.client.impl.messages.nosave;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.util.Objects;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.limewire.friend.impl.feature.NoSave;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NoSaveIQ extends IQ {
    
    private final Map<String, NoSave> items = new HashMap<String, NoSave>();

    public static final String ELEMENT_NAME = "query";
    public static final String NAME_SPACE = "google:nosave";


    NoSaveIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("query")) {
                } else if(parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue(null, "jid");
                    if (jid == null) { 
                        throw new InvalidIQException("no jid value");
                    }
                    String value = parser.getAttributeValue(null, "value");
                    if (value == null) {
                        throw new InvalidIQException("no value in value attribute");
                    }
                    items.put(jid, value.equals(NoSave.ENABLED.getPacketIdentifier()) ? NoSave.ENABLED : NoSave.DISABLED);
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("query")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }

    private NoSaveIQ(String jid, NoSave value) {
        items.put(Objects.nonNull(jid, "jid"), value);
    }

    private NoSaveIQ() { }


    
    /**
     * Create a NoSaveIQ message which sets a given user's nosave status.
     *
     * @param userId String representing ID of contact
     * @param value nosave boolean value to set
     * @return NoSaveIQ message
     */
    public static NoSaveIQ getNoSaveSetMessage(String userId, NoSave value) {
        NoSaveIQ setMsg = new NoSaveIQ(userId, value);
        setMsg.setType(Type.SET);
        return setMsg;
    }

    /**
     * Create a NoSaveIQ message which gets the nosave state for all users
     * on the roster.
     *
     * @return NoSaveIQ message to request nosave state for all friends
     */
    public static NoSaveIQ getNoSaveStatesMessage() {
        return new NoSaveIQ();
    }

    public Map<String, NoSave> getNoSaveUsers() {
        return Collections.unmodifiableMap(items);
    }

    @Override
    public String getChildElementXML() {
        StringBuilder s = new StringBuilder("<query xmlns='google:nosave'>");
        for(Map.Entry<String, NoSave> entry : items.entrySet()) {
            s.append("<item xmlns='google:nosave' jid='").append(entry.getKey()).append("' value='")
                    .append(entry.getValue().getPacketIdentifier()).append("'/>");
        }
        s.append("</query>");
        return s.toString();
    }

    public static IQProvider getIQProvider() {
        return new NoSaveIQProvider();
    }

    private static class NoSaveIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            try { 
                return new NoSaveIQ(parser);
            } catch (InvalidIQException iie) {
                // throwing would close connection
                return null;
            }
        }
    }
}
