package com.limegroup.gnutella.settings;

import com.google.inject.AbstractModule;

public class LimeWireSettingsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SettingsSaverService.class);
    }
}
