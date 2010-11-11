package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.painter.ProgressBarBackgroundPainter;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class ProgressPainterFactoryImpl implements ProgressPainterFactory {
    
    @Resource private Color barBorderDisabled = PainterUtils.TRANSPARENT;
    @Resource private Color barBackgroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color barBackgroundGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color barDisabledForegroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color barDisabledForegroundGradientBottom = PainterUtils.TRANSPARENT;
    @Resource private Color barUpperAccent = PainterUtils.TRANSPARENT;
    
    @Resource private Color proBarBorder = PainterUtils.TRANSPARENT;
    @Resource private Color proBarForegroundGradientTop = PainterUtils.TRANSPARENT;
    @Resource private Color proBarForegroundGradientBottom = PainterUtils.TRANSPARENT;  
    
    public ProgressPainterFactoryImpl() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    public AbstractPainter<JComponent> createBackgroundPainter() {
        return new ProgressBarBackgroundPainter(
                new GradientPaint(0,0,this.barBackgroundGradientTop,0,1,this.barBackgroundGradientBottom),
                this.proBarBorder, this.barBorderDisabled);
    }
    
    @Override
    public AbstractPainter<JProgressBar> createForegroundPainter() {
        return new ProgressBarForegroundPainter<JProgressBar>(
                new GradientPaint(0,0,this.proBarForegroundGradientTop,0,1,this.proBarForegroundGradientBottom),
                new GradientPaint(0,0,this.barDisabledForegroundGradientTop,0,1,this.barDisabledForegroundGradientBottom),
                barUpperAccent);
    }
}
