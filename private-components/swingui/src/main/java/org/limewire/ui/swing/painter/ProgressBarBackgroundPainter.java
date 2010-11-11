package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JComponent;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;

/**
 * A painter for the standard background image of horizontal
 *  progress components.
 */
public class ProgressBarBackgroundPainter extends AbstractPainter<JComponent> {

    private final Paint border;
    private final Paint borderDisabled;
    private Paint background;
    
    private int heightCache = 0;
    
    public ProgressBarBackgroundPainter(Paint background, Paint border, Paint borderDisabled) {
        this.background = background;
        this.border = border;
        this.borderDisabled = borderDisabled;
        
        this.setAntialiasing(false);
        this.setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            this.background = PaintUtils.resizeGradient(background, 0, height-1);
        }
        
        g.setPaint(this.background);
        g.fillRect(0, 0, width-1, height-1);        
        
        g.setPaint(object.isEnabled() ? this.border : this.borderDisabled);
        g.drawRect(0, 0, width-1, height-1);
    }
}
