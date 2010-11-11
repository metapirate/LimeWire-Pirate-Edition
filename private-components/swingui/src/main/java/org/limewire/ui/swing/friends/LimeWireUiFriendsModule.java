package org.limewire.ui.swing.friends;

import org.limewire.inject.LazyBinder;
import org.limewire.ui.swing.friends.chat.LimeWireUiFriendsChatModule;
import org.limewire.ui.swing.friends.chat.ChatStateEvent;
import org.limewire.ui.swing.friends.chat.ChatMessageEvent;
import org.limewire.ui.swing.friends.login.LimeWireUiFriendsLoginModule;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManagerImpl;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;


public class LimeWireUiFriendsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendRequestNotificationController.class);
        bind(FriendAccountConfigurationManager.class).toProvider(LazyBinder.newLazyProvider(
                FriendAccountConfigurationManager.class, FriendAccountConfigurationManagerImpl.class));
        
        EventMulticaster<ChatMessageEvent> chatMessageListenerManager = new EventMulticasterImpl<ChatMessageEvent>();
        bind(new TypeLiteral<EventBroadcaster<ChatMessageEvent>>(){}).toInstance(chatMessageListenerManager);
        bind(new TypeLiteral<ListenerSupport<ChatMessageEvent>>(){}).toInstance(chatMessageListenerManager);
        bind(new TypeLiteral<EventMulticaster<ChatMessageEvent>>(){}).toInstance(chatMessageListenerManager);
        
        EventMulticaster<ChatStateEvent> chatStateListenerManager = new EventMulticasterImpl<ChatStateEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<ChatStateEvent>>(){}).toInstance(chatStateListenerManager);
        bind(new TypeLiteral<ListenerSupport<ChatStateEvent>>(){}).toInstance(chatStateListenerManager);
        bind(new TypeLiteral<EventMulticaster<ChatStateEvent>>(){}).toInstance(chatStateListenerManager);
        
        install(new LimeWireUiFriendsLoginModule());
        install(new LimeWireUiFriendsChatModule());
    }

}
