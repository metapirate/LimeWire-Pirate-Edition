package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Background painter for an initially unstyled button a clicked state that 
 *  forms a connectable rounded tab for attachment to a popup box
 *  below. 
 */
public class DropDownHeaderButtonBackgroundPainter extends AbstractPainter<JXButton> {

    @Resource private Color background = PainterUtils.TRANSPARENT;
    @Resource private Color outsideBorder = PainterUtils.TRANSPARENT;
    @Resource private Color outsideBorderCurve1 = PainterUtils.TRANSPARENT;
    @Resource private Color outsideBorderCurve2 = PainterUtils.TRANSPARENT;
    @Resource private Color insideBorder = PainterUtils.TRANSPARENT;
    
    public DropDownHeaderButtonBackgroundPainter() {
        GuiUtils.assignResources(this);
        
        this.setCacheable(false);
        this.setAntialiasing(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        // Button is unstyled unless it is selected or pressed
        if (object.getModel().isPressed() || object.getModel().isSelected()) {
            g.setPaint(background);
            g.fillRect(1, 1, width-3, height-2);
            
            g.setPaint(outsideBorder);
            g.drawLine(0,3, 0, height-1);
            g.drawLine(4,0, width-6, 0);
            g.drawLine(width-2, 3, width-2, height-4);
            g.drawLine(1,1,1,1);
            g.drawLine(width-2-1, 1, width-2-1, 1);
            
            g.setPaint(PainterUtils.lighten(outsideBorder,10));
            g.drawLine(width-2, height-3, width-2, height-3);
            g.drawLine(3,0,3,0);
            g.drawLine(width-5, 0, width-5, 0);
            
            g.setPaint(outsideBorderCurve1);
            g.drawLine(0,2,1,2);
            g.drawLine(2,0,2,1);
            g.drawLine(width-4, 0, width-4, 1);
            g.drawLine(width-3, 2, width-2, 2);
            
            g.setPaint(outsideBorderCurve2);
            g.drawLine(width-1, height-2, width-1, height-2);
            g.setPaint(PainterUtils.lighten(outsideBorderCurve2, 10));
            g.drawLine(width-2, height-2, width-2, height-2);
            
            g.setPaint(insideBorder);
            g.drawLine(width-3, 3, width-3, height-4);
            g.setPaint(PainterUtils.lighten(insideBorder, 10));
            g.drawLine(width-3, height-3, width-3, height-3);
            
        }
    }

}
