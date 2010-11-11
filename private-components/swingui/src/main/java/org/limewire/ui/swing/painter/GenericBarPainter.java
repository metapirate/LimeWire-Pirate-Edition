package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Actuates background painting of "bars" such as header, status, and the top panel.
 *  Generally should be managed by a factory storing the different paint values,
 *  (in this case BarPainterFactory).
 */
public class GenericBarPainter<X> extends AbstractPainter<X> {

    private final Paint topBorder1;
    private final Paint topBorder2;
    
    private final Paint bottomBorder1;
    private final Paint bottomBorder2;
    
    private Paint gradient;
    
    private int cachedHeight = 0;
    
    public GenericBarPainter(Paint gradient) {
        this(gradient, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT,
                PainterUtils.TRANSPARENT,PainterUtils.TRANSPARENT);
    }
    
    public GenericBarPainter(Paint gradient, Paint topBorder1, 
            Paint topBorder2, Paint bottomBorder1, Paint bottomBorder2) {

        this.gradient = gradient;
        
        this.topBorder1 = topBorder1;
        this.topBorder2 = topBorder2;
        this.bottomBorder1 = bottomBorder1;
        this.bottomBorder2 = bottomBorder2;
        
        this.setCacheable(true);
        this.setAntialiasing(true);
    }

    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
    
        // Update gradient size if height has changed
        if (this.cachedHeight != height) {
            this.cachedHeight = height;
            
            this.gradient = PaintUtils.resizeGradient(gradient, 0, height);
        }
        
        //paint the gradient
        g.setPaint(this.gradient);
        g.fillRect(0, 0, width, height);

        // paint the top borders
        g.setPaint(this.topBorder1);
        g.drawLine(0, 0, width, 0);
        g.setPaint(this.topBorder2);
        g.drawLine(0, 1, width, 1);

        //paint the bottom borders
        g.setPaint(this.bottomBorder1);
        g.drawLine(0, height-1, width, height-1);
        g.setPaint(this.bottomBorder2);
        g.drawLine(0, height-2, width, height-2);
    }
    
}
