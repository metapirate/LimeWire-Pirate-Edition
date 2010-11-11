package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class TorrentSelectorPanel implements PropertyChangeListener {
    
    @Resource private Color foreground;
    @Resource private Color backgroundColor;
    @Resource private Font headerFont;
    
    private final JPanel component;
   
    private FileInfoBittorrentPanel bittorrentPanel;
    private JButton okButton;
    private JCheckBox checkBox;
    private int closeValue = JOptionPane.CANCEL_OPTION;
    
    public TorrentSelectorPanel(Torrent torrent, FileInfoPanelFactory factory) {        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fill, gap 0, insets 0", "[grow]", "[][][grow][]"));
        component.setPreferredSize(new Dimension(440, 500));
        component.setBackground(backgroundColor);        
        
        bittorrentPanel = factory.createBittorentPanel(torrent);
        bittorrentPanel.addPropertyChangeListener(this);
        
        component.add(factory.createOverviewPanel(torrent).getComponent(), "growx, wrap, gapleft 5, gapright 5");
        component.add(createHeaderLabel(I18n.tr("Choose files to download")), "gapleft 5, wrap");
        component.add(bittorrentPanel.getComponent(), "grow");
        createFooter();
        component.addAncestorListener(new AncestorListener() {
           @Override
           public void ancestorAdded(AncestorEvent event) {
               okButton.requestFocusInWindow();
           }
           
           @Override
           public void ancestorMoved(AncestorEvent event) {
               
           };
           
           @Override
           public void ancestorRemoved(AncestorEvent event) {
               
           }
        });
    }
    
    public int getCloseValue() {
        return closeValue;
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    public JButton getOkButton() {
        return okButton;
    }
    
    private void close() {
        bittorrentPanel.dispose();
        Window window = SwingUtilities.getWindowAncestor(component);
        window.setVisible(false);
    }
    
    /**
     * Adds a footer with the cancel/ok button to close the dialog.
     */
    private void createFooter() {
        okButton = new JButton(new OKAction());
        checkBox = new JCheckBox(I18n.tr("Always ask before starting torrent"));
        checkBox.setOpaque(false);
        checkBox.setSelected(BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.getValue());
        
        JPanel footerPanel = new JPanel(new MigLayout("fill, insets 0 10 10 15"));
        footerPanel.add(checkBox, "span, wrap");
        footerPanel.add(okButton, "alignx right, aligny bottom, split, tag ok");
        footerPanel.add(new JButton(new CancelAction()), "aligny bottom, tag cancel");
        footerPanel.setOpaque(false);
        
        component.add(footerPanel, "grow, south");
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        label.setForeground(foreground);
        return label;
    }
    
    /**
     * Closes the dialog and saves any data that may have changed.
     */
    private class OKAction extends AbstractAction{
        public OKAction() {
            super(tr("OK"));
         }

        @Override
        public void actionPerformed(ActionEvent e) {
            closeValue = JOptionPane.OK_OPTION;
            bittorrentPanel.save();
            BittorrentSettings.TORRENT_SHOW_POPUP_BEFORE_DOWNLOADING.setValue(checkBox.isSelected());
            close();
        }
    }

    /**
     * Closes the data and does not save any data even if it
     * has changed.
     */
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            closeValue = JOptionPane.CANCEL_OPTION;
            close();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ( evt.getPropertyName().equals( FileInfoBittorrentPanel.TORRENT_FILE_ENTRY_SELECTED) )
        {
            okButton.setEnabled( ((Boolean) evt.getNewValue()).booleanValue() );
        }
    }
}
