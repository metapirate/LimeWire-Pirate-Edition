package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.download.DownloadState;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;

import static org.limewire.ui.swing.util.I18n.tr;

/**
 * Implementation of a message containing a file offer.
 */
class MessageFileOfferImpl extends AbstractMessageImpl implements MessageFileOffer {

    private static final String DOWNLOAD_FROM_LIBRARY = tr("Download it now, or get it from them " +
            "{0}later{1}.","<a href=\"" + ChatDocumentBuilder.LIBRARY_LINK + "\">", "</a>");

    private final FileMetaData fileMetadata;
    private DownloadState downloadState;
    private FriendPresence sourcePresence;


    public MessageFileOfferImpl(String senderName, String friendId, Type type, FileMetaData fileMetadata, FriendPresence sourcePresence) {
        super(senderName, friendId, type);
        this.fileMetadata = fileMetadata;
        this.downloadState = null;
        this.sourcePresence = sourcePresence;
    }

    @Override
    public FileMetaData getFileOffer() {
        return fileMetadata;
    }

    @Override
    public void setDownloadState(DownloadState downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public FriendPresence getPresence() {
        return sourcePresence;
    }

    @Override
    public String toString() {
        String state = (downloadState == null) ? "No State" : downloadState.toString();
        String fileOffer = fileMetadata.getName();
        return fileOffer + "(" + state + ")";
    }

    @Override
    public String format() {
        boolean isIncoming = (getType() == Message.Type.RECEIVED);
        return isIncoming ? formatIncoming() : formatOutgoing();
    }

    private String formatOutgoing() {
        StringBuffer fileOfferOutgoingMsg = new StringBuffer();
        
        fileOfferOutgoingMsg.append(tr("Sharing file with {0}", getFriendID()));
        fileOfferOutgoingMsg.append(formatButtonText(getFileOffer().getName(), false));
        return fileOfferOutgoingMsg.toString();
    }

    private String formatButtonText(String buttonText, boolean buttonEnabled) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<br/>")
            .append("<form action=\"\"><input type=\"hidden\" name=\"fileid\" value=\"")
            .append(getFileOffer().getId())
            .append("\"/><input type=\"submit\" value=\"")
            .append(buttonText)
            .append(buttonEnabled ? "\"/>" : ":disabled\"/>")
            .append("</form><br/>");
        return bldr.toString();
    }

    private String formatIncoming() {

        String fileOfferFormatted;
        String fileOfferReceived = tr("{0} wants to share a file with you", getFriendID());
        String defaultFileOfferFormatted = fileOfferReceived + formatButtonText(tr("Download {0}", fileMetadata.getName()), true)
                    + DOWNLOAD_FROM_LIBRARY;

        if (downloadState == null) {
            // dl has not started yet...
            fileOfferFormatted = defaultFileOfferFormatted;
        } else {

            switch (downloadState) {
                case REMOTE_QUEUED:
                case LOCAL_QUEUED:
                case TRYING_AGAIN:
                case CONNECTING:
                case PAUSED:
                case FINISHING:
                case DOWNLOADING:
                    fileOfferFormatted = fileOfferReceived +
                        formatButtonText(tr("Downloading {0}", fileMetadata.getName()), false) +
                                DOWNLOAD_FROM_LIBRARY;
                    break;

                case CANCELLED:
                    fileOfferFormatted = defaultFileOfferFormatted + "<br/><br/>" +
                            tr("Download cancelled.  Click button to retry");
                    break;

                case STALLED:
                case ERROR:
                    fileOfferFormatted = defaultFileOfferFormatted + "<br/><br/>" +
                            tr("Error downloading file.  Click button to retry");
                    break;
                case DONE:
                    fileOfferFormatted = fileOfferReceived +
                            tr("{0}Downloaded{1}","<a href=\"" +
                            ChatDocumentBuilder.MY_LIBRARY_LINK + "\">", "</a>");
                    break;
                    
                case SCAN_FAILED:
                    fileOfferFormatted = fileOfferReceived +
                            tr("{0}Downloaded but not scanned for viruses{1}",
                                    "<a href=\"" + ChatDocumentBuilder.MY_LIBRARY_LINK + "\">", "</a>");
                    break;
                    
                case SCANNING:
                case SCANNING_FRAGMENT:
                    fileOfferFormatted = fileOfferReceived +
                            tr("Scanning for viruses - Powered by AVG");
                    break;
                    
                case THREAT_FOUND:
                    fileOfferFormatted = fileOfferReceived + "<br/><br/>" +
                            tr("File deleted - Threat detected by AVG");
                    break;
                    
                case DANGEROUS:
                    fileOfferFormatted = fileOfferReceived + "<br/><br/>" +
                            tr("File deleted - Dangerous file");
                    break;
                    
                default:
                    fileOfferFormatted = defaultFileOfferFormatted;
                    break;
            }
        }
        return fileOfferFormatted;
    }
}
