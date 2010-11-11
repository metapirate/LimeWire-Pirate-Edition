package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JSlider;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.painter.ProgressBarBackgroundPainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

/**
 * Factory for managing the painters that skin
 *  JSlider.
 */
public class SliderPainterFactory {

    @Resource private Color mediaSliderBorder = PainterUtils.TRANSPARENT;
    @Resource private Color mediaSliderBackgroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color mediaSliderBackgroundGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color mediaSliderForegroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color mediaSliderForegroundGradientBottom = PainterUtils.TRANSPARENT;
    
    SliderPainterFactory() { 
        GuiUtils.assignResources(this);
    }
    
    /**
     * Creates a painter for the standard media slider background.
     */
    public AbstractPainter<JComponent> createMediaBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
                new GradientPaint(0,0,mediaSliderBackgroundGradientTop,0,1,mediaSliderBackgroundGradientBottom),
                mediaSliderBorder, mediaSliderBorder);
    }
    
    /**
     * Creates a foreground painter for a draggable media slider.
     *  This takes care of drawing the slider knob and current
     *  slider progress. 
     * <p>
     * Should not be cachable so the slider nob can be repainted
     *  easily on mouse events. 
     */
    public AbstractPainter<JSlider> createMediaForegroundPainter() {
        
        ProgressBarForegroundPainter<JSlider> painter =  new ProgressBarForegroundPainter<JSlider>(
             new GradientPaint(0,0,mediaSliderForegroundGradientTop,0,1,mediaSliderForegroundGradientBottom),
             Color.GRAY, true);
        
        painter.setCacheable(false);
        
        return painter;
    }
    
}
