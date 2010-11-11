package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.api.library.URNFactory;
import org.limewire.inject.AbstractModule;

import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(LibraryManagerImpl.class);
        bind(RemoteLibraryManager.class).to(RemoteLibraryManagerImpl.class);
        bind(SharedFileListManager.class).to(SharedFileListManagerImpl.class);
        bind(MagnetLinkFactory.class).to(MagnetLinkFactoryImpl.class);
        bind(URNFactory.class).to(URNFactoryImpl.class);
        bind(PresenceLibraryBrowser.class);
        bind(FriendSearcher.class);
        
        bind(CoreLocalFileItemFactory.class).toProvider(FactoryProvider.newFactory(CoreLocalFileItemFactory.class,CoreLocalFileItem.class));
        bind(MetaDataManager.class).to(MetaDataManagerImpl.class);
        bind(FriendAutoCompleterFactory.class).to(FriendAutoCompleterFactoryImpl.class);
        bind(LibraryData.class).to(LibraryDataImpl.class);
    }

}
