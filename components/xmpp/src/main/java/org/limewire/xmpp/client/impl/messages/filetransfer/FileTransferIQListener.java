package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.xmpp.client.impl.XMPPFriendConnectionImpl;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class FileTransferIQListener implements PacketListener, FeatureTransport<FileMetaData> {
    private static final Log LOG = LogFactory.getLog(FileTransferIQListener.class);
    private final XMPPFriendConnectionImpl connection;
    private final Handler<FileMetaData> fileMetaDataHandler;

    @Inject
    public FileTransferIQListener(@Assisted XMPPFriendConnectionImpl connection,
                                  FeatureTransport.Handler<FileMetaData> fileMetaDataHandler) {
        this.connection = connection;
        this.fileMetaDataHandler = fileMetaDataHandler;
    }

    public void processPacket(Packet packet) {
        FileTransferIQ iq = (FileTransferIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        }
    }

    private void handleGet(FileTransferIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling file transfer get " + packet.getPacketID());
        }
        fileMetaDataHandler.featureReceived(packet.getFrom(), packet.getFileMetaData());
    }

    @Override
    public void sendFeature(FriendPresence presence, FileMetaData localFeature) throws FriendException {
        if(LOG.isInfoEnabled()) {
            LOG.info("offering file " + localFeature.toString() + " to " + presence.getPresenceId());
        }
        final FileTransferIQ transferIQ = new FileTransferIQ(localFeature, FileTransferIQ.TransferType.OFFER);
        transferIQ.setType(IQ.Type.GET);
        transferIQ.setTo(presence.getPresenceId());
        transferIQ.setPacketID(IQ.nextID());
        connection.sendPacket(transferIQ);
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof FileTransferIQ;
            }
        };
    }
}
