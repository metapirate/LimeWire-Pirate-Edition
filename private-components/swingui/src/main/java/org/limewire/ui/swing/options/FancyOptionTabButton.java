package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.components.ButtonMattePainter;
import org.limewire.ui.swing.options.actions.TabAction;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Paints a main button on the Option Panel.
 */
public class FancyOptionTabButton extends JXButton {
    
    @Resource
    private Color headerGradientTop;
    @Resource
    private Color headerGradientBottom;
    @Resource
    private Color fontColor;
    @Resource
    private Font font;
    
    public FancyOptionTabButton(TabAction action) {
        super(action);
        
        GuiUtils.assignResources(this);
        
        setFont(font);
        setForeground(fontColor);
        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setIconTextGap(0);
        setPreferredSize(new Dimension(getPreferredSize().width, 60));
        setGradients(headerGradientTop, headerGradientBottom);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorder(BorderFactory.createEmptyBorder(4,12,4,12));
        setOpaque(false);
    }
    
    public void setGradients(Color topGradient, Color bottomGradient) {
        getAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    repaint();
                }
            }
        });
        
        final Painter<JXButton> oldPainter = getBackgroundPainter();
        GradientPaint paint = new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                new Point2D.Double(0, 1), bottomGradient, false);
        setBackgroundPainter(new ButtonMattePainter(new MattePainter(paint, true)) {
            @Override
            public void paint(Graphics2D g, JXButton component, int width,
                    int height) {
                if(Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                    super.paint(g, component, width, height-1);
                } else {
                    oldPainter.paint(g, component, width, height);
                }
            }
        });
    }
}
