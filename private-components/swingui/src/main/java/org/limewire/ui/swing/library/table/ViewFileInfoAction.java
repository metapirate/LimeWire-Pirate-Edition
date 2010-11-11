package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Opens the file info view for the given file. 
 */
class ViewFileInfoAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final FileInfoDialogFactory fileInfoFactory;
    
    @Inject
    public ViewFileInfoAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            FileInfoDialogFactory fileInfoFactory) {
        super(I18n.tr("View File Info..."));
        
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.fileInfoFactory = fileInfoFactory;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if(localFileItems.size() > 0) {
            JDialog dialog = fileInfoFactory.createFileInfoDialog(localFileItems.get(0), FileInfoType.LOCAL_FILE);
            dialog.setVisible(true);
        }
    }
}