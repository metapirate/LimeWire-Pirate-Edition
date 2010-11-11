package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class is the glue that holds LimeWire together.
 * All various components are wired together here.
 */
@EagerSingleton
public class LimeCoreGlue implements Service {
    
    private static AtomicBoolean preinstalled = new AtomicBoolean(false);
    private AtomicBoolean installed = new AtomicBoolean(false);
    
    /**
     * Wires initial pieces together that are required for nearly everything.
     * 
     * @param userSettingsDir the preferred directory for user settings
     */
    public static void preinstall() throws InstallFailedException {
        preinstall(null);
    }
    
    /**
     * Wires initial pieces together that are required for nearly everything.
     * 
     * @param userSettingsDir the preferred directory for user settings
     */
    public static void preinstall(File userSettingsDir) throws InstallFailedException {
        // Only preinstall once
        if(!preinstalled.compareAndSet(false, true))
            return;
        // Only get the settings location once
        if (userSettingsDir == null)
            userSettingsDir = LimeWireUtils.getRequestedUserSettingsLocation();
        
        // This looks a lot more complicated than it really is.
        // The excess try/catch blocks are just to make debugging easier,
        // to keep track of what messages each successive IOException is.
        // The flow is basically:
        //  - Try to set the settings dir to the requested location.
        //  - If that doesn't work, try getting a temporary directory to use.
        //  - If we can't find a temporary directory, deleting old stale ones & try again.
        //  - If it still doesn't work, bail.
        //  - If it did work, mark it for deletion & set it as the settings directory.
        //  - If it can't be set, bail.
        //  - Otherwise, success.
        try {
            CommonUtils.setUserSettingsDir(userSettingsDir);
        } catch(IOException requestedFailed) {
            try {
                // First clear any older temporary settings directories.
                LimeWireUtils.clearTemporarySettingsDirectories();
                // Then try to set a temporary directory...
                File temporaryDir;
                try {
                    temporaryDir = LimeWireUtils.getTemporarySettingsDirectory();
                } catch(IOException tempFailed) {
                    tempFailed.initCause(requestedFailed);
                    throw tempFailed;
                }
                
                temporaryDir.deleteOnExit();
                
                try {
                    CommonUtils.setUserSettingsDir(temporaryDir);
                } catch(IOException cannotSet) {
                    cannotSet.initCause(requestedFailed);
                    throw cannotSet;
                }
                
                LimeWireUtils.setTemporaryDirectoryInUse(true);
            } catch(IOException totalFailure) {
                throw new InstallFailedException("Settings Directory Failure", totalFailure);
            }
        }
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this).in("SuperEarly");
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Core Glue");
    }
    
    public void initialize() {
    }
    
    public void start() {
        install();
    }
    
    public void stop() {
    }

    /** Wires all various components together. */
    public void install() {
        // Only install once.
        if(!installed.compareAndSet(false, true))
            return;
        
        preinstall(); // Ensure we're preinstalled.
    }
    
    /** Simple exception for failure to install. */
    public static class InstallFailedException extends RuntimeException {
        public InstallFailedException() {
            super();
        }

        public InstallFailedException(String message, Throwable cause) {
            super(message, cause);
        }

        public InstallFailedException(String message) {
            super(message);
        }

        public InstallFailedException(Throwable cause) {
            super(cause);
        }
        
    }

}
