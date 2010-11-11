/**
 * 
 */
package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Locates the selected file on disk.
 */
class LocateFileAction extends AbstractAction {
    
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
        
    @Inject
    public LocateFileAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        super(I18n.tr("Locate on Disk"));

        this.selectedLocalFileItems = selectedLocalFileItems;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if(localFileItems.size() > 0) {
            LocalFileItem item = localFileItems.get(0);
            NativeLaunchUtils.launchExplorer(item.getFile());
        }
    }
}