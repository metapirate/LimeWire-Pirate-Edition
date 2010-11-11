package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.warnings.LibraryWarningController;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AddFileAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    private final LibraryWarningController librarySupport;
    
    @Inject
    public AddFileAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel, LibraryWarningController librarySupport) {
        super(I18n.tr("Add Files"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.librarySupport = librarySupport;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final List<File> files = FileChooser.getInput(GuiUtils.getMainFrame(), I18n.tr("Add File(s)"), 
                I18n.tr("Add File(s)"), FileChooser.getLastInputDirectory(),
                JFileChooser.FILES_AND_DIRECTORIES, JFileChooser.APPROVE_OPTION, true,
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.exists();
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr("All Files and Folders");
                    }
                });

        if (files != null) {
            LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
            if(item != null) {
                librarySupport.addFiles(item.getLocalFileList(), files);
            }                    
        }
    } 
}
