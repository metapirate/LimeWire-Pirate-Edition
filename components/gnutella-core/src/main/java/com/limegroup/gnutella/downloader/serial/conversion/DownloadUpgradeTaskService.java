package com.limegroup.gnutella.downloader.serial.conversion;

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class DownloadUpgradeTaskService {

    private final Provider<DownloadUpgradeTask> upgradeTask;

    @Inject DownloadUpgradeTaskService(Provider<DownloadUpgradeTask> upgradeTask) {
        this.upgradeTask = upgradeTask;
    }

    @Inject @SuppressWarnings("unused")
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public String getServiceName() {
                return org.limewire.i18n.I18nMarker.marktr("Download Upgrade Task");
            }

            public void initialize() {
            };

            public void start() {
                upgradeTask.get().upgrade();
            }

            public void stop() {
            };
        }).in(ServiceStage.EARLY);
    }

}
