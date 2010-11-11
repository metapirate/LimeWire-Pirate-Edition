package org.limewire.ui.swing.properties;

import javax.swing.JDialog;

import org.limewire.bittorrent.Torrent;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class TorrentDownloadSelector extends LimeJDialog {
    
    public static int showBittorrentSelector(Torrent torrent, FileInfoPanelFactory factory) {

        TorrentSelectorPanel panel = new TorrentSelectorPanel(torrent, factory);
        
        //this forces LW to take over focus, even if it was hidden before.
        GuiUtils.getMainFrame().setAlwaysOnTop(true);
        GuiUtils.getMainFrame().setAlwaysOnTop(false);
        
        JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("Torrent Download"), 
                null, panel.getComponent());
        dialog.getRootPane().setDefaultButton(panel.getOkButton());
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setResizable(true);
        dialog.setVisible(true);
        dialog.dispose();
        return panel.getCloseValue();
    }
}
