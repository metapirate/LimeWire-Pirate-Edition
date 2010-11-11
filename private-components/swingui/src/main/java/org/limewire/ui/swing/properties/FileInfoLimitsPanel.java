package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * File info panel for downloads and uploads which provides means for limiting bandwith and
 *  related things on an individual level.
 */
public class FileInfoLimitsPanel implements FileInfoPanel {
    
    @Resource private Color foreground;
    @Resource private Font headerFont;
    
    private final TorrentManagerSettings torrentSettings;
    private final PropertiableFile propertiableFile;
    
    private final JPanel component;
    private TorrentManagementPanel torrentManagementPanel;
    
    private void createTorrentSettings() {
        Torrent torrent = (Torrent)propertiableFile.getProperty(FilePropertyKey.TORRENT);
        
        component.add(createHeaderLabel(I18n.tr("Torrent Settings")), "span, gaptop 15, wrap");
            
        torrentManagementPanel = new TorrentManagementPanel(torrent, torrentSettings);
        component.add(torrentManagementPanel.getComponent(), "span, wrap");
    }
    
    public FileInfoLimitsPanel(PropertiableFile propertiableFile, TorrentManagerSettings torrentSettings) {

        this.torrentSettings = torrentSettings;
        this.propertiableFile = propertiableFile;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        createTorrentSettings();
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return torrentManagementPanel.hasChanged();
    }

    @Override
    public void save() {
        if (torrentManagementPanel.hasChanged())
            torrentManagementPanel.save();
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
    }

    @Override
    public void dispose() {
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        label.setForeground(foreground);
        return label;
    }

}
