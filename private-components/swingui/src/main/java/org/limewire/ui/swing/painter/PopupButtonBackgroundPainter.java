package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Background painter a simple 3 state rounded button.
 *  When the mouse is not over the component is left
 *  unpainted 
 */
public class PopupButtonBackgroundPainter extends AbstractPainter<JXButton> {
    
    final private int arcWidth;
    final private int arcHeight;
    final private Paint backgroundPressed;
    final private Paint backgroundRollover;
    
    public PopupButtonBackgroundPainter(Paint backgroundPressed, Paint backgroundRollover, 
            int arcWidth, int arcHeight) {

        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.backgroundPressed = backgroundPressed;
        this.backgroundRollover = backgroundRollover;
        
        this.setAntialiasing(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton button, int width, int height) {
        if (button.getModel().isPressed() || button.getModel().isSelected()) {
            g.setPaint(backgroundPressed);
            g.fillRoundRect(1, 0, width-2, height-1, arcWidth, arcHeight);
        }
        else if (button.getModel().isRollover() || button.hasFocus()) {
            g.setPaint(backgroundRollover);
            g.fillRoundRect(1, 0, width-2, height-1, arcWidth, arcHeight);
        }
    }
}