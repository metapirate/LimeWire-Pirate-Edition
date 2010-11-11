package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Removes the selected file(s) from the library. 
 */
class RemoveFromLibraryAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final LibraryManager libraryManager;
    private final SharedFileListManager sharedFileListManager;
    private final Provider<PlayerMediator> playerMediator;
    
    @Inject
    public RemoveFromLibraryAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems, 
            LibraryManager libraryManager, SharedFileListManager sharedFileListManager,
            Provider<PlayerMediator> playerMediator) {
        super(I18n.tr("All Lists and Library"));
        
        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryManager = libraryManager;
        this.sharedFileListManager = sharedFileListManager;
        this.playerMediator = playerMediator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {       
        List<LocalFileItem> selected = selectedLocalFileItems.get();
        
        String removeText = I18n.tr("Remove");
        String cancelText = I18n.tr("Cancel");
        
        Object[] options = new Object[] {removeText, cancelText};
        
        int confirmation = FocusJOptionPane.showOptionDialog(null, 
                getMessage(selected), I18n.trn("Remove File","Remove Files", selected.size()), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, cancelText);
        
        if (confirmation > -1 && options[confirmation] == removeText) {
            removeFromLibrary(libraryManager, playerMediator.get(), selected);
        }
    }
    
    private String getMessage(List<LocalFileItem> list) {
        if(list.size() == 1) {
            if(containsSharedFile(list))
                return I18n.tr("Remove \"{0}\" from Library? This will unshare this file with everyone.", list.get(0).getFileName());
            else 
                return I18n.tr("Remove \"{0}\" from Library?", list.get(0).getFileName());
        } else {
            if(containsSharedFile(list))
                return I18n.tr("Remove {0} files from Library? This will unshare these files with everyone.", list.size());
            else
                return I18n.tr("Remove {0} files from Library?", list.size());
        }
    }
    
    /**
     * Returns true if at least one file list this list is contained in a 
     * FileList that is shared with at least one person.
     */
    private boolean containsSharedFile(List<LocalFileItem> list) {
        boolean hasSharedFile = false;
        
        sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
        try {
            for(SharedFileList sharedFileList : sharedFileListManager.getModel()) {
                if(sharedFileList.getFriendIds().size() > 0) {
                    for(LocalFileItem item : list) {
                        if(sharedFileList.contains(item.getFile())) {
                            hasSharedFile = true;
                            break;
                        }
                    }
                }
            }
        } finally {
            sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
        }
        return hasSharedFile;
    }
    
    public static void removeFromLibrary(final LibraryManager libraryManager, final PlayerMediator playerMediator,
            final List<LocalFileItem> selected) {
        File currentSong = playerMediator.getCurrentMediaFile();
        
        final List<File> toRemove = new ArrayList<File>(selected.size());
        for(LocalFileItem item : selected) {
            if(item.getFile().equals(currentSong)){
                playerMediator.stop();
            }
            if(!item.isIncomplete()) {
                toRemove.add(item.getFile());
            }
        }

        if(!toRemove.isEmpty()) {
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    for(File file : toRemove) {
                        libraryManager.getLibraryManagedList().removeFile(file);
                    }
                }
            });
        }
    }
}