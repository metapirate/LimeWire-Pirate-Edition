package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

/**
 * Attempts to write the audio files within the selected
 * LocalFileList as an M3U list. 
 */
class ExportListAction  extends AbstractAction {

    private final LibraryNavigatorPanel libraryNavigatorPanel;
    
    @Inject
    public ExportListAction(LibraryNavigatorPanel libraryNavigatorPanel) {
        super(I18n.tr("Export List..."));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem item = libraryNavigatorPanel.getSelectedNavItem();
        
        File file = FileChooser.getSaveAsFile(GuiUtils.getMainFrame(),
                I18n.tr("Export M3U List"), 
                new File(FileChooser.getLastInputDirectory(), getSuggestedFileName(item.getDisplayText())),
                new M3UFileFilter());
        //TODO: check for overwrite
        if (file == null) {
            return;
        }
        M3UList m3uList = new M3UList(file, item.getLocalFileList());
        m3uList.save();
    }
    
    /**
     * Suggests the SharedFileList name to be the m3u fileName.
     */
    private String getSuggestedFileName(String displayName) {
        //TODO: remove illegal filename chars
        String ext = FileUtils.getFilenameNoExtension(displayName);
        if(ext.compareToIgnoreCase("m3u") == 0)
            return displayName;
        else
            return displayName + ".m3u";
    }
    
    /**
     * Displays only directories and M3U lists in the 
     * File Chooser.
     */
    private static class M3UFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() ||
                   f.getName().toLowerCase().endsWith("m3u");
        }

        @Override
        public String getDescription() {
            return I18n.tr("Playlist Files (*.m3u)");
        }
    }
}
