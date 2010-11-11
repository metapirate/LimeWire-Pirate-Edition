package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.impl.feature.AuthTokenImpl;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AuthTokenIQ extends IQ {
    
    private AuthToken authToken;

    public AuthTokenIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("token")) {
                    String value = parser.getAttributeValue(null, "value");
                    if (value == null) {
                        throw new InvalidIQException("no value");
                    }
                    authToken = new AuthTokenImpl(value);
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("auth-token")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        
        if (authToken == null) {
            throw new InvalidIQException("no auth token parsed");
        }
    }
    
    /**
     * @param authToken must not be null
     */
    public AuthTokenIQ(AuthToken authToken) {
        this.authToken = Objects.nonNull(authToken, "authToken");
    }

    /**
     * @return not null
     */
    public AuthToken getAuthToken() {
        return authToken;
    }

    @Override
    public String getChildElementXML() {        
        StringBuilder authTokenElement = new StringBuilder("<auth-token xmlns=\"jabber:iq:lw-auth-token\">");        
        // original implementation does base64 encoding twice, oh well
        authTokenElement.append("<token value=\"").append(StringUtils.getUTF8String(Base64.encodeBase64(StringUtils.toUTF8Bytes(authToken.getBase64())))).append("\"/>");
        authTokenElement.append("</auth-token>");
        return authTokenElement.toString();
    }
    
    @Override
    public String toString() {
        return authToken.getBase64();
    }
}
