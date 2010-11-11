package org.limewire.ui.swing.images;

import java.io.File;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;

/**
 * Manages thumbnails for image files. 
 */
public interface ThumbnailManager {
    
    public static final int WIDTH = 116;
    public static final int HEIGHT = 86;

    /**
     * Returns the thumbnail for this file. If this file is not immediately 
     * available, it returns a place holder and lazily loads the image in
     * the background. Once the image has been loaded, subsequent requests 
     * for the image will return the thumbnail immediately.
     */
    public Icon getThumbnailForFile(File file);
    
    /**
	 * Returns the thumbnail for this file. If this file is not immediately 
     * available, it returns a place holder and lazily loads the image in
     * the background. Once the image has been loaded, subsequent requests 
     * for the image will return the thumbnail immediately.
     * <p>
     * Once the thumbnail has been loaded, if the component is still showing, 
     * it will cause a repaint at which time the correct image should replace
     * the place holder image.
     * <p>
     * NOTE: the component must be visible at the time the thumbnail will 
     * be created. If the component is no longer visible, the thumbnail
     * will not be created.
	 */
    public Icon getThumbnailForFile(File file, JComponent callback);
    
    /**
     * Returns the thumbnail for this file. If this file is not immediately 
     * available, it returns a place holder and lazily loads the image in
     * the background. Once the image has been loaded, subsequent requests 
     * for the image will return the thumbnail immediately.
     * <p>
     * Once the thumbnail has been loaded, if the component is still showing, 
     * it will cause a repaint at which time the correct image should replace
     * the place holder image.
     * <p>
     * NOTE: the JList must be visible at the time the thumbnail will 
     * be created. If the JList is no longer visible, the thumbnail
     * will not be created. Or if the index is no longer visible 
     * within the JList, the thumbnail will not be created.
     */
    public Icon getThumbnailForFile(File file, JList list, int index);
    
    /**
     * Returns true if the thumbnail is available immediately.
     */
    public boolean isThumbnailForFileAvailable(File file);
    
    public boolean isErrorIcon(File file);
}
