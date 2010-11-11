package org.limewire.ui.swing.painter;

import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;

/**
 * Generic painter that paints a special border and a filled
 *  background which is possibly a gradient.
 */
public class ComponentBackgroundPainter<X> extends CompoundPainter<X> {

    public ComponentBackgroundPainter(Paint background, Paint border, Paint bevelLeft, Paint bevelTop1,
            Paint bevelTop2, Paint bevelRight, Paint bevelBottom, int arcWidth, int arcHeight,
            AccentType accentType) {
        
        RectanglePainter<X> textBackgroundPainter = new RectanglePainter<X>();
        
        textBackgroundPainter.setRounded(true);
        textBackgroundPainter.setFillPaint(background);
        textBackgroundPainter.setRoundWidth(arcWidth);
        textBackgroundPainter.setRoundHeight(arcHeight);
        textBackgroundPainter.setInsets(new Insets(2,2,2,2));
        textBackgroundPainter.setBorderPaint(null);
        textBackgroundPainter.setPaintStretched(true);
        textBackgroundPainter.setFillVertical(true);
        textBackgroundPainter.setFillHorizontal(true);
        textBackgroundPainter.setAntialiasing(true);
        textBackgroundPainter.setCacheable(true);
        
        setPainters(textBackgroundPainter, new BorderPainter<X>(arcWidth, arcHeight,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, accentType));
        
        setCacheable(true);
    }
    
}
