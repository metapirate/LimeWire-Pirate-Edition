package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.download.DownloadItem.ErrorState;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.malware.AntivirusUpdateType;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.transfer.TransferRendererResources;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

/**
 * Renderer/editor component for the download table to display message. 
 */
public class DownloadMessageRendererEditor extends TableRendererEditor {

    @Resource private Icon infoIcon;
    
    private final DownloadActionHandler actionHandler;
    private final TransferRendererResources resources;
    
    private JLabel messageLabel;
    private JButton infoButton;
    
    private DownloadItem downloadItem;
    
    /**
     * Constructs a DownloadMessageRendererEditor that uses the specified 
     * action handler.
     */
    @Inject
    public DownloadMessageRendererEditor(DownloadActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        this.resources = new TransferRendererResources();
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, aligny center, nogrid, novisualpadding"));
        
        messageLabel = new JLabel();
        
        infoButton = new IconButton(infoIcon);
        infoButton.setActionCommand(DownloadActionHandler.INFO_COMMAND);
        infoButton.setToolTipText(I18n.tr("Info"));
        infoButton.addActionListener(new ButtonListener());
        
        add(messageLabel, "");
        add(infoButton, "");
    }

    @Override
    protected Component doTableCellEditorComponent(JTable table, Object value, 
            boolean isSelected, int row, int column) {
        if (value instanceof DownloadItem) {
            // Save download item for use by button listener.
            downloadItem = (DownloadItem) value;
            update(downloadItem);
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof DownloadItem) {
            update((DownloadItem) value);
            return this;
        } else {
            return emptyPanel;
        }
    }

    /**
     * Updates the display components using the specified download item.
     */
    private void update(DownloadItem item) {
        DownloadState state = item.getState();
        
        resources.decorateComponent(messageLabel);
        messageLabel.setText(getPercentMessage(item) + getMessage(item));
        if (state == DownloadState.DANGEROUS ||
                state == DownloadState.THREAT_FOUND ||
                state == DownloadState.SCAN_FAILED) {
            messageLabel.setForeground(resources.getDisabledForeground());
        }
        
        infoButton.setVisible(item.getDownloadItemType() == DownloadItemType.ANTIVIRUS ||
                item.getDownloadItemType() == DownloadItemType.BITTORRENT ||
                state == DownloadState.DANGEROUS ||
                state == DownloadState.SCANNING ||
                state == DownloadState.SCANNING_FRAGMENT || 
                state == DownloadState.THREAT_FOUND ||
                state == DownloadState.SCAN_FAILED);
    }
    
    /**
     * Returns the percent complete as a string.
     */
    private String getPercentMessage(DownloadItem item) {
        int percent = item.getPercentComplete();
        DownloadState state = item.getState();
        if (percent == 0 || state.isFinished() ||
                state == DownloadState.DOWNLOADING ||
                state == DownloadState.ERROR ||
                state == DownloadState.SCANNING || 
                state == DownloadState.APPLYING_DEFINITION_UPDATE) {
            return "";
        }
        return percent + "% - ";    
    }
    
    /**
     * @return the message displayed after the percentage. Can be null for non-covered states.
     */
    private String getMessage(DownloadItem item) {
        switch (item.getState()) {
        case RESUMING:
            return I18n.tr("Resuming");
        case CANCELLED:
            return I18n.tr("Cancelled");
        case FINISHING:
            return I18n.tr("Finishing...");
        case DONE:
            return I18n.tr("Done");
        case CONNECTING:
            Collection<RemoteHost> hosts = item.getRemoteHosts();
            if(hosts.size() == 0){
                return I18n.tr("Connecting...");
            }
            //{0}: 1 person, 2 people, etc
            return I18n.tr("Connecting to {0}", getPeopleText(item));
        case DOWNLOADING:            
            // {0}: current size
            // {1}: total size
            // {2}: download speed
            // {3}: download source
            if(item.getDownloadSourceCount() == 0){
                return I18n.tr("{0} of {1} ({2})",
                        GuiUtils.formatUnitFromBytes(item.getCurrentSize()), 
                        GuiUtils.formatUnitFromBytes(item.getTotalSize()),
                        GuiUtils.formatKilobytesPerSec(item.getDownloadSpeed()));
            } else { 
                return I18n.tr("{0} of {1} ({2}) from {3}",
                    GuiUtils.formatUnitFromBytes(item.getCurrentSize()), 
                    GuiUtils.formatUnitFromBytes(item.getTotalSize()),
                    GuiUtils.formatKilobytesPerSec(item.getDownloadSpeed()), 
                    getPeopleText(item));
            }
        case TRYING_AGAIN:
            return getTryAgainMessage(item.getRemainingTimeInState());
        case STALLED:
        	 if(item.getDownloadItemType() == DownloadItemType.BITTORRENT) {
                return I18n.tr("Error downloading torrent");
             } else {
             	return I18n.tr("Stalled - {0} of {1}", 
                    GuiUtils.formatUnitFromBytes(item.getCurrentSize()),
                    GuiUtils.formatUnitFromBytes(item.getTotalSize()));
             }
        case ERROR:         
            if(item.getErrorState() == ErrorState.INVALID) {
                return I18n.tr(item.getErrorState().getMessage());
            } else {
                return I18n.tr("Unable to download: ") + I18n.tr(item.getErrorState().getMessage());
            }
        case PAUSED:
            // {0}: current size, {1} total size
            return I18n.tr("Paused - {0} of {1}", 
                    GuiUtils.formatUnitFromBytes(item.getCurrentSize()), GuiUtils.formatUnitFromBytes(item.getTotalSize()));
        case LOCAL_QUEUED:
            return getQueueTimeMessage(item.getRemainingTimeInState());
        case REMOTE_QUEUED:
            if(item.getRemoteQueuePosition() == -1 || item.getRemoteQueuePosition() == Integer.MAX_VALUE){
                return getQueueTimeMessage(item.getRemainingTimeInState());
            }
            return I18n.trn("Waiting - Next in line",
                    "Waiting - {0} in line",
                    item.getRemoteQueuePosition(), item.getRemoteQueuePosition());
        case DANGEROUS:
            return I18n.tr("File deleted - Dangerous file");
        case SCANNING:
            return I18n.tr("Scanning for viruses - Powered by AVG");
        case SCANNING_FRAGMENT:
            return I18n.tr("Scanning preview - Powered by AVG");
        case THREAT_FOUND:
            return I18n.tr("File deleted - Threat detected by AVG");
        case SCAN_FAILED:
            return I18n.tr("Done, but unable to scan for viruses");
        case APPLYING_DEFINITION_UPDATE:
            AntivirusUpdateType type = (AntivirusUpdateType)item.getDownloadProperty(DownloadPropertyKey.ANTIVIRUS_UPDATE_TYPE);
            if (type == AntivirusUpdateType.CHECKING) {
                return I18n.tr("Evaluating...");
            }
       return I18n.tr("Applying update...");
        default:
            return null;
        }
        
    }
    
    private String getTryAgainMessage(long tryingAgainTime) {
        if(tryingAgainTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Looking for file...");                
        } else {
            return I18n.tr("Looking for file ({0} left)", CommonUtils.seconds2time(tryingAgainTime));
        }
    }
    
    private String getPeopleText(DownloadItem item) {
        Collection<RemoteHost> hosts = item.getRemoteHosts();
        if (hosts.size() == 0) {
            //checking sources to support showing the number of bit torrent hosts.
            int downloadSourceCount = item.getDownloadSourceCount();
            if(downloadSourceCount < 1) {
                return I18n.tr("nobody");
            } else {
                return I18n.trn("{0} P2P User", "{0} P2P Users", downloadSourceCount);
            }
        } else if (hosts.size() == 1) {

            Friend friend = hosts.iterator().next().getFriendPresence().getFriend();
            if (friend.isAnonymous()) {
                return I18n.tr("1 P2P User");
            } else {
                return friend.getRenderName();
            }

        } else {
            boolean hasP2P = false;
            boolean hasFriend = false;
            
            for (RemoteHost host : hosts) {                
                if (host.getFriendPresence().getFriend().isAnonymous()) {
                    hasP2P = true;
                } else {
                    hasFriend = true;
                }
                
                if (hasP2P && hasFriend) {
                    // We found both.  We're done.
                    break;
                }
            }
            if (hasP2P && hasFriend ) {
                return I18n.trn("{0} Person", "{0} People", hosts.size());
            } else if (hasP2P) {
                return I18n.trn("{0} P2P User", "{0} P2P Users", hosts.size());
            } else {
                //just friends
                return I18n.trn("{0} Friend", "{0} Friends", hosts.size());
            }
        }
    }
    
    private String getQueueTimeMessage(long queueTime){
        //if(queueTime == DownloadItem.UNKNOWN_TIME){
            return I18n.tr("Waiting...");                
        //} else {
        //    return I18n.tr("Waiting...", CommonUtils.seconds2time(queueTime));
        //}
    }

    /**
     * Action listener for editor buttons.
     */
    private class ButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset cursor if source component is IconButton.  If the action
            // displays a modal dialog, then IconButton does not receive the
            // mouseExited event to reset the default cursor.
            if (e.getSource() instanceof IconButton) {
                ((IconButton) e.getSource()).resetDefaultCursor();
            }
            
            actionHandler.performAction(e.getActionCommand(), downloadItem);
            cancelCellEditing();
        }
    }
}
