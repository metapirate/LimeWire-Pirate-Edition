package org.limewire.xmpp.client.impl.messages.library;

import java.io.IOException;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class LibraryChangedIQListener implements PacketListener, FeatureTransport<LibraryChangedNotifier> {
    
    private static final Log LOG = LogFactory.getLog(LibraryChangedIQListener.class);

    private final Handler<LibraryChangedNotifier> libChangedHandler;
    private final XMPPFriendConnectionImpl connection;

    @Inject
    public LibraryChangedIQListener(Handler<LibraryChangedNotifier> libChangedListeners,
                                    @Assisted XMPPFriendConnectionImpl connection) {
        this.libChangedHandler = libChangedListeners;
        this.connection = connection;
    }

    public void processPacket(Packet packet) {
        LibraryChangedIQ iq = (LibraryChangedIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                //handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                //handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                LOG.debugf("received iq {0}", packet);
                handleSet(iq);
            } else if(iq.getType().equals(IQ.Type.ERROR)) {
                //handleError(iq);
            } else {
                //sendError(packet);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        }
    }

    private void handleSet(LibraryChangedIQ packet) throws IOException, XmlPullParserException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("handling library changed set " + packet.getPacketID());
        }
        libChangedHandler.featureReceived(packet.getFrom(), new LibraryChangedNotifier(){});
    }

    @Override
    public void sendFeature(FriendPresence presence, LibraryChangedNotifier localFeature) throws FriendException {
        LOG.debug("send library refresh");
        if(connection.isLoggedIn()) {
            final LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
            libraryChangedIQ.setType(IQ.Type.SET);
            libraryChangedIQ.setTo(presence.getPresenceId());
            libraryChangedIQ.setPacketID(IQ.nextID());
            try {
                LOG.debugf("sending refresh to {0}", presence.getPresenceId());
                connection.sendPacket(libraryChangedIQ);
            } catch (FriendException e) {
                LOG.debugf("library refresh failed", e);
            }
        }
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof LibraryChangedIQ;
            }
        };
    }
}
