package org.limewire.ui.swing.components;

import java.awt.Image;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class LimeIconInfo {    

    @Resource private Icon limeIcon;
    @Resource private ImageIcon lime16;
    @Resource private ImageIcon lime32;
    @Resource private ImageIcon lime48;
    @Resource private String limeFrameIconLocation;
    
    public LimeIconInfo() {
        GuiUtils.assignResources(this);
    }
    
    public Icon getIcon() {
        return limeIcon;
    }
    
    /**
     * Returns a list of varying size icons similar to how
     * an .ico file is built.
     */
    public List<? extends Image> getIconImages() {
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(lime16.getImage());
        images.add(lime32.getImage());
        images.add(lime48.getImage());
        
        return images;
    }
    
    public String getIconLocation() {
        return limeFrameIconLocation;
    }
    
    public Image getImage() {
        return ((ImageIcon)limeIcon).getImage();
    }
    
    public File getIconFile() {
        return new File(
           URI.create(ClassLoader.getSystemResource(limeFrameIconLocation).getFile()).getPath())
                .getAbsoluteFile();
    }
}
