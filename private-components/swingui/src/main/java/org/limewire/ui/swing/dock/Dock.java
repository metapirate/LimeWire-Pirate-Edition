package org.limewire.ui.swing.dock;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JPanel;


import org.limewire.service.ErrorService;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.util.OSUtils;


/**
 * A utility class to modify the Dock Icon and request
 * for user attention.
 */
public class Dock {
    
    private static final boolean HAS_DOCK;
    
    static {
        boolean hasDock = false;
        if (OSUtils.isMacOSX()) {
            try {
                System.loadLibrary("Dock");
                hasDock = true;
            } catch (UnsatisfiedLinkError err) {
                 ErrorService.error(err, "java.library.path=" + System.getProperty("java.library.path") + "\n\n" + "trace dependencies=" + MacOSXUtils.traceLibraryDependencies("libDock.jnilib"));
            }
        }
        HAS_DOCK = hasDock;
    }   

    /**
     * These constants specify the level of severity of a user 
     * attention request and are used by 
     * {@link Dock#requestUserAttention(com.limegroup.gnutella.gui.Dock.AttentionType)}
     * and {@link Dock#cancelUserAttentionRequest(int)}.
     * 
     * See the documentation for NSApplication and its Constants for
     * more information.
     */
    public static enum AttentionType {
        
        /**
         * The Dock Icon will bounce until either the application
         * becomes active or the request is canceled.
         */
        CRITICAL(0),
        
        /**
         * The Dock Icon will bounce for one second. The request, 
         * though, remains active until either the application 
         * becomes active or the request is canceled.
         * 
         * NOTE: The Dock Icon will bounce like {@link #CRITICAL} 
         * endlessly until either the application becomes active or 
         * the request is canceled.
         */
        INFORMATIONAL(10);
        
        private final int type;
        
        private AttentionType(int type) {
            this.type = type;
        }
    }
    
    /** 
     * The width of the Dock Icon (128px).
     */
    public static final int ICON_WIDTH = 128;
    
    /**
     * The height of the Dock Icon (128px).
     */
    public static final int ICON_HEIGHT = 128;
    
    private Dock() {
        
    }
    
    /**
     * Returns the Lock the Dock is using to synchronize
     * access to this class.
     */
    public static Object getDockLock() {
        return Dock.class;
    }
    
    /**
     * Replaces the current Dock Icon with the given Icon.
     * 
     * @param icon The new Icon to be used in the Dock
     */
    public synchronized static void setDockTileImage(Icon icon) {
        paintIcon(icon, false);
    }
    
    /**
     * Overlays the current Dock icon with the given Icon.
     * 
     * @param icon The new overlay Icon
     */
    public synchronized static void setDockTileOverlayImage(Icon icon) {
        paintIcon(icon, true);
    }
    
    /**
     * Paints the Icon, extracts the pixel data and passes it to
     * the native code which draws the Icon in the Dock.
     * 
     * @param icon The new Icon
     * @param overlay Whether it's an overlay or replacement Icon
     */
    private synchronized static void paintIcon(Icon icon, boolean overlay) {
        if (!HAS_DOCK) {
            return;
        }
        
        if (icon == null) {
            throw new NullPointerException("Icon is null");
        }
        
        // We'll store the pixel in this Array and pass it
        // to the native methods later
        int[] pixel = new int[ICON_WIDTH * ICON_HEIGHT];

        // We draw the Icon in this BufferedImage
        BufferedImage image = new BufferedImage(ICON_WIDTH, ICON_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        
        // Setup a fake Component
        Component panel = new JPanel();
        Dimension iconSize = new Dimension(ICON_WIDTH, ICON_HEIGHT);
        panel.setSize(iconSize);
        panel.setPreferredSize(iconSize);
        panel.setMinimumSize(iconSize);
        panel.setMaximumSize(iconSize);
        
        // Initialize the Image with the current pixel (zeros)
        image.setRGB(0, 0, ICON_WIDTH, ICON_HEIGHT, pixel, 0, ICON_WIDTH);
        
        // Create a Graphics Object and paint the Icon on it
        Graphics2D g = image.createGraphics();
        try {
            icon.paintIcon(panel, g, 0, 0);
        } finally {
            g.dispose();
        }
        
        // Get the pixel!
        image.getRGB(0, 0, ICON_WIDTH, ICON_HEIGHT, pixel, 0, ICON_WIDTH);
        
        // And draw the Icon!
        DrawDockTileImage(pixel, overlay, MacOSXUtils.getAppDir());
    }
    
    /**
     * Restores the Dock Icon to its original state.
     */
    public synchronized static void restoreDockTileImage() {
        if (HAS_DOCK) {
            RestoreApplicationDockTileImage(MacOSXUtils.getAppDir());
        }
    }
    
    /**
     * Starts a user attention request. Calling this method has no
     * effect if the application is already active. The value returned
     * by this method can be used to manually cancel an attention
     * request.
     * 
     * @param requestType The type of the attention request
     * @return request identifier
     */
    public synchronized static int requestUserAttention(AttentionType requestType) {
        if (HAS_DOCK) {
            return RequestUserAttention(requestType.type);
        }
        return -1;
    }
    
    /**
     * Cancels a previous user attention request. A request is also canceled
     * automatically by user activation of the application.
     * 
     * @param request The return value from a previous call to 
     *  {@link #requestUserAttention(com.limegroup.gnutella.gui.Dock.AttentionType)}
     */
    public synchronized static void cancelUserAttentionRequest(int request) {
        if (HAS_DOCK) {
            CancelUserAttentionRequest(request);
        }
    }
    
    /**
     * Draws the Dock Tile Image.
     * 
     * OverlayApplicationDockTileImage(CGImageRef)
     * SetApplicationDockTileImage(CGImageRef)
     */
    private static final native void DrawDockTileImage(int[] icon, boolean overlay, String appDir);
    
    /**
     * Restores the Dock Tile Image.
     */
    private static final native void RestoreApplicationDockTileImage(String appDir);
    
    /**
     * Starts an user attention request.
     */
    private static final native int RequestUserAttention(int requestType);
    
    /**
     * Cancels an user attention request manually.
     */
    private static final native void CancelUserAttentionRequest(int request);
}
