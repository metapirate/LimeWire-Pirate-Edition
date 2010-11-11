package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.GradientPaint;

import org.limewire.ui.swing.painter.ButtonBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Resourceless button class for the intent screen.
  */
public class IntentGreenButtonBackgroundPainter extends ButtonBackgroundPainter {
    
    private int arcWidth = 6;
    private int arcHeight = 6;
    private Color backgroundGradientTop = new Color(0xc7ee89);
    private Color backgroundGradientBottom = new Color(0xbce282);
    private Color highlightGradientTop = new Color(0xdafea3);
    private Color highlightGradientBottom = new Color(0xbde383);
    private Color clickGradientTop = new Color(0xcff09a);
    private Color clickGradientBottom = new Color(0xbde282);
    private Color borderColour = new Color(0x696969);
    private Color bevelTop1 = new Color(0xe7f8cc);
    private Color bevelTop2 = PainterUtils.TRANSPARENT;
    private Color bevelLeft = new Color(0xd3efa8);
    private Color bevelRightGradientTop = new Color(0xabe551);
    private Color bevelRightGradientBottom = new Color(0x9bd344);
    private Color bevelBottom = new Color(0x88ca21);
    
    public IntentGreenButtonBackgroundPainter() {
                
        GradientPaint gradientRight = new GradientPaint(0,0, this.bevelRightGradientTop, 
                0, 1, this.bevelRightGradientBottom, false);
        
        this.normalPainter = createPainter(this.backgroundGradientTop, this.backgroundGradientBottom,
                this.borderColour, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.hoveredPainter = createPainter(this.highlightGradientTop, this.highlightGradientBottom,
                this.borderColour, bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.clickedPainter = createPainter(this.clickGradientTop, this.clickGradientBottom,
                this.borderColour, bevelLeft, PainterUtils.TRANSPARENT, PainterUtils.TRANSPARENT, 
                gradientRight, this.bevelBottom, this.arcWidth, this.arcHeight, AccentType.NONE);
        
        this.setCacheable(false);
    }
}