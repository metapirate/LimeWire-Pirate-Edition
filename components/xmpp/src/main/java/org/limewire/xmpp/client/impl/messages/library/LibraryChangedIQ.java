package org.limewire.xmpp.client.impl.messages.library;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LibraryChangedIQ extends IQ {

    public LibraryChangedIQ(XmlPullParser parser) throws IOException, XmlPullParserException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("library-changed")) {

                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("library-changed")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }

    public LibraryChangedIQ() {
    }

    @Override
    public String getChildElementXML() {
        return "<library-changed xmlns='jabber:iq:lw-lib-change'/>";
    }

    public static IQProvider getIQProvider() {
        return new LibraryChangedIQProvider();
    }

    private static class LibraryChangedIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            return new LibraryChangedIQ(parser);
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " from: " + getFrom();
    }
}
