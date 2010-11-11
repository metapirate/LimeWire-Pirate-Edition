package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * An implementation of SwingX Painter used to draw the horizontal 
 * progress bar foreground on a specified Swing component. 
 * The foreground constitutes the current progress and a knob 
 * for changing the value if the component is user modifiable.  
 * 
 * At this time, the only components supported are
 * JProgressBar and JSlider.
 */
public class ProgressBarForegroundPainter<X extends JComponent> extends AbstractPainter<X> {
    
    private final boolean drawHandle;
    
    private Paint foreground;
    private Paint disabledForeground;
    
    private final Paint upperAccent;

    private int heightCache = 0;
    
    public ProgressBarForegroundPainter(Paint foreground, Paint disabledForeground) {
        this(foreground, disabledForeground, PainterUtils.TRANSPARENT, false);
    }
    
    public ProgressBarForegroundPainter(Paint foreground, Paint disabledForeground, Paint upperAccent) {
        this(foreground, disabledForeground, upperAccent, false);
    }
    
    public ProgressBarForegroundPainter(Paint foreground, Paint disabledForeground, boolean drawHandle) {
        this(foreground, disabledForeground, PainterUtils.TRANSPARENT, drawHandle);
    }
    
    public ProgressBarForegroundPainter(Paint foreground, Paint disabledForeground,
            Paint upperAccent, boolean drawHandle) {
        
        this.drawHandle = drawHandle;
        
        this.foreground = foreground;
        this.disabledForeground = disabledForeground;
        this.upperAccent = upperAccent;
        
        this.setAntialiasing(false);
        this.setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        if (height != this.heightCache) {
            this.heightCache = height;
            
            this.foreground = PaintUtils.resizeGradient(this.foreground, 0, height-2);
            this.disabledForeground = PaintUtils.resizeGradient(this.disabledForeground, 0, height-2);
        }
        
        int progress = (int) ((width-3) * getPercentComplete(object));
                        
        if (object.isEnabled()) {
            g.setPaint(this.foreground);
            g.fillRect(1, 1, progress, height-2);
            g.setPaint(upperAccent);
            g.drawLine(1, 1, progress, 1);
        } 
        else {
            g.setPaint(this.disabledForeground);
            g.fillRect(1, 1, progress, height-2);
        }
        
        if (drawHandle && object.getMousePosition() != null) {
            if (progress == 0) {
                progress++;
            }
            g.setPaint(Color.WHITE);
            g.fillRect(progress, 0, 2, height);
        }
    }
    
    /**
     * Avoiding using 2 painter classes for exactly the same function.
     *  Shortcut method to avoid using providers for now.  There
     *  is no nice way to do this anyways since in order to be consistent
     *  any provider MUST match the object passed into doPaint.  
     */
    private static double getPercentComplete(Object object) {
        if (object instanceof JProgressBar) {
            return ((JProgressBar)object).getPercentComplete();
        }
     
        if (object instanceof JSlider) {
            JSlider slider = (JSlider)object;
            
            return   (double)(slider.getValue()   - slider.getMinimum()) 
                   / (double)(slider.getMaximum() - slider.getMinimum()); 
        }
        
        throw new IllegalArgumentException(
            "Progress bar painter does not support " + object.getClass().getName());
    }
}
