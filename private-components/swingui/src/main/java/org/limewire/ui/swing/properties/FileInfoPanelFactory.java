package org.limewire.ui.swing.properties;

import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;

/**
 * Creates subPanels for displaying information about a PropertiableFile. 
 */
public interface FileInfoPanelFactory {

    public FileInfoPanel createGeneralPanel(FileInfoType type, PropertiableFile propertiableFile);
    
    public FileInfoPanel createOverviewPanel(Torrent torrent);
    
    public FileInfoPanel createOverviewPanel(FileInfoType type, PropertiableFile propertiableFile);
    
    public FileInfoPanel createTrackersPanel(FileInfoType type, PropertiableFile propertiableFile);
    
    public FileInfoPanel createTransferPanel(FileInfoType type, PropertiableFile propertiableFile);
    
    public FileInfoPanel createSharingPanel(FileInfoType type, PropertiableFile propertiableFile);
    
    public FileInfoBittorrentPanel createBittorentPanel(Torrent torrent);
    
    public FileInfoPanel createPiecesPanel(FileInfoType type, DownloadItem propertiableFile);

    public FileInfoPanel createLimitsPanel(FileInfoType type, PropertiableFile propertiableFile);
}
