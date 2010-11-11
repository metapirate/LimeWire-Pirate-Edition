package org.limewire.friend.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.MutableFriendManager;
import org.limewire.friend.impl.feature.AuthTokenRegistry;
import org.limewire.friend.impl.feature.LimeWireFriendFeatureModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class LimeWireFriendImplModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireFriendFeatureModule());

        bind(FriendConnectionFactoryRegistry.class).to(FriendConnectionFactoryRegistryImpl.class);
        bind(FriendConnectionFactory.class).to(FriendConnectionFactoryRegistryImpl.class);

        bind(FriendManager.class).to(MutableFriendManagerImpl.class);
        bind(MutableFriendManager.class).to(MutableFriendManagerImpl.class);

        bind(AuthTokenRegistry.class).to(DefaultFriendAuthenticator.class).asEagerSingleton();
        bind(SessionTimeListener.class);
    }
    
    @Provides @Named("known") Collection<Friend> knownFriendsList(@Named("known") Map<String, Friend> friends) {
        return friends.values();
    }
    
    @Provides @Named("available") Collection<Friend> availableFriendsList(@Named("available") Map<String, Friend> friends) {
        return friends.values();
    }
    
    @Provides @Named("availableFriendIds") Set<String> availableFriendIds(MutableFriendManagerImpl friendManager) {
        return friendManager.getAvailableFriendIds();
    }
    
    @Provides @Named("available") Map<String, Friend> availableFriends(MutableFriendManagerImpl friendManager) {
        return friendManager.getAvailableFriends();
    }
    
    @Provides @Named("known") Map<String, Friend> knownFriends(MutableFriendManagerImpl friendManager) {
        return friendManager.getKnownFriends();
    }

}
