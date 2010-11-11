package org.limewire.ui.swing.components.decorators;

import java.awt.Color;
import java.awt.GradientPaint;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.painter.ProgressBarForegroundPainter;
import org.limewire.ui.swing.painter.factories.ProgressPainterFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class ProgressBarDecorator {
    
    private final Provider<ProgressPainterFactory> painterFactoryProvider;
    private ProgressPainterFactory painterFactory;
    
    @Inject
    ProgressBarDecorator(Provider<ProgressPainterFactory> painterFactory) {
        this.painterFactoryProvider = painterFactory;
    }
    
    
    public void decoratePlain(LimeProgressBar bar) {
        
        decorateGeneral(bar);
        
        if(painterFactory == null)
            painterFactory = painterFactoryProvider.get();
        
        AbstractPainter<JProgressBar> foregroundPainter =  painterFactory.createForegroundPainter();
        AbstractPainter<JComponent>   backgroundPainter = painterFactory.createBackgroundPainter();
        
        bar.setForegroundPainter(foregroundPainter);
        bar.setBackgroundPainter(backgroundPainter);
        
        foregroundPainter.setCacheable(bar.hasCacheSupport());
        backgroundPainter.setCacheable(bar.hasCacheSupport());
    }
    
    public static void decorateStaticBasic(LimeProgressBar bar) {

        decorateGeneral(bar);
        
        bar.setForegroundPainter(new ProgressBarForegroundPainter<JProgressBar>(
                new GradientPaint(0,0,new Color(0xcf,0xf8,0x8f), 0,1, new Color(0xaa,0xcb,0x75)),
                Color.GRAY));
        
        bar.setBackgroundPainter(new RectanglePainter<JComponent>(Color.WHITE, new Color(0x27,0x27,0x27)));
        
    }
    
    public static void decorateStaticPro(LimeProgressBar bar) {

        decorateGeneral(bar);
        
        bar.setForegroundPainter(new ProgressBarForegroundPainter<JProgressBar>(
                new GradientPaint(0,0,new Color(0x3883cf),0,1,new Color(0x1667a8)),
                Color.GRAY, new Color(0x3d9dff)));
        
        bar.setBackgroundPainter(new RectanglePainter<JComponent>(Color.WHITE, new Color(0x00316b)));
    }
    
    private static void decorateGeneral(LimeProgressBar bar) {
        bar.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
    }
}
