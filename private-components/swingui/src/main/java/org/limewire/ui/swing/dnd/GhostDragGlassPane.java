package org.limewire.ui.swing.dnd;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Panel which gets installed as the glass pane of a Window. 
 * This glass pane creates and displays semi-transparent images
 * over the main application.
 * <p>
 * It is the responsibility of drag listeners to update the positioning,
 * image and visibility of this glass pane.
 */
@LazySingleton
public class GhostDragGlassPane extends JPanel {

    @Resource
    private Icon dragIconAccept;
    @Resource
    private Icon dragIconReject;
    
    private Icon dragIcon;
    
    private float alpha = 0.85f;
    
    private BufferedImage dragged = null;
    private Point location = new Point(0, 0);
    private Point oldLocation = new Point(0, 0);
    
    private int width;
    private int height;
    private Rectangle visibleRect = null;
    
    @Inject
    public GhostDragGlassPane() {
        setOpaque(false);
        
        GuiUtils.assignResources(this);
        this.dragIcon = dragIconAccept;
        updateDragImage();
    }

    private void updateDragImage() {
        dragged =  new BufferedImage(dragIcon.getIconWidth(), dragIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = dragged.getGraphics();
        g.drawImage(((ImageIcon)dragIcon).getImage(), 0, 0, null);

        width = dragIcon.getIconWidth();
        height = dragIcon.getIconHeight();
    }
    
    /**
     * Relocates the image to this point. The image is displayed
     * to the right of this location and 50% above/below the 
     * y coordinate of this location.
     */
    public void setPoint(Point location) {
        this.oldLocation = this.location;
        this.location = location;
    }
    
    /**
     * Returns the rectangle of where changes have occurred
     * on the glass pane. This can greatly improve performance
     * by not repainting the entire glass pane.
     */
    public Rectangle getRepaintRect() {
        int x = (int) (location.getX());
        int y = (int) (location.getY() - (height/ 2));
        
        int x2 = (int) (oldLocation.getX());
        int y2 = (int) (oldLocation.getY() - (height/ 2));
        
        return new Rectangle(x, y, width, height).union(new Rectangle(x2, y2, width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (dragged == null || !isVisible()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (int) (location.getX());
        int y = (int) (location.getY() - (height/ 2));
        
        if (visibleRect != null) {
            g2.setClip(visibleRect);
        }
        
        g2.drawImage(dragged, x, y, width, height, null);
        g2.dispose();
    }
    
    public void setAccept(boolean accept) {
        Icon oldIcon = dragIcon;
        dragIcon = accept ? dragIconAccept : dragIconReject;
        if(dragIcon != oldIcon) {
            updateDragImage();
            repaint();
        }
    }
}
