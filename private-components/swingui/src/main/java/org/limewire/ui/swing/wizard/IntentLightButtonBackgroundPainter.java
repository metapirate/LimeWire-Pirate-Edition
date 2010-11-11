package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.GradientPaint;

import org.limewire.ui.swing.painter.ButtonBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Resourceless button class for the intent screen.
  */
public class IntentLightButtonBackgroundPainter extends ButtonBackgroundPainter {

    private int arcWidth = 6;
    private int arcHeight = 6;
    private Color backgroundGradientTop = new Color(0xffffff);
    private Color backgroundGradientBottom = new Color(0xdcdcdc);
    private Color highlightGradientTop = new Color(0xffffff);
    private Color highlightGradientBottom = new Color(0xc7c7c7);
    private Color clickGradientTop = new Color(0xc7c7c7);
    private Color clickGradientBottom = new Color(0xffffff);
    private Color borderColour = new Color(0x696969);
    private Color bevelTop1 = new Color(0xe1e1e1);
    private Color bevelTop2 = new Color(0xfefefe);
    private Color bevelLeft = new Color(0xd3efa8);
    private Color bevelRightGradientTop = new Color(0xd8d8d8);
    private Color bevelRightGradientBottom = new Color(0xc2c2c2);
    private Color bevelBottom = new Color(0xb8b8b8);
    

    public IntentLightButtonBackgroundPainter() {
                
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