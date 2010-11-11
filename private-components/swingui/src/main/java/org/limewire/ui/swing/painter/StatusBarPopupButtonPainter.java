package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class StatusBarPopupButtonPainter extends AbstractPainter<JXButton> {

    private final PopupVisibilityChecker visibilityChecker;
    private final Paint activeBackground;
    private final Paint activeBorder;
    private final DrawMode mode;
    
    @Resource private Color dividerForeground = PainterUtils.TRANSPARENT;
    @Resource private Color rolloverBackground = PainterUtils.TRANSPARENT;
    @Resource private Color rolloverBorder = PainterUtils.TRANSPARENT;
    
    public StatusBarPopupButtonPainter(PopupVisibilityChecker visibilityChecker,
            Paint activeBackground, Paint activeBorder, DrawMode mode) {
        GuiUtils.assignResources(this);
        
        this.visibilityChecker = visibilityChecker;
        this.activeBackground = activeBackground;
        this.activeBorder = activeBorder;
        this.mode = mode;
        
        setAntialiasing(false);
        setCacheable(false);
    }

    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        if(visibilityChecker.isPopupVisible()) {
            g.setPaint(activeBackground);
            g.fillRect(0, 0, width, height);
            g.setPaint(activeBorder);
            g.drawLine(0, 0, 0, height-1);
            g.drawLine(0, height-1, width-1, height-1);
            g.drawLine(width-1, 0, width-1, height-1);
        } else if (object.getModel().isRollover() && object.isEnabled()) {
            g.setPaint(rolloverBackground);
            g.fillRect(0, 2, width-1, height-2);
            g.setPaint(rolloverBorder);
            g.drawLine(0, 1, 0, height-1);
            g.drawLine(width-1, 1, width-1, height-1);
        }
        else {
            g.setPaint(dividerForeground);
            if (mode != DrawMode.LEFT_CONNECTING) {
                g.drawLine(0, 3, 0, height-4);
            }
            if (mode != DrawMode.RIGHT_CONNECTING) {
                g.drawLine(width-1, 3, width-1, height-4);
            }
        }
    }
    
    public static interface PopupVisibilityChecker {
        boolean isPopupVisible();
    }
    
    public enum DrawMode {
        NORMAL, LEFT_CONNECTING, RIGHT_CONNECTING; 
    }
}
