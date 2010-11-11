package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Paints a rounded border like box with one pixel inner 
 *  shadowing/beveling.  Used a the base painter of almost
 *  all components.
 * <p> 
 *  
 * NOTE: This painter does NOT use resources for
 *        the colours defined by the accents.  This
 *        is the most commonly used painter in the 
 *        application and the idea was to keep it as 
 *        simple as possible.  If resources are desired
 *        the class must be refactored with a factory.
 *        This class MUST NOT import resources directly
 *        since it is used early in the startup cycle. 
 */
public class BorderPainter<X> extends AbstractPainter<X> {

    private final int arcWidth;
    private final int arcHeight;
    private final Paint border;
    private final Paint bevelTop1;
    private final Paint bevelTop2;
    private final Paint bevelBottom;
    
    private Paint bevelLeft;
    private Paint bevelRight;
    
    private int tabHeightCache = -1;
    
    private final Paint accentPaint1;
    private final Paint accentPaint2;
    private final Paint accentPaint3;
    
    private Insets insets = PainterUtils.BLANK_INSETS;
    
    private final AccentType accentType;
    
    // DO NOT CONVERT THESE TO RESOURCES IN THIS CLASS
    private static final Paint BUBBLE_PAINT1 = new Color(0xeeeeee);
    private static final Paint BUBBLE_PAINT2 = new Color(0xededed);
    private static final Paint BUBBLE_PAINT3 = new Color(0xf0f0f0);
    private static final Paint SHADOW_PAINT1 = new Color(0x5f5f5f);
    private static final Paint SHADOW_PAINT2 = new Color(0x5e5e5e);
    private static final Paint SHADOW_PAINT3 = new Color(0x646464);
    private static final Paint GREEN_SHADOW_PAINT1 = new Color(0xc3d9a1);
    private static final Paint GREEN_SHADOW_PAINT2 = new Color(0xb9d78d);
    private static final Paint GREEN_SHADOW_PAINT3 = new Color(0xe1eecc);
    
    public BorderPainter(int arcWidth, int arcHeight, Paint border, 
            Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, AccentType accentType) {
        
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.border = border;
        this.bevelLeft = bevelLeft;
        this.bevelTop1 = bevelTop1;
        this.bevelTop2 = bevelTop2;
        this.bevelRight = bevelRight;
        this.bevelBottom = bevelBottom;
        
        this.accentType = accentType;
        
        switch (accentType) {
        
        case BUBBLE :
            
            accentPaint1 = BUBBLE_PAINT1;
            accentPaint2 = BUBBLE_PAINT2;
            accentPaint3 = BUBBLE_PAINT3;
            break;
            
        case SHADOW :
            
            accentPaint1 = SHADOW_PAINT1;
            accentPaint2 = SHADOW_PAINT2;
            accentPaint3 = SHADOW_PAINT3;
            break;
            
        case GREEN_SHADOW :    
            
            accentPaint1 = GREEN_SHADOW_PAINT1;
            accentPaint2 = GREEN_SHADOW_PAINT2;
            accentPaint3 = GREEN_SHADOW_PAINT3;
            break;
            
        default:
        
            accentPaint1 = PainterUtils.TRANSPARENT;
            accentPaint2 = PainterUtils.TRANSPARENT;
            accentPaint3 = PainterUtils.TRANSPARENT;
            
        }
        
        this.setCacheable(true);
    }

    public int getArcHeight() {
        return arcHeight;
    }

    public int getArcWidth() {
        return arcWidth;
    }
    
    /** 
     * Allows the painting to be offset by certain values to
     *  remove rounding as desired on the sides. Sides that
     *  fall painted offscreen will be capped with the normal
     *  border.
     * <p> 
     * NOTE: at the moment only horizonal insets are supported
     *        and capping will only work properly if the inset
     *        is larger than the arc size (ie. can not correctly cap
     *        partially flattened edges)
     * <pre>       
     *  Example: setInsets(0,-10,0,-10)  
     *           - left side will be moved 10 pixels off the 
     *              canvas and thus be cut off, left side will 
     *              be capped
     *           - right side will be moved 10 pixels off the canvas
     *              and thus be cut off, right side will be capped
     *              
     *           setInsets(-10,0,10,0)
     *           - will have no effect at this time
     * </pre>
     */
    public void setInsets(Insets insets) {
        this.insets = insets;
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        
        int ix1 = insets.left;
        int ix2 = insets.right;
        
        int singleArcHeight = arcHeight/2;
        
        // Draw upper bevels
        g.setClip(0+ix1, 0, width-2-ix1-ix2, 7);
        g.setPaint(bevelTop2);
        g.drawRoundRect(1+ix1, 2, width-2-ix1-ix2, height-5, arcWidth, arcHeight);
        g.setPaint(bevelTop1);
        g.drawRoundRect(1+ix1, 1, width-3-ix1-ix2, height-4, arcWidth, arcHeight);
        
        // Update gradients if height has changed
        if (tabHeightCache != height) {
            bevelLeft = PaintUtils.resizeGradient(bevelLeft, 0, height-singleArcHeight+1);
            bevelRight = PaintUtils.resizeGradient(bevelRight, 0, height-singleArcHeight+1);
            tabHeightCache = height;
        }
        
        // Draw side and bottom bevels
        g.setClip(0+ix1, singleArcHeight, width-2-ix1-ix2, height);
        g.setPaint(bevelBottom);        
        g.drawRoundRect(1+ix1, 1, width-4-ix1-ix2, height-4, arcWidth, arcHeight);
        g.setClip(0+ix1, singleArcHeight-1, width-2-ix1-ix2, height);
        g.setPaint(bevelLeft);
        g.drawLine(2+ix1,singleArcHeight-1,2+ix1,height-singleArcHeight);
        g.setPaint(bevelRight);
        g.drawLine(width-3-ix2,singleArcHeight-1,width-3-ix2,height-singleArcHeight);
                
        
        if (this.accentType != AccentType.NONE) {
            // Draw the bottom accent bubble or shadow
        
            g.setClip(0+ix1, singleArcHeight, width-ix1-ix2, height);
            g.setPaint(accentPaint3);
            g.drawRoundRect(0+ix1, 0, width-1-ix1-ix2, height-1, arcWidth, arcHeight);
            g.setPaint(accentPaint2);        
            g.drawLine(0+ix1,singleArcHeight,0+ix1,height/2);
            g.drawLine(width-1-ix2,singleArcHeight,width-1-ix2,height/2);
            g.setPaint(accentPaint1);
            g.drawLine(0+ix1,height/2,0+ix1,height-singleArcHeight);
            g.drawLine(width-1-ix2,height/2,width-1-ix2,height-singleArcHeight);
        }
         
        g.setClip(0+ix1, 0, width-ix1-ix2, height);
        
        // Draw final border
        g.setPaint(PaintUtils.resizeGradient(border, 0, height));
        g.drawRoundRect(1+ix1, 0, width-3-ix1-ix2, height-2, arcWidth, arcHeight);
        
        // Cap the left border if it is not rounded on the left        
        if (ix1 < 0) {
            g.drawLine(0, 1, 0, height-2);
        }

        // Cap the right border if it is not rounded on the right        
        if (ix2 < 0) {
            GradientPaint spanGradient 
            = new GradientPaint(0,1, PainterUtils.getColour(bevelTop1), 
                    0, height-3, PainterUtils.getColour(bevelBottom), false);
            
            g.setPaint(spanGradient);
            g.drawLine(width-1, 2, width-1, height-3);
        }
    }
    
    /**
     * Specifies which accent to draw around the bottom edge of a rounded border.
     *  Accents are used to add emphasis or depth to a component but at this time
     *  there are only the few preset types to choose in this enum.
     */
    public enum AccentType {
        
        /**
         * Standard shadow -- a gray bottom outline -- to be used
         *  on dark coloured panels.
         */
        SHADOW, 
        
        /**
         * Similar to the standard shadow but with a green tinge
         *  that is used on green backgrounds.
         */
        GREEN_SHADOW, 
        
        /**
         * A kind of bubble that makes a button look like it is popping
         *  out of a panel.  This bubble effect only looks good on
         *  lightly coloured panels.
         */
        BUBBLE,
        
        /**
         * No accent, no shadow, no bubble, this works everywhere but 
         *  looks a little bit boring.  Used on panels with non standard
         *  colouring or buttons where no emphasis is needed.   
         */
        NONE
    }

}
