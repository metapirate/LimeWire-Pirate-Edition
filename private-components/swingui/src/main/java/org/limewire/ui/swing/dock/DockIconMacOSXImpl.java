package org.limewire.ui.swing.dock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;

/**
 * A Mac OS X Dock Icon for LimeWire. Overlays the download
 * completion count on the LimeWire icon in the dock.
 */
class DockIconMacOSXImpl implements DockIcon {
    
    /**
     * The max number we can display (depends on the FONT!).
     */
    private static final int MAX_NUMBER = 999999;
    
    /**
     * The minimum width and height of the badge.
     */
    private static final int MIN_WnH = 42;
    
    /** 
     * Gradient FROM Color.
     */
    private final Color FROM = new Color(0xE0, 0x00, 0x00);
    
    /**
     * Gradient TO Color.
     */
    private final Color TO = new Color(0xC0, 0x00, 0x00);
    
    /**
     * The Font that is used to draw the numbers.
     */
    private final Font FONT = new Font("Lucida Grande", Font.BOLD, 24);
    
    /**
     * The number of complete Downloads.
     */
    private int complete = 0;
    
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(new Service() {
            public void start() {}

            @Asynchronous (daemon = false)
            public void stop() {
                Dock.restoreDockTileImage();
            }

            public void initialize() {}

            public String getServiceName() {
                return "Dock cleanup";
            }
        }).in("UIHack");
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#getIconWidth()
     */
    public int getIconWidth() {
        return Dock.ICON_WIDTH;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#getIconHeight()
     */
    public int getIconHeight() {
        return Dock.ICON_HEIGHT;
    }
    
    /**
     * Draws the LimeWire Dock Icon. Only draws if the count
     * has changed since the last draw.
     * 
     * @param complete The number of complete Downloads
     */
    public void draw(int complete) {
        synchronized (Dock.getDockLock()) {
            if (complete != this.complete) {
                this.complete = complete;
                
                Dock.restoreDockTileImage();
                
                if (complete > 0)
                    Dock.setDockTileOverlayImage(this);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        String value = Integer.toString(Math.min(complete, MAX_NUMBER));

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setFont(FONT);
        FontMetrics fm = g2.getFontMetrics();
        int valueWidth = fm.stringWidth(value);
        
        int width = MIN_WnH;
        if (value.length() > 2) {
            width = Math.max(valueWidth + 16, width);
        }
        
        RoundRectangle2D.Float ellipse = new RoundRectangle2D.Float(
                    x+Dock.ICON_WIDTH-width-5, y+Dock.ICON_HEIGHT-55, 
                    width, MIN_WnH, MIN_WnH, MIN_WnH);
        
        g2.setStroke(new BasicStroke(1.5f));
        g2.setPaint(new GradientPaint(0, 0, FROM, ellipse.width, ellipse.height, TO));
        g2.fill(ellipse);
        
        g2.setPaint(Color.black);
        g2.draw(ellipse);
        
        g2.setPaint(Color.white);
        g2.drawString(value, ellipse.x + (ellipse.width - valueWidth)/2, ellipse.y + fm.getHeight());
    }
    
}