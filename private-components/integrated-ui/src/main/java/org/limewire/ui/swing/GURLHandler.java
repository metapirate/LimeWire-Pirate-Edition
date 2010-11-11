package org.limewire.ui.swing;

import javax.swing.SwingUtilities;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.browser.ExternalControl;

/**
 * JNI based GetURL AppleEvent handler for Mac OS X.
 * Do not move this class without rebuilding the native lib.
 */
public final class GURLHandler {
    
    private static GURLHandler instance;
    
    private volatile boolean registered = false;    
    private volatile boolean enabled = false;
    private volatile String url;
    private volatile ExternalControl externalControl;
    
    static {
        try {
            System.loadLibrary("GURL");
        }
        catch (UnsatisfiedLinkError err) {
            ErrorService.error(err);
        }
    }
    
    public static synchronized GURLHandler getInstance() {
        if(instance == null)
            instance = new GURLHandler();
        return instance;
    }
        
    /** Called by the native code. */
    @SuppressWarnings("unused")
    private void callback(final String url) {
        if ( enabled && externalControl.isInitialized() ) {
            Runnable runner = new Runnable() {
                public void run() {
                    try {
                        externalControl.handleMagnetRequest(url);
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                } 
            };
            SwingUtilities.invokeLater(runner);
        } else {
            this.url = url;
        }
    }
    
    public void enable(ExternalControl externalControl) {
        this.externalControl = externalControl;
        externalControl.enqueueControlRequest(url);
        this.url = null;
        this.enabled = true;
    }
    
    /** Registers the GetURL AppleEvent handler. */
    public void register() {
        if (!registered) {
            if (InstallEventHandler() == 0) {
                registered = true;
            }
        }
    }
    
    /** We're nice guys and remove the GetURL AppleEvent handler although
    this never happens. */
    @Override
    protected void finalize() throws Throwable {
        if (registered) {
            RemoveEventHandler();
        }
    }
    
    private synchronized final native int InstallEventHandler();
    private synchronized final native int RemoveEventHandler();
}
