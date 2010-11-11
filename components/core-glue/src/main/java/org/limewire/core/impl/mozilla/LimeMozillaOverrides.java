package org.limewire.core.impl.mozilla;

import org.limewire.core.settings.MozillaSettings;
import org.limewire.core.settings.SharingSettings;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIPrefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class LimeMozillaOverrides {

    private final LimeMozillaDownloadManagerListenerImpl downloadManagerListener;

    @Inject
    public LimeMozillaOverrides(LimeMozillaDownloadManagerListenerImpl downloadManagerListener) {
        this.downloadManagerListener = downloadManagerListener;
    }

    public void overrideMozillaDefaults() {
        // Using asynchronous executor because the synchronous executor blocks
        // indefinitely on osx while bootstrapping
        MozillaExecutor.mozAsyncExec(new Runnable() {
            @Override
            public void run() {
                // lookup the preferences service by contract id.
                // by getting a proxy we do not need to run code through mozilla
                // thread.
                nsIPrefService prefService = XPCOMUtils.getService(
                        "@mozilla.org/preferences-service;1", nsIPrefService.class);

                // set default downloads to desktop, we are going to override
                // this with
                // our own download manager
                prefService.getBranch("browser.download.").setBoolPref("useDownloadDir", 1);
                prefService.getBranch("browser.download.").setIntPref("folderList", 2);
                String downloadDir = SharingSettings.INCOMPLETE_DIRECTORY.get().getAbsolutePath();
                prefService.getBranch("browser.download.").setCharPref("dir", downloadDir);
                prefService.getBranch("browser.download.manager.").setBoolPref("showWhenStarting",
                        0);
                prefService.getBranch("browser.download.manager.").setBoolPref(
                        "showAlertOnComplete", 0);
                prefService.getBranch("general.useragent.extra.").setCharPref("firefox",
                        LimeWireUtils.getHttpServer());

                // prevents mozilla from beiing in offline mode inside of
                // limewire.
                prefService.getBranch("browser.").setBoolPref("offline", 0);
                prefService.getBranch("network.").setBoolPref("online", 1);

                // setup which mime types do not prompt to download
                // this will prevent the save or open dialogue from prompting
                prefService.getBranch("browser.helperApps.neverAsk.").setCharPref("saveToDisk",
                        MozillaSettings.DOWNLOAD_MIME_TYPES.get());

                // adding the download listener
                nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(
                        "@mozilla.org/download-manager;1", nsIDownloadManager.class);
                downloadManagerListener.addMissingDownloads();
                downloadManagerListener.resumeDownloads();
                downloadManager.addListener(downloadManagerListener);

                // Limit the number of parallel HTTP connections
                int max = MozillaSettings.MAX_CONNECTIONS.getValue();
                prefService.getBranch("network.http.").setIntPref("max-connections", max);
                prefService.getBranch("network.http.").setIntPref("max-connections-per-server",
                        max / 2);
            }
        });
    }
}
