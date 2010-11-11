package org.limewire.ui.swing.friends.login;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiFriendsLoginModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XMPPUserEntryLoginPanelFactory.class).toProvider(
              FactoryProvider.newFactory(XMPPUserEntryLoginPanelFactory.class, XMPPUserEntryLoginPanel.class));
        bind(AutoLoginService.class);
    }

}
