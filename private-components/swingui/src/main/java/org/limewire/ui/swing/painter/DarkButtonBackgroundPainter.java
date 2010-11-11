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
public class DarkButtonBackgroundPainter extends ButtonBackgroundPainter {
        
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    
    @Resource private Color border = PainterUtils.TRANSPARENT;
    
    @Resource private Color normalGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color normalGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color normalBevelRightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color normalBevelRightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color normalBevelBottom = PainterUtils.TRANSPARENT;
    
    @Resource private Color highlightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color highlightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color highlightBevelRightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color highlightBevelRightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color highlightBevelBottom = PainterUtils.TRANSPARENT;
    
    @Resource private Color clickGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color clickGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color clickBevelRightGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color clickBevelRightGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color clickBevelBottom = PainterUtils.TRANSPARENT;
    @Resource private Color clickBevelTop = PainterUtils.TRANSPARENT;
    
    @Resource private Color disabledGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color disabledGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color disabledBorderGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color disabledBorderGradientBottom = PainterUtils.TRANSPARENT;
    
   
    public DarkButtonBackgroundPainter(DrawMode mode, AccentType accentType) {
        GuiUtils.assignResources(this);
        
        this.drawMode = mode;
        
        GradientPaint normalRightGradient = new GradientPaint(0,0, this.normalBevelRightGradientTop, 
                0, 1, this.normalBevelRightGradientBottom, false);
        
        GradientPaint hoveredRightGradient = new GradientPaint(0,0, this.highlightBevelRightGradientTop, 
                0, 1, this.highlightBevelRightGradientBottom, false);
        
        GradientPaint clickedRightGradient = new GradientPaint(0,0, this.clickBevelRightGradientTop, 
                0, 1, this.clickBevelRightGradientBottom, false);
        
        GradientPaint disabledBorderGradient = new GradientPaint(0,0, this.disabledBorderGradientTop, 
                0, 1, this.disabledBorderGradientBottom, false);
        
        this.normalPainter = createPainter(this.normalGradientTop, this.normalGradientBottom,
                this.border, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT,
                normalRightGradient, this.normalBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.border,  PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, 
                hoveredRightGradient, this.highlightBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.border,  PainterUtils.TRANSPARENT, clickBevelTop, PainterUtils.TRANSPARENT, 
                clickedRightGradient, this.clickBevelBottom, this.arcWidth, this.arcHeight, accentType);
        
        this.disabledPainter = createPainter(this.disabledGradientTop, this.disabledGradientBottom,
                disabledBorderGradient,  PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, 
                PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, this.arcWidth, this.arcHeight, accentType);
        
        this.setCacheable(false);
    }
}