package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;

import javax.swing.ButtonModel;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;

/**
 * Background painter for a gradient button. 
 */
public abstract class ButtonBackgroundPainter extends AbstractPainter<JXButton> {
    
    protected DrawMode drawMode = DrawMode.FULLY_ROUNDED;
    
    protected Painter<JXButton> normalPainter;
    protected Painter<JXButton> clickedPainter;
    protected Painter<JXButton> hoveredPainter;
    protected Painter<JXButton> disabledPainter;
    
    protected Painter<JXButton> createPainter(Color gradientTop, Color gradientBottom, 
            Paint border, Paint bevelLeft, Paint bevelTop1, Paint bevelTop2, 
            Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight, AccentType accentType) {
        
        CompoundPainter<JXButton> compoundPainter = new CompoundPainter<JXButton>();
        
        RectanglePainter<JXButton> painter = new RectanglePainter<JXButton>();
        
        int shiftX1 = 0;
        int shiftX2 = 0;
        
        switch (this.drawMode) {
        
        case LEFT_ROUNDED :

            shiftX1 = 0;
            shiftX2 = -arcWidth+2;
            break;
            
        case RIGHT_ROUNDED :
            
            shiftX1 = -arcWidth-2;
            shiftX2 = 0;
            break;
            
        case UNROUNDED :
            
            shiftX1 = -arcWidth-2;
            shiftX2 = -arcWidth-2;
            break;
            
        }
        
        painter.setRounded(true);
        painter.setFillPaint(new GradientPaint(0,0, gradientTop, 0, 1, gradientBottom, false));
        painter.setRoundWidth(arcWidth);
        painter.setRoundHeight(arcHeight);
        painter.setInsets(new Insets(1,2+shiftX1,2,2+shiftX2));
        painter.setPaintStretched(true);
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
                
        BorderPainter borderPainter = new BorderPainter(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, accentType);
        borderPainter.setInsets(new Insets(0,shiftX1, 0, shiftX2));        
        
        compoundPainter.setPainters(painter, borderPainter);
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
    
    @Override
    public void doPaint(Graphics2D g, JXButton object, int width, int height) {
        if (!object.isEnabled() && disabledPainter != null) {
            this.disabledPainter.paint(g, object, width, height);
            return;
        }
        
        ButtonModel model = object.getModel();
        
        if(model.isPressed() || model.isSelected()) {
            this.clickedPainter.paint(g, object, width, height);
        } 
        else if (model.isRollover() || object.hasFocus()) {
            this.hoveredPainter.paint(g, object, width, height);
        } 
        else {
            this.normalPainter.paint(g, object, width, height);
        }        
    }
    
    
    /**
     * For creating buttons with different edge rounding properties.
     * <pre>
     *   Examples :     
     * 
     *       ( LEFT_ROUNDED |   | UNROUNDED |   | RIGHT_ROUNDED ) 
     *       
     *                         ( FULLY_ROUNDED )
     * </pre>
     */
    
    public enum DrawMode {
        FULLY_ROUNDED, RIGHT_ROUNDED, LEFT_ROUNDED, UNROUNDED 
    }
}