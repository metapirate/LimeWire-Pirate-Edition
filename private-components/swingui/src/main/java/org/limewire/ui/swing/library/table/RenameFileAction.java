package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.properties.FileInfoDialog;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Handles renaming a file. Opens the FileInfo dialog and
 * enables editing on the file name within the dialog.
 */
public class RenameFileAction extends AbstractAction {

    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final FileInfoDialogFactory factory;
    
    @Inject
    public RenameFileAction(Provider<LibraryManager> libraryManager, FileInfoDialogFactory factory,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        super(I18n.tr("Rename"));
        
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.factory = factory;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final FileInfoDialog dialog = factory.createFileInfoDialog(selectedLocalFileItems.get().get(0), FileInfoType.LOCAL_FILE);
        dialog.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentShown(ComponentEvent e) {
                dialog.renameFile();
            }
            
        });
        dialog.setVisible(true);
    }
}
