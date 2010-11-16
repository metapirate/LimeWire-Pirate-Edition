package org.limewire.core.settings;

import org.limewire.core.api.malware.VirusNfoUrl;
import org.limewire.core.api.malware.VirusUpdatesUrl;
import org.limewire.inject.AbstractModule;

import com.google.inject.TypeLiteral;

public class LimeWireCoreSettingsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<String>(){}).annotatedWith(VirusUpdatesUrl.class).toProvider(DownloadSettings.VIRUS_UPDATES_SERVER);
        bind(new TypeLiteral<String>(){}).annotatedWith(VirusNfoUrl.class).toProvider(DownloadSettings.VIRUS_NFO_SERVER);
    }
}
