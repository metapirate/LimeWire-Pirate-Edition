package org.limewire.ui.swing.library.table;

import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DeletionKeyListener extends KeyAdapter {
    
    private final LibraryManager libraryManager;
    private final DownloadListManager downloadListManager;
    private final Provider<PlayerMediator> playerMediator;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;    
    
    @Inject
    public DeletionKeyListener(LibraryManager libraryManager, 
            DownloadListManager downloadListManager,
            Provider<PlayerMediator> playerMediator,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
        this.playerMediator = playerMediator;
        this.selectedLocalFileItems = selectedLocalFileItems;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            final List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
                                
            String title = null;
            String message = null;
            String deleteText = null;
            String removeText = I18n.tr("&Remove from Library");
            String cancelText = I18n.tr("&Cancel");
           
            if (OSUtils.isWindows() && OSUtils.supportsTrash()) {
                title = I18n.trn("Move File to the Recycle Bin or Remove from Library", "Move Files to the Recycle Bin or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to move this file to the Recycle Bin or just remove it from the Library?", 
                        "Do you want to move this file to the Recycle Bin or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("&Move to Recycle Bin");
            }
            else if (OSUtils.isMacOSX() && OSUtils.supportsTrash()) {
                title = I18n.trn("Move File to the Trash or Remove from Library", "Move Files to the Trash or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to move this file to the Trash or just remove it from the Library?", 
                        "Do you want to move this file to the Trash or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("&Move to Trash");
            }
            else {
                title = I18n.trn("Delete File or Remove from Library", "Delete Files or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to delete this file from disk or just remove it from the Library?",
                        "Do you want to delete this file from disk or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("&Delete from Disk");
                
            }
            
            JButton deleteButton = new JButton(new DeletionOptionAction(deleteText) {
                @Override
                public void doAction(ActionEvent e) {
                    DeleteAction.deleteSelectedItems(libraryManager, playerMediator.get(),
                            downloadListManager, selectedItems);
                }
            });

            JButton removeButton = new JButton(new DeletionOptionAction(removeText) {
                @Override
                public void doAction(ActionEvent e) {
                    RemoveFromLibraryAction.removeFromLibrary(libraryManager, playerMediator.get(),
                            selectedItems);
                }
            });

            JButton cancelButton = new JButton(new DeletionOptionAction(cancelText));

            /*
             * Configure the option buttons focus traversal keys to also
             * navigate forward using RIGHT arrow key
             */
            final Set<AWTKeyStroke> forwardTraversalSet = new HashSet<AWTKeyStroke>(removeButton
                    .getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
            forwardTraversalSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_RIGHT, 0));
            
            removeButton.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalSet);
            deleteButton.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalSet);
            cancelButton.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardTraversalSet);
            
            /*
             * Configure the option buttons focus traversal keys to also
             * navigate backward using LEFT arrow key
             */
            final Set<AWTKeyStroke> backwardTraversalSet = new HashSet<AWTKeyStroke>(removeButton
                    .getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
            backwardTraversalSet.add(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_LEFT, 0));
            
            removeButton.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardTraversalSet);
            deleteButton.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardTraversalSet);
            cancelButton.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardTraversalSet);
            
            Object[] options = new Object[] { removeButton, deleteButton, cancelButton };

            FocusJOptionPane.showOptionDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, cancelButton);
        }
    }

    /**
     * Action for the option buttons defined in
     * {@link #DeletionKeyListener}. Once a button is pressed, the
     * listener obtains the parent dialog, closes it and lets the caller take
     * over with the proper action, by overriding the <code>doAction</code> if
     * necessary.
     * 
     * @see #doAction(ActionEvent)
     */
    private class DeletionOptionAction extends AbstractAction {
       
        DeletionOptionAction(String actionName) {
            super(actionName);
        }

        /**
         * Obtains the parent dialog for the option pane, and disposes it.
         * Then, calls <code>doAction</code> for additional behavior.
         * @see #doAction(ActionEvent)
         */
        @Override
        public final void actionPerformed(ActionEvent e) {
            JComponent eventSource = (JComponent) e.getSource();
            Window window = FocusJOptionPane.getWindowForComponent(eventSource);
            window.dispose();
            doAction(e);
        }

        /**
         * Override this method to add custom behavior, once the dialog is closed.
         * @see #actionPerformed(ActionEvent)
         */
        public void doAction(ActionEvent e) {
        }
    }
    
    
 
}
