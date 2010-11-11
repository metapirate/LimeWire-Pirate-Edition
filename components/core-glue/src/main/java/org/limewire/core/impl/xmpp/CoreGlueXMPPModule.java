package org.limewire.core.impl.xmpp;

import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.friend.api.PasswordManager;
import org.limewire.inject.AbstractModule;
import org.limewire.xmpp.client.LimeWireXMPPModule;
import org.limewire.xmpp.client.impl.PasswordManagerImpl;

import com.limegroup.gnutella.settings.SettingsBackedJabberSettings;

public class CoreGlueXMPPModule extends AbstractModule {
    @Override
    protected void configure() {
        binder().install(new LimeWireXMPPModule(SettingsBackedJabberSettings.class));
        
        
        bind(PasswordManager.class).to(PasswordManagerImpl.class);
        bind(XMPPResourceFactory.class).to(XMPPResourceFactoryImpl.class);
    }
}
