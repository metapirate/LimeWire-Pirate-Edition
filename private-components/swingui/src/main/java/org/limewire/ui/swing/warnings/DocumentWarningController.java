package org.limewire.ui.swing.warnings;

import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.Disposable;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Has logic one whether or not to display a warning to the user that they are
 * sharing documents with a public shared list. If the user shares a document
 * with a public shared list, then a DocumentWarningPanel will be constructed
 * and attached to the GlobalLayeredPanel.
 * 
 * If the user is setup to show this warning the warning will show up. If not
 * nothing will happen and this class will not register listeners to find out
 * about this event either.
 * 
 * The user is presented with two options, to continue sharing, in which case
 * the user will no longer receive a warning message until the next time they
 * upgrade, or they can use to remove all the documents shared with gnutella.
 * 
 * Choosing the remove option will also disable the capability to share
 * documents with gnutella. If they re-enable sharing documents, then share a
 * document they will see the warning again, in this instance.
 */
@Singleton
public class DocumentWarningController implements ComponentListener {
    private boolean showing = false;

    private final Provider<DocumentWarningPanel> documentWarningPanel;
    private final CategoryManager categoryManager;

    @Inject
    public DocumentWarningController(Provider<DocumentWarningPanel> documentWarningPanel, CategoryManager categoryManager) {
        this.documentWarningPanel = documentWarningPanel;
        this.categoryManager = categoryManager;
    }

    @Inject
    public void register(SharedFileListManager sharedFileListManager) {
        if (SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD.get()) {
            
            ListEventListener<LocalFileItem> listener = new ListEventListener<LocalFileItem>() {
                @Override
                public void listChanged(ListEvent<LocalFileItem> listChanges) {
                    while (listChanges.next()) {
                        if (listChanges.getType() == ListEvent.INSERT
                                || listChanges.getType() == ListEvent.UPDATE) {
                            LocalFileItem localFileItem = listChanges
                                    .getSourceList()
                                    .get(listChanges.getIndex());
                            if (categoryManager.getCategoryForFile(localFileItem.getFile()) == Category.DOCUMENT
                                    && SharingSettings.WARN_SHARING_DOCUMENTS_WITH_WORLD
                                            .getValue()) {
                                showDocumentSharingWarning();
                            }
                        }
                    }
                }
            };
            
            sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
            try {
                for (SharedFileList shareList : sharedFileListManager.getModel()) {
                    if (shareList.isPublic()) {
                        shareList.getSwingModel().addListEventListener(listener);
                    }
                }
            } finally {
                sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
            }
        }
    }

    private void showDocumentSharingWarning() {
        if (!showing) {
            showing = true;
            
            DocumentWarningPanel panel = documentWarningPanel.get();
            // component hidden event comes in to tell us we can show more
            // warnings.
            panel.addComponentListener(this);
        }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        Component panel = e.getComponent();
        if (panel instanceof Disposable) {
            showing = false;
            ((Disposable) panel).dispose();
        }
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentResized(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

}
