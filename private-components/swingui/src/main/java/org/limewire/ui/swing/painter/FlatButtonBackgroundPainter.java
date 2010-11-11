package org.limewire.ui.swing.painter;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

public class FlatButtonBackgroundPainter extends RectanglePainter<JXButton>  {

    @Resource int arcHeight = 0;
    @Resource int arcWidth = 0;
    @Resource Color background = PainterUtils.TRANSPARENT;
    @Resource Color border = PainterUtils.TRANSPARENT;
    
    public FlatButtonBackgroundPainter() {
        
        GuiUtils.assignResources(this);
        
        setAntialiasing(true);
        setCacheable(true);
        
        setRounded(true);
        setFillPaint(background);
        setBorderPaint(border);
        setRoundHeight(arcHeight);
        setRoundWidth(arcWidth);
        setFillVertical(true);
    }

}
