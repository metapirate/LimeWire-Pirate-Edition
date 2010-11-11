package org.limewire.ui.swing.player;

import org.limewire.util.OSUtils;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiPlayerModule extends AbstractModule {

    @Override
    protected void configure() {
        if(OSUtils.isLinux())
            bind(PlayerMediator.class).to(AudioPlayerMediator.class);
        else
            bind(PlayerMediator.class).to(PlayerMediatorImpl.class);
        bind(VideoPanelFactory.class).toProvider(FactoryProvider.newFactory(VideoPanelFactory.class, VideoPanel.class));
        bind(MediaPlayerFactory.class).to(MediaPlayerFactoryImpl.class);
    }

}
