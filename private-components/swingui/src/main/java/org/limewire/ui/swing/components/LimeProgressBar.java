package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * A wrapper for JProgressBar that can accept a foreground and background painter
 *  The background painter is responsible for painting the basic component with
 *  no progress and any borders, the foreground painter paints the current progress.
 * <p>
 * NOTE: This component implements a lazy caching update model.  
 *  Directly changing the model will not fire repaints. setValue()
 *  and setEnabled() will clear the cache if the components
 *  state is changed.
 */
public class LimeProgressBar extends JProgressBar {

    /**
     * If false caching will be disabled on incoming painters.
     *  Only use this temporarily until the caching model
     *  is upgraded to account for the desired changes. 
     */
    private static final boolean CACHING_SUPPORTED = true;
    
    private AbstractPainter<JProgressBar> foregroundPainter;
    private AbstractPainter<JComponent> backgroundPainter;

    /**
     * Creates an unskinned instance with the default l&f and 
     *  properties used by JProgressBar.
     */
    public LimeProgressBar() {
    }
    
    /**
     * Creates an unskinned instance with a minimum and maximum value. 
     */
    public LimeProgressBar(int min, int max) {
        super(min, max);        
    }

    /**
     * Sets a painter for painting the progress portion.
     * <p> 
     * Both background and foreground painter must be set to have an effect.
     */
    public void setForegroundPainter(AbstractPainter<JProgressBar> painter) {
        this.foregroundPainter = painter;
        painter.setCacheable(hasCacheSupport());
    }

    /**
     * Sets the painter that will be used to draw the components background
     *  and border.
     * <p>
     * Both background and foreground painter must be set to have an effect.
     */
    public void setBackgroundPainter(AbstractPainter<JComponent> painter) {
        this.backgroundPainter = painter;
    }

    /**
     * Note: This component implements a lazy caching update model.  
     *        Changing the model directly will not fire repaints.
     */
    public boolean hasCacheSupport() {
        return CACHING_SUPPORTED;
    }
    
    @Override 
    public void setValue(int v) {
        if (this.getValue() != v) {
            this.foregroundPainter.clearCache();
            this.backgroundPainter.clearCache();
        }
        super.setValue(v);
    }
    
    @Override
    public void setEnabled(boolean b) {
        if (this.isEnabled() != b) {
            this.foregroundPainter.clearCache();
            this.backgroundPainter.clearCache();
        }
        super.setEnabled(b);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (foregroundPainter == null || backgroundPainter == null) {
            super.paintComponent(g);
        }
        else {
            backgroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
            foregroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
        }
	    }
}
