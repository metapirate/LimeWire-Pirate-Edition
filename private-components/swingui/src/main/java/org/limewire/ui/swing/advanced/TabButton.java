package org.limewire.ui.swing.advanced;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.components.ButtonMattePainter;

/**
 * A tab button that is used in a window with multiple tabs.  A TabButton
 * responds to changes in its action's selected state to display a color
 * gradient.
 * 
 * <p><b>NOTE</b>: This is an updated version of FancyOptionTabButton that 
 * accepts a plain Action and provides a public setGradients() method.  
 * TabButton could be moved into the ui.swing.components package.</p>
 */
public class TabButton extends JXButton {
    
    /**
     * Constructs a TabButton that uses the specified Action.
     */
    public TabButton(Action action) {
        super(action);
        
        // Set button attributes.
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setIconTextGap(0);
        setPreferredSize(new Dimension(Math.max(getPreferredSize().width, 70), 62));
        setBorder(BorderFactory.createEmptyBorder(4,12,4,12));
        
        // Add listener to repaint button when selected state changes.
        getAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    repaint();
                }
            }
        });
    }

    /**
     * Sets the gradients used to paint the button. 
     */
    public void setGradients(Color topGradient, Color bottomGradient) {
        // Get current painter.
        final Painter<JXButton> oldPainter = getBackgroundPainter();

        // Create gradient paint.
        Paint gradientPaint = new GradientPaint(new Point2D.Double(0, 0), 
            topGradient, new Point2D.Double(0, 1), bottomGradient, false);
        
        // Set background painter using gradient.
        setBackgroundPainter(new ButtonMattePainter(new MattePainter(gradientPaint, true)) {
            @Override
            public void paint(Graphics2D g, JXButton component, int width, int height) {
                if (Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                    super.paint(g, component, width, height-1);
                } else {
                    oldPainter.paint(g, component, width, height);
                }
            }
        });
    }
}
