package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Attempts to import a given M3U list into the
 * LocalFileList that is currently selected.
 */
class ImportListAction extends AbstractAction {

    private final LibraryNavigatorPanel libraryNavigatorPanel;
    
    @Inject
    public ImportListAction(LibraryNavigatorPanel libraryNavigatorPanel) {
        super(I18n.tr("Import List..."));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem item = libraryNavigatorPanel.getSelectedNavItem();
        
        File file = FileChooser.getInputFile(GuiUtils.getMainFrame(),
                I18n.tr("Import M3U List"), 
                I18n.tr("Load"),
                FileChooser.getLastInputDirectory(),
                new M3UFileFilter());
        //TODO: check for overwrite
        if (file == null) {
            return;
        }
        
        M3UList m3uList = new M3UList(file, item.getLocalFileList());
        m3uList.load();
    }
    
    /**
     * Only show directories and m3u lists in the File Chooser.
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
