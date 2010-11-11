package org.limewire.ui.swing.util;

import java.awt.Component;
import java.awt.Graphics;
import java.io.File;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Manages finding native icons for files and file types.
 */
@Singleton
public class IconManager {
    
    /** The original basic file icon controller. **/
    private final FileIconController originalFileController;
    
    
    /** The current FileIconController. */
    private FileIconController fileController;
    
    private Icon blankIcon;
    
    @Inject
    IconManager(NonBlockFileIconController nonBlockFileIconController,
                final Provider<NativeFileIconController> nativeFileIconControllerFactory) {
        // Always begin with the basic controller,
        // whose construction can never block.
        this.originalFileController = nonBlockFileIconController;
        fileController = this.originalFileController;
        
        // Then, in a new thread, try to change it to a controller
        // that can block.
        if (OSUtils.isMacOSX() || OSUtils.isWindows()) {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    final FileIconController newController = nativeFileIconControllerFactory.get();
                    if (newController.isValid()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                fileController = newController;
                            }
                        });
                    }
                }
            }, "NativeFileIconLoader");
        }
    }
    
    /**
     * Function to create, cache, and provide an empty 16 by 16 icon.
     */
    public Icon getBlankIcon() {
        
        if (this.blankIcon != null)  return this.blankIcon;
        
        this.blankIcon = new Icon() {

            @Override
            public int getIconHeight() {
                return 16;
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
            }
        };
        
    
        return this.blankIcon;
    }
    
    /**
     * Returns the icon associated with this file.
     * If the file does not exist, or no icon can be found, returns
     * the icon associated with the extension.
     */
    public Icon getIconForFile(File f) {
        validate();
        return fileController.getIconForFile(f);
    }
    
    /**
     * Returns the icon associated with the extension.
     * TODO: Implement better.
     */
    public Icon getIconForExtension(String ext) {
        validate();
        return fileController.getIconForExtension(ext);
    }
    
    /**
     * Returns the icon associated with the extension for this PropertiableFile.
     */
    public Icon getIconForPropertiableFile(PropertiableFile file) {
        return getIconForExtension(getExtension(file));
    }
    
    /** Returns true if the icon can be returned immediately. */
    public boolean isIconForFileAvailable(File f) {
        validate();
        return fileController.isIconForFileAvailable(f);
    }
    
    /**
     * Returns a text description of the MIME type for the given PropertiableFile's extension.
     */
    public String getMIMEDescription(PropertiableFile propertiable) {
        return getMIMEDescription(getExtension(propertiable));
    }
    
    /**
     * Returns a text description of the MIME type for the given file extension.
     */
    public String getMIMEDescription(String extension) {
        if (extension == null) {
            return null;
        }
        
        Icon icon = getIconForExtension(extension);
        
        if (icon == null) {
            return null;
        }
        
        String iconString = icon.toString();
        if (iconString.indexOf("@") > -1) {
            //MIME label isn't set to something readable for the icon
            return null;
        }
        
        if (iconString.endsWith(".png")) {
            return null;
        }
        
        return iconString;
    }

    private String getExtension(PropertiableFile propertiable) {
        return FileUtils.getFileExtension(propertiable.getFileName());
    }
    
    /**
     * Reverts the IconController to a basic controller if at any point
     * in time the controller becomes invalid.
     * <p>
     * Returns true if the current controller is already valid.
     */
    private void validate() {
        if (!fileController.isValid())
            fileController = this.originalFileController;
    }
}