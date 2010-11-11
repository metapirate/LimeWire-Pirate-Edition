package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Background painter for a gradient button. 
 */
public class GreenButtonBackgroundPainter extends ButtonBackgroundPainter {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color backgroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color backgroundGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color highlightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color highlightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color clickGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color clickGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color border = PainterUtils.TRANSPARENT;
    @Resource private Color bevelTop1 = PainterUtils.TRANSPARENT;
    @Resource private Color bevelTop2 = PainterUtils.TRANSPARENT;
    @Resource private Color bevelLeft = PainterUtils.TRANSPARENT;
    @Resource private Color bevelRightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color bevelRightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color bevelBottom = PainterUtils.TRANSPARENT;
    
    public GreenButtonBackgroundPainter() {
        GuiUtils.assignResources(this);
                
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.border, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.border, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.border, bevelLeft, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.setCacheable(false);
    }
}