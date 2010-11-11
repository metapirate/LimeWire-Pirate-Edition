package org.limewire.ui.swing.images;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;

import org.jdesktop.application.Resource;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

@LazySingleton
public class ThumbnailManagerImpl implements ThumbnailManager {
    
    /**
     * List of images that have thumbnails created already
     */
    private final Map<File,Icon> thumbnails = new FixedsizeForgetfulHashMap<File,Icon>(2000);
    
    /**
     * List of images that are queued to be loaded but haven't been loaded yet
     */
    private final Map<File, String> loading = new HashMap<File, String>();
    
    @Resource
    private Icon loadIcon;
    @Resource
    private Icon errorIcon;
    
    @Inject
    public ThumbnailManagerImpl() {
        GuiUtils.assignResources(this); 
    }

    @Override
    public Icon getThumbnailForFile(File file) {
        if(file == null)
            return null;
        Icon icon = thumbnails.get(file);
        if(icon == null) {
            icon = loadIcon;
            if(!loading.containsKey(file)) {
                loading.put(file, "");
                ImageExecutorService.submit(new ThumbnailCallable(thumbnails, loading, file, errorIcon));
            }
        }
        return icon;
    }
    
    @Override
    public Icon getThumbnailForFile(File file, JComponent callback) {
        if(file == null)
            return null;
        Icon icon = thumbnails.get(file);
        if(icon == null) {
            icon = loadIcon;
            if(!loading.containsKey(file)) {
                loading.put(file, "");
                ImageExecutorService.submit(new ThumbnailCallable(thumbnails, loading, file, errorIcon, callback));
            }
        }
        return icon;
    }
    
    @Override 
    public Icon getThumbnailForFile(File file, JList list, int index) {
        if(file == null) 
            return null;
        Icon icon = thumbnails.get(file);
        if(icon == null) {
            icon = loadIcon;
            if(!loading.containsKey(file)) {
                loading.put(file, "");
                ImageExecutorService.submit(new ThumbnailCallable(thumbnails, loading, file, errorIcon, list, index));
            }
        } 
        return icon;
    }

    @Override
    public boolean isThumbnailForFileAvailable(File file) {
        return thumbnails.containsKey(file);
    }
    
    public boolean isErrorIcon(File file) {
        return thumbnails.containsKey(file) && 
                thumbnails.get(file).equals(errorIcon);
    }
}
