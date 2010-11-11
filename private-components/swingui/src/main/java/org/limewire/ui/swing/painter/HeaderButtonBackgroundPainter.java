package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.util.PaintUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Initially unpainted button painter that when clicked or selected
 *  draws a centered gradient on the background
 *  and two divider divots on the left and right.
 */
public class HeaderButtonBackgroundPainter extends AbstractPainter<JXButton> {
    
    @Resource private Color innerSideBorder = PainterUtils.TRANSPARENT;
    @Resource private Color outterSideBorder = PainterUtils.TRANSPARENT;
    
    @Resource private int innerSideBorderFadeStep;
    @Resource private int outterSideBorderFadeStep;
    
    @Resource private Color backgroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color backgroundGradientMiddle = PainterUtils.TRANSPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRANSPARENT;

    private Paint topBackground;
    private Paint bottomBackground;
    
    private int cachedHeight = -1;
    
    public HeaderButtonBackgroundPainter() {
        GuiUtils.assignResources(this);
        
        topBackground = new GradientPaint(0,0,backgroundGradientTop,0,1,backgroundGradientMiddle,true);
        bottomBackground = new GradientPaint(0,0,backgroundGradientBottom,0,1,backgroundGradientMiddle,true);
        
        setAntialiasing(false);
        setCacheable(false);
    }
   
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        
        if (object.getModel().isPressed() || object.getModel().isSelected()) {
            
            if (cachedHeight != height) {
                cachedHeight = height;
                topBackground = PaintUtils.resizeGradient(topBackground, 0, height/2);
                bottomBackground = PaintUtils.resizeGradient(bottomBackground, 0, height/2); 
            }
        
            g.setPaint(topBackground);
            g.fillRect(0, 0, width, height/2);
            g.setPaint(bottomBackground);
            g.fillRect(0, height/2, width, height/2);
        
            int outerFadePoint = 9;
            int innerFadePoint = 11;
        
            g.setPaint(outterSideBorder);
            g.drawLine(0,outerFadePoint,0, height-outerFadePoint-1);
            g.drawLine(width-1,outerFadePoint,width-1, height-outerFadePoint-1);
            
            g.setPaint(innerSideBorder);
            g.drawLine(1,innerFadePoint,1, height-innerFadePoint-1);
            g.drawLine(width-2,innerFadePoint,width-2, height-innerFadePoint-1);
        
            Color strokeThreshold = PainterUtils.lighten(backgroundGradientTop, -30);
        
            Color outerStroke = outterSideBorder;
                
            for ( int i=0 ; i<outerFadePoint ; i++ ) {
                outerStroke = PainterUtils.lighten(outerStroke, strokeThreshold, outterSideBorderFadeStep);
            
                g.setPaint(outerStroke);
                g.drawLine(0, outerFadePoint-i, 0, outerFadePoint-i);
                g.drawLine(width-1, outerFadePoint-i, width-1, outerFadePoint-i);
                g.drawLine(0, height-outerFadePoint-1+i, 0, height-outerFadePoint-1+i);
                g.drawLine(width-1, height-outerFadePoint-1+i, width-1, height-outerFadePoint-1+i);
            }
        
            Color innerStroke = innerSideBorder;
        
            for ( int i=0 ; i<innerFadePoint ; i++ ) {
                innerStroke = PainterUtils.lighten(innerStroke, strokeThreshold, innerSideBorderFadeStep);
                g.setPaint(innerStroke);
                g.drawLine(1, innerFadePoint-i, 1, innerFadePoint-i);
                g.drawLine(width-2, innerFadePoint-i, width-2, innerFadePoint-i);
                g.drawLine(1, height-innerFadePoint-1+i, 1, height-innerFadePoint-1+i);
                g.drawLine(width-2, height-innerFadePoint-1+i, width-2, height-innerFadePoint-1+i);
            }
        }
    }
}
