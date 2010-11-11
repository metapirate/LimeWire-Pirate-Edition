package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Removes the selected items from the selected SharedList
 */
public class RemoveFromListAction extends AbstractAction {
    private final Provider<LocalFileList> selectedLocalFileList;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;

    @Inject
    public RemoveFromListAction(@LibrarySelected Provider<LocalFileList> selectedLocalFileList,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        super(I18n.tr("Remove from List"));

        this.selectedLocalFileList = selectedLocalFileList;
        this.selectedLocalFileItems = selectedLocalFileItems;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final List<LocalFileItem> items = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        final LocalFileList localFileList = selectedLocalFileList.get();
        
        BackgroundExecutorService.execute(new Runnable() {
            public void run() {
                for(LocalFileItem item : items) {
                    localFileList.removeFile(item.getFile());
                }
            }
        });
    }
}


