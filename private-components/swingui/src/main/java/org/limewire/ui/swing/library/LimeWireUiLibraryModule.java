package org.limewire.ui.swing.library;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.listener.EventListenerList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.sharing.LibrarySharingEvent;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // Bind listener support for LibrarySharingEvent.
        EventListenerList<LibrarySharingEvent> listenerList = new EventListenerList<LibrarySharingEvent>();
        bind(new TypeLiteral<EventListenerList<LibrarySharingEvent>>(){}).toInstance(listenerList);
    }
    
    @Provides @LibrarySelected LocalFileList selectedLFL(Provider<LibraryNavigatorPanel> navigator) {
        LibraryNavItem item = navigator.get().getSelectedNavItem();
        if (item != null) {
            return item.getLocalFileList();
        } else {
            return null;
        }
    }
    
    @Provides @LibrarySelected List<File> selectedFiles(Provider<LibraryPanel> libraryPanel) {
        return libraryPanel.get().getSelectedFiles();
    }
    
    @Provides @LibrarySelected List<LocalFileItem> selectedFileItems(Provider<LibraryPanel> libraryPanel) {
        return libraryPanel.get().getSelectedItems();
    }
    
    @Provides @LibraryTableRect Rectangle tableRect(Provider<LibraryPanel> libraryPanel) {
        return libraryPanel.get().getTableListRect();
    }

}
