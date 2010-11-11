package org.limewire.ui.swing.mainframe;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.upload.UploadListManager;

import com.google.inject.Inject;

/**
 * This handles a delayed shutdown of the application after all transfers are
 * completed.  An application must install an instance of DelayedShutdownHandler
 * by calling the <code>install(AppFrame)</code> method.
 */
public class DelayedShutdownHandler {

    private final GnutellaConnectionManager gnutellaConnectionManager;
    private final DownloadListManager downloadListManager;
    private final UploadListManager uploadListManager;

    /** Application instance. */
    private AppFrame appFrame;
    /** Indicates whether a disconnect was performed prior to shutdown. */    
    private boolean disconnectOnShutdown;
    /** Indicates whether a delayed shutdown has been initiated. */    
    private boolean shutdownInitiated;
    /** Indicates whether all file downloads are complete. */    
    private boolean downloadsCompleted;
    /** Indicates whether all file uploads are complete. */    
    private boolean uploadsCompleted;
    /** Listener for downloads completed event. */
    private PropertyChangeListener downloadsCompletedListener;
    /** Listener for uploads completed event. */
    private PropertyChangeListener uploadsCompletedListener;

    /**
     * Constructs a handler to exit the application after all transfers are
     * completed.
     */
    @Inject
    public DelayedShutdownHandler(
        GnutellaConnectionManager gnutellaConnectionManager,
        DownloadListManager downloadListManager,
        UploadListManager uploadListManager) {
        
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        this.downloadListManager = downloadListManager;
        this.uploadListManager = uploadListManager;
    }

    /**
     * Installs the handler on the specified application instance.  This method
     * also adds a listener to the GUI main frame to cancel shutdown when the
     * window is restored.  
     */
    public void install(AppFrame appFrame) {
        // Save application instance.
        this.appFrame = appFrame;
        
        // Add listener to cancel delayed shutdown whenever UI is restored.
        appFrame.getMainFrame().addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeiconified(WindowEvent e) {
                cancelShutdown();
            }
        });
    }

    /**
     * Exits the application after all transfers are completed.  This performs 
     * the following tasks:
     * <ul>
     *   <li>Disconnects from Gnutella</li>
     *   <li>Installs a listener for download completion</li>
     *   <li>Installs a listener for upload completion</li>
     *   <li>Minimizes the UI to the system tray</li>
     * </ul>  
     */
    public void shutdownAfterTransfers() {
        // Skip if shutdown already initiated.
        if (shutdownInitiated) {
            return;
        }
        shutdownInitiated = true;
        
        // Disconnect from Gnutella, and save state for possible reconnect.
        disconnectOnShutdown = gnutellaConnectionManager.isConnected();
        if (disconnectOnShutdown) {
            gnutellaConnectionManager.disconnect();
        }
        
        // Initialize indicators.
        downloadsCompleted = false;
        uploadsCompleted = false;
        
        // Install listener for downloads completed event.  The event is
        // handled by setting an indicator and performing the delayed shutdown.
        if (downloadsCompletedListener == null) {
            downloadsCompletedListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (DownloadListManager.DOWNLOADS_COMPLETED.equals(evt.getPropertyName())) {
                        downloadsCompleted = true;
                        doDelayedShutdown();
                    }
                }
            };
            downloadListManager.addPropertyChangeListener(downloadsCompletedListener);
        }
        
        // Install listener for uploads completed event.  The event is
        // handled by setting an indicator and performing the delayed shutdown.
        if (uploadsCompletedListener == null) {
            uploadsCompletedListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (UploadListManager.UPLOADS_COMPLETED.equals(evt.getPropertyName())) {
                        uploadsCompleted = true;
                        doDelayedShutdown();
                    }
                }
            };
            uploadListManager.addPropertyChangeListener(uploadsCompletedListener);
        }
        
        // Update state after installing listeners.  This generates events for 
        // transfers that are already done.
        downloadListManager.updateDownloadsCompleted();
        uploadListManager.updateUploadsCompleted();
        
        // Minimize UI window.
        appFrame.minimizeToTray();
    }
    
    /**
     * Performs delayed shutdown if all downloads and uploads are completed.
     */
    private void doDelayedShutdown() {
        if (shutdownInitiated && downloadsCompleted && uploadsCompleted) {
            // Exit using action to notify ExitListener instances.
            appFrame.exit(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"));
        }
    }
    
    /**
     * Cancels delayed shutdown after transfer operation.  This performs the 
     * following tasks:
     * <ul>
     *   <li>Resets indicators</li>
     *   <li>Uninstalls listeners for download/upload completion</li>
     *   <li>Reconnects to Gnutella</li>
     * </ul>
     */
    public void cancelShutdown() {
        // Reset indicator.
        shutdownInitiated = false;
        
        // Remove download/upload listeners.
        if (downloadsCompletedListener != null) {
            downloadListManager.removePropertyChangeListener(downloadsCompletedListener);
            downloadsCompletedListener = null;
        }
        if (uploadsCompletedListener != null) {
            uploadListManager.removePropertyChangeListener(uploadsCompletedListener);
            uploadsCompletedListener = null;
        }

        // Reset indicators.
        downloadsCompleted = false;
        uploadsCompleted = false;
        
        // Reconnect to Gnutella. 
        if (disconnectOnShutdown) {
            gnutellaConnectionManager.connect();
            disconnectOnShutdown = false;
        }
    }
}
