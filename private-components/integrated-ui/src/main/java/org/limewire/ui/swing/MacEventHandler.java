package org.limewire.ui.swing;

import java.io.File;

import javax.swing.Action;
import javax.swing.ActionMap;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationAdapter;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.ui.swing.mainframe.AboutAction;
import org.limewire.ui.swing.mainframe.OptionsAction;
import org.limewire.ui.swing.menu.ExitAction;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class handles Macintosh specific events. The handled events include the
 * selection of the "About" option in the Mac file menu, the selection of the
 * "Quit" option from the Mac file menu, and the dropping of a file on LimeWire
 * on the Mac, which LimeWire would be expected to handle in some way.
 */
@SuppressWarnings("restriction")
public class MacEventHandler extends ApplicationAdapter {

    private static MacEventHandler INSTANCE;

    public static synchronized MacEventHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new MacEventHandler();
        }
        return INSTANCE;
    }

    private volatile File lastFileOpened = null;
    private volatile boolean enabled;

    @Inject private volatile ExternalControl externalControl = null;
    @Inject private volatile DownloadManager downloadManager = null;
    @Inject private volatile LifeCycleManager lifecycleManager = null;
    @Inject private volatile ActivityCallback activityCallback = null;
    @Inject private volatile AboutAction aboutAction = null;
    @Inject private volatile OptionsAction optionsAction = null;
    @Inject private volatile ExitAction exitAction = null;
    @Inject private volatile CategoryManager categoryManager = null;

    private boolean allMenusAndActionsEnabled = false;
    
    /** Creates a new instance of MacEventHandler */
    @Inject
    public MacEventHandler() {
        assert ( OSUtils.isMacOSX() ) : "MacEventHandler should only be used on Mac OS-X operating systems.";
        
        Application.getApplication().addApplicationListener(this);
        
        Application.getApplication().setEnabledPreferencesMenu(false);
        Application.getApplication().setEnabledAboutMenu(false);
    }

    @Inject
    public void startup() {
        this.enabled = true;
        
        if (lastFileOpened != null) {
            runFileOpen(lastFileOpened);
        }
    }

    /**
     * Enable preferences.
     */
    public void enableAllMacMenusAndEventHandlers() {
        Application.getApplication().setEnabledAboutMenu(true);
        Application.getApplication().setEnabledPreferencesMenu(true);
        allMenusAndActionsEnabled = true;
    }
    
    @Override
    public void handlePreferences(ApplicationEvent event) {
        if (allMenusAndActionsEnabled ) {
            optionsAction.actionPerformed(null);
            event.setHandled(true);
        }
    }
    
    @Override
    public void handleAbout(ApplicationEvent event) {
        if (allMenusAndActionsEnabled ) {        
            aboutAction.actionPerformed(null);
            event.setHandled(true);
        }
    }
    
    @Override
    public void handleQuit(ApplicationEvent event) {
        if (allMenusAndActionsEnabled ) {
            exitAction.actionPerformed(null);
            event.setHandled(true);
        }
    }
  
    /**
     * Must be added after the UI is created, otherwise it'll create the application too soon!
     * Since the preferences are enabled after the UI is created, let's handle these events only
     * after the preferences have been enabled.
     */
    @Override
    public void handleReOpenApplication(ApplicationEvent event) {
        if ( allMenusAndActionsEnabled ) {
            ActionMap map = org.jdesktop.application.Application.getInstance().getContext().getActionManager().getActionMap();
            Action action = map.get("restoreView");
            if (action != null) {
                action.actionPerformed(null);
            }
            event.setHandled(true);
        }
    }
    
    @Override
    public void handleOpenFile(ApplicationEvent event) {
        // This handler unlike the others may receive OS events before the UI is created.
        // So, don't check if allMenusAndActionsEnabled is true.
        File file = new File(event.getFilename());
        if (!enabled) {
            lastFileOpened = file;
        } else {
            runFileOpen(file);
        }
        event.setHandled(true);
    }

    private void runFileOpen(final File file) {
        String filename = file.getPath();
        if (filename.endsWith("limestart")) {
            LimeWireUtils.setAutoStartupLaunch(true);
        } else if (filename.endsWith("torrent")) {
            if (!lifecycleManager.isStarted()) {
                externalControl.enqueueControlRequest(file.getAbsolutePath());
            } else {
                try {
                    downloadManager.downloadTorrent(file, null, false);
                } catch (DownloadException e) {
                    activityCallback.handleDownloadException(new DownloadAction() {
                        @Override
                        public void download(File saveDirectory, boolean overwrite)
                                throws DownloadException {
                            downloadManager.downloadTorrent(file, saveDirectory, overwrite);
                        }

                        @Override
                        public void downloadCanceled(DownloadException ignored) {
                            //nothing to do
                        }

                    }, e, false);

                }
            }
        } else {
            NativeLaunchUtils.safeLaunchFile(file, categoryManager);
        }
    }
}
