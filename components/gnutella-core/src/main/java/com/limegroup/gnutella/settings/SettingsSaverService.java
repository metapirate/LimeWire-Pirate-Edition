package com.limegroup.gnutella.settings;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.i18n.I18nMarker;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.setting.SettingsGroupManager;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@EagerSingleton
class SettingsSaverService implements Service {

    /** How often settings should be saved, in seconds. */
    private static final int SAVE_INTERVAL = 300;

    private final ScheduledExecutorService backgroundExecutor;
    private volatile Future future = null;

    @Inject
    SettingsSaverService(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    @Inject
    public void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public String getServiceName() {
        return I18nMarker.marktr("Settings Saver");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        future = backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                SettingsGroupManager.instance().save();
            }
        }, 60, SAVE_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        final Future future = this.future;
        if(future != null) {
            future.cancel(false);
            this.future = null;
        }
    }
}
