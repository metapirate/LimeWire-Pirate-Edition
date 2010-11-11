package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.properties.FileInfoTabPanel.FileInfoTabListener;
import org.limewire.ui.swing.properties.FileInfoTabPanel.Tabs;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A Dialog for displaying FileInfo about a file. This dialog changes 
 * depending on the state of the file and the location of the file. 
 */
public class FileInfoDialog extends LimeJDialog {
    
    /**
     * Type of FileInfoDialog that can be created.
     */
    public static enum FileInfoType {
        /**
         * File exists within the current LimeWire.
         */
        LOCAL_FILE,
        
        /**
         * File is in the process of downloading or a portion
         *  of the file has been downloaded already,
         */
        DOWNLOADING_FILE,
        
        /**
         * File it contained on one or more networked computers but not
         *  within this client.
         */
        REMOTE_FILE,
        
        /**
         * The file represents a current upload.
         */
        UPLOADING_FILE;
    }
    
    @Resource private Color backgroundColor;

    private final LibraryManager libraryManager;
    
    private final FileInfoTabPanel tabPanel;
    private final FileInfoOverviewPanel overviewPanel;
    private final JPanel cardPanel;
    private final Map<Tabs, FileInfoPanel> cards;
    private JButton okButton;
    private PropertiableFile propertiableFile;
    private RenameListener renameListener;
    
    @Inject
    public FileInfoDialog(@Assisted PropertiableFile propertiable, @Assisted final FileInfoType type,
                        FileInfoTabPanel fileInfoTabPanel, final FileInfoPanelFactory fileInfoFactory,
                        LibraryManager libraryManager) {
        super(GuiUtils.getMainFrame());
        
        this.libraryManager = libraryManager;
        
        tabPanel = fileInfoTabPanel;
        cardPanel = new JPanel(new BorderLayout());
        cardPanel.setPreferredSize(new Dimension(400,600));
        cards = new HashMap<Tabs, FileInfoPanel>();
        this.propertiableFile = propertiable;
        
        GuiUtils.assignResources(this);
        
        cardPanel.setOpaque(false);
        createTabs(propertiableFile, type);
        cards.put(Tabs.GENERAL, fileInfoFactory.createGeneralPanel(type, propertiableFile));
        cardPanel.add(cards.get(Tabs.GENERAL).getComponent());
        
        updateTitle(propertiableFile.getFileName());
        
        setLayout(new MigLayout("gap 0, insets 0, fill"));
        getContentPane().setBackground(backgroundColor);

        overviewPanel = (FileInfoOverviewPanel) fileInfoFactory.createOverviewPanel(type, propertiableFile);
        
        add(overviewPanel.getComponent(), "dock north");
        add(tabPanel.getComponent(), "dock north");
        
        add(cardPanel, "gapleft 10, grow, gapright 10");
        createFooter();
    
        setPreferredSize(new Dimension(500,565));
        setDefaultCloseOperation(FileInfoDialog.DISPOSE_ON_CLOSE);

        if (type == FileInfoType.LOCAL_FILE) {
            // TODO: In KDE there is a problem where minimise is possible and
            //  a minimised modal dialogue can't be restored.  The dialogue
            //  disappears and control can never be restored to the rest of the
            //  app.  Stopping resizability prevents this problem.
            setModalityType(ModalityType.APPLICATION_MODAL);
        }
        pack();

        setLocationRelativeTo(GuiUtils.getMainFrame());

        // listen to the visibility setting/ disposes of the dialog when it becomes invisible
        addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                dispose();
            }

            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {
                if(okButton != null)
                    okButton.requestFocusInWindow();
            }
        });  

        renameListener = new RenameListener(libraryManager);
        libraryManager.getLibraryManagedList().addPropertyChangeListener(renameListener);
        
        tabPanel.addSearchTabListener(new FileInfoTabListener(){
            @Override
            public void tabSelected(Tabs tab) {
                if(!cards.containsKey(tab)) {
                    if(tab == Tabs.GENERAL) {
                        cards.put(tab, fileInfoFactory.createGeneralPanel(type, propertiableFile));
                    } else if(tab == Tabs.SHARING) {
                        cards.put(tab, fileInfoFactory.createSharingPanel(type, propertiableFile));
                    } else if(tab== Tabs.LIMITS) {
                        Torrent torrent = (Torrent) propertiableFile.getProperty(FilePropertyKey.TORRENT);
                        
                        if (torrent != null && torrent.isEditable()) {
                            cards.put(tab, fileInfoFactory.createLimitsPanel(type, propertiableFile));
                        }
                    } else if (tab == Tabs.TRACKERS) {
                        cards.put(tab, fileInfoFactory.createTrackersPanel(type, propertiableFile));
                    } else if(tab == Tabs.TRANSFERS) {
                        cards.put(tab, fileInfoFactory.createTransferPanel(type, propertiableFile));    
                    } else if(tab == Tabs.BITTORENT) {
                        Torrent torrent = (Torrent) propertiableFile.getProperty(FilePropertyKey.TORRENT);
                        
                        //we can't show the file info panel for torrents without metadata.
                        if(torrent != null && torrent.hasMetaData()) {
                            cards.put(tab, fileInfoFactory.createBittorentPanel(torrent));
                        }
                    } else if (tab == Tabs.PIECES) {
                        if(propertiableFile instanceof DownloadItem) {
                            cards.put(tab, fileInfoFactory.createPiecesPanel(type, (DownloadItem)propertiableFile));    
                        } 
                        else {
                            // TODO: Pieces should still be available in some cases for torrents..
                            //        Need to overhaul file info.
                        }
                    } else {
                        throw new IllegalStateException("Unknown state:" + tab);
                    }
                }
                cardPanel.removeAll();
                cardPanel.add(cards.get(tab).getComponent());
                FileInfoDialog.this.validate();
                FileInfoDialog.this.repaint();
            }
        });
    }
    
    @Override
    public void dispose() {
        overviewPanel.dispose();
        //unregister any listeners used and dispose of dialog when made invisible
        for(FileInfoPanel panel : cards.values()) {
            panel.dispose();
        }
        libraryManager.getLibraryManagedList().removePropertyChangeListener(renameListener);
        
        super.dispose();
    }
    
    private void updateTitle(String fileName) {
        setTitle(I18n.tr("{0} Properties", propertiableFile.getFileName()));
    }
    
    public void renameFile() {
        overviewPanel.enableRename();
    }
    
    private void createTabs(PropertiableFile propertiableFile, final FileInfoType type) {
        List<Tabs> tabs = new ArrayList<Tabs>();
        // general tab is always shown
        tabs.add(Tabs.GENERAL);
        
        Torrent torrent = (Torrent)propertiableFile.getProperty(FilePropertyKey.TORRENT);

        if (torrent != null) {
            if (torrent.isEditable()) {
                tabs.add(Tabs.LIMITS);
            }

            if (torrent.hasMetaData()) {
                tabs.add(Tabs.BITTORENT);
                tabs.add(Tabs.TRACKERS);
            }

            if (torrent.isEditable()) {
                tabs.add(Tabs.TRANSFERS);
                
                // TODO: Pieces should still be available in some cases for torrents..
                //        Need to overhaul file info.
                if (type == FileInfoType.DOWNLOADING_FILE) {
                    tabs.add(Tabs.PIECES);
                }
            }
        } else {
            if (type == FileInfoType.LOCAL_FILE) {
                tabs.add(Tabs.SHARING);
            }
            if (type != FileInfoType.REMOTE_FILE) {
                tabs.add(Tabs.TRANSFERS);
            }
            if (type == FileInfoType.DOWNLOADING_FILE) {
                tabs.add(Tabs.PIECES);
            }
        }
        
        tabPanel.setTabs(tabs);
    }
    
    /**
     * Adds a footer with the cancel/ok button to close the dialog.
     */
    private void createFooter() {
        okButton = new JButton(new OKAction());
        JPanel footerPanel = new JPanel(new MigLayout("fill, insets 0 15 10 15"));
        footerPanel.add(okButton, "alignx right, aligny bottom, split, tag ok");
        footerPanel.add(new JButton(new CancelAction()), "aligny bottom, tag cancel");
        footerPanel.setBackground(backgroundColor);
        
        add(footerPanel, "grow, south");
    }

    /**
     * Closes the dialog and saves any data that may have changed.
     */
    private class OKAction extends AbstractAction {
        public OKAction() {
            super(tr("OK"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            overviewPanel.save();
            
            for(FileInfoPanel panel : cards.values()) {
                panel.save();
            }
            setVisible(false);
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
            setVisible(false);
        }
    }
    
    /**
	 * Listens for rename events. If this file was renamed, 
     * updates all the created panels with the new 
     * propertiable file since the underlying FileDesc
     * has changed.
 	 */
    private class RenameListener implements PropertyChangeListener {
        private LibraryManager libraryManager;
        
        public RenameListener(LibraryManager libraryManager) {
            this.libraryManager = libraryManager;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // if its a rename event, check if this propertiable file name changed.
            if(evt.getPropertyName().equals("rename")) {
                // if this file name changed, update the propertiable file on all the created panels so
                // they can properlly save
                if( propertiableFile instanceof LocalFileItem && ((LocalFileItem)propertiableFile).getFile().equals(evt.getOldValue())) {
                    propertiableFile = libraryManager.getLibraryManagedList().getFileItem((File)evt.getNewValue());
                    for(FileInfoPanel panel : cards.values()) {
                        panel.updatePropertiableFile(propertiableFile);
                    }
                    overviewPanel.updatePropertiableFile(propertiableFile);
                    updateTitle(propertiableFile.getFileName());
                }
            }
        }
    }
}
