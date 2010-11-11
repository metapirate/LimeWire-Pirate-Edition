package org.limewire.ui.swing.friends.chat;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiFriendsChatModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ChatHyperlinkListenerFactory.class).toProvider(
                FactoryProvider.newFactory(ChatHyperlinkListenerFactory.class, ChatHyperlinkListener.class));        
        bind(ConversationPaneFactory.class).toProvider(
                FactoryProvider.newFactory(ConversationPaneFactory.class, ConversationPane.class));
    }
}
