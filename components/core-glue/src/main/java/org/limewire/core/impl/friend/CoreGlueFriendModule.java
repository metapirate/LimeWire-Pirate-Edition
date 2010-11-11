package org.limewire.core.impl.friend;

import org.limewire.core.api.friend.FileMetaDataConverter;
import org.limewire.friend.api.LimeWireFriendModule;
import org.limewire.friend.impl.LimeWireFriendImplModule;

import com.google.inject.AbstractModule;

public class CoreGlueFriendModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireFriendModule());
        install(new LimeWireFriendImplModule());
        bind(FriendFirewalledAddressConnector.class).asEagerSingleton();
        bind(FriendRemoteFileDescCreator.class).asEagerSingleton();
        bind(CoreGlueFriendService.class);
        bind(FileMetaDataConverter.class).to(FileMetaDataConverterImpl.class);
        bind(FriendShareListRefresher.class);
        bind(FriendPresenceLibraryAdder.class);
    }

}
