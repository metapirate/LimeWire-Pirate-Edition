package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Draws the search tab shape within a given bounds.
 */
public class SearchTabPainter<X> extends AbstractPainter<X> {
    
    private final Paint topBevelBackground;
    private final Paint topBevelBorder;
    private final boolean raiseBottomClip;
    
    private Paint background;
    
    private Area tabAreaCache   = null;
    private int  tabWidthCache  = 0;
    private int  tabHeightCache = 0;
    
    public SearchTabPainter(Paint topBevelBackground, Paint topBevelBorder, Paint background) {
        this(topBevelBackground, topBevelBorder, background, false);
    }
    
    /**
     * raiseBottomClip can be used to clip painting one pixel earlier.  This is necessary
     *  since currently tabs are painted over top of the bottom border of the top bar.  
     *  highlighted tabs are clipped early to use the top bar bottom border as a
     *  high contrast bottom edge that makes them look "behind" any other front tab which 
     *  run over the same border and have no hard edge.
     */
    public SearchTabPainter(Paint topBevelBackground, Paint topBevelBorder, Paint background,
            boolean raiseBottomClip) {
        
        GuiUtils.assignResources(this);
        
        this.setAntialiasing(true);
        this.setCacheable(true);
        
        this.topBevelBackground = topBevelBackground;
        this.topBevelBorder = topBevelBorder;
        this.raiseBottomClip = raiseBottomClip;
        
        this.background = background;
    }
  
    private void cacheTabArea(int width, int height) {
        if (this.tabWidthCache == width && this.tabHeightCache == height)  return;        
        
        background = PaintUtils.resizeGradient(background, 0, height);
        
        Area compound = new Area(new RoundRectangle2D.Float(10, 3, width-1-20, height-1, 20, 20));
        
        compound.add(new Area(new Rectangle2D.Float(0,height-10-1,width-1,height-1)));
        
        compound.subtract(new Area(new Arc2D.Float(-10,height-1-21,20,20,270,90,Arc2D.PIE)));
        compound.subtract(new Area(new Arc2D.Float(width-1-10,height-1-21,20,20,360,270,Arc2D.PIE)));
        
        this.tabAreaCache   = compound;
        this.tabWidthCache  = width;
        this.tabHeightCache = height;
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        
        cacheTabArea(width, height);

        if (raiseBottomClip) {
            g.setClip(0,0,width,height-1);
        }
        
        // Draw top bevel
        g.setPaint(this.topBevelBackground);
        g.fillRoundRect(10, 0, width-2-20, 20, 20, 20);
        
        // Draw top border
        g.setPaint(this.topBevelBorder);
        g.drawRoundRect(10, 0, width-2-20, 20, 20, 20);
        
        // Draw tab
        g.setPaint(background);
        g.fill(this.tabAreaCache);
        
        if (!raiseBottomClip) {        
            // Hack for Anthony to correct the antialiasing for the bottom border and tab mixing
            g.setPaint(new Color(0xca,0xca,0xca));
            g.drawLine(0,height-2,0,height-2);
            g.drawLine(width-1,height-2,width-1,height-2);
            g.setPaint(new Color(0xab,0xab,0xab));
            g.drawLine(1,height-2,1,height-2);
            g.drawLine(width-2,height-2,width-2,height-2);
        }
    }
}