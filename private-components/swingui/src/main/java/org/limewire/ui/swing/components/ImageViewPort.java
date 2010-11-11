package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JViewport;

/**
 * Extends ViewPort to draw a background image instead of the standard background color.
 * 
 * In order to have the image be scene, the Component placed inside the viewPort must
 * be set to opaque(false) else the components background color will be seen instead.
 */
public class ImageViewPort extends JViewport {

    private Image image;
    
    public ImageViewPort(Image image){
        this.image = image;
    }
    
    @Override
    public void paintComponent(Graphics g){        
        if( image != null && image.getWidth(this) > 0 && image.getHeight(this) > 0) {
            g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
        }
        
        super.paintComponents(g);
    }
}