package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.io.IOException;

import org.limewire.friend.impl.FileMetaDataImpl;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class XMPPFileMetaData extends FileMetaDataImpl {
    public XMPPFileMetaData(XmlPullParser parser) throws XmlPullParserException, IOException, InvalidIQException {
        parser.nextTag();
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                data.put(parser.getName(), parser.nextText());
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("file")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        if (!isValid()) {
            throw new InvalidIQException("is missing mandatory fields: " + this);
        }
    }
}
