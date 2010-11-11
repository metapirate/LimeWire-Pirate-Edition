package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Paint;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PainterUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MessagePainterFactory {
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;

    private final GreenMessagePainterResources greenResources = new GreenMessagePainterResources();
    private final GrayMessagePainterResources grayResources = new GrayMessagePainterResources();
    
    @Inject
    public MessagePainterFactory() {
        GuiUtils.assignResources(this);
    }

    public Painter createGrayMessagePainter() {
        return createPainter(grayResources.backgroundGradientTop, grayResources.backgroundGradientBottom,
                grayResources.border, grayResources.bevelTop1, grayResources.bevelTop2, grayResources.bevelLeft,
                grayResources.bevelRightGradientTop, grayResources.bevelRightGradientBottom,
                grayResources.bevelBottom);
    }

    public Painter createGreenMessagePainter() {
        return createPainter(greenResources.backgroundGradientTop, greenResources.backgroundGradientBottom,
                greenResources.border, greenResources.bevelTop1, greenResources.bevelTop2, greenResources.bevelLeft,
                greenResources.bevelRightGradientTop, greenResources.bevelRightGradientBottom,
                greenResources.bevelBottom);
    }

    /**
     * Creates a painter for a rectangular region that does not render rounded
     * corners.
     */
    public Painter createGreenRectanglePainter() {
        return createRectanglePainter(grayResources.backgroundGradientTop, grayResources.backgroundGradientBottom,
                grayResources.border, grayResources.bevelTop1, grayResources.bevelTop2, grayResources.bevelLeft,
                grayResources.bevelRightGradientTop, grayResources.bevelRightGradientBottom,
                grayResources.bevelBottom);
    }

    private Painter createPainter(Color backgroundGradientTop, Color backgroundGradientBottom,
            Color border, Color bevelTop1, Color bevelTop2, Color bevelLeft,
            Color bevelRightGradientTop, Color bevelRightGradientBottom, Color bevelBottom) {

        Paint background = new GradientPaint(0, 0, backgroundGradientTop, 0, 1,
                backgroundGradientBottom);
        Paint bevelRight = new GradientPaint(0, 0, bevelRightGradientTop, 0, 1,
                bevelRightGradientBottom);

        return new ComponentBackgroundPainter(background, border,
                bevelLeft, bevelTop1, bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                AccentType.NONE);
    }

    /**
     * Creates a painter for a rectangular region that does not render rounded
     * corners.
     */
    private Painter createRectanglePainter(Color backgroundGradientTop, Color backgroundGradientBottom,
            Color border, Color bevelTop1, Color bevelTop2, Color bevelLeft,
            Color bevelRightGradientTop, Color bevelRightGradientBottom, Color bevelBottom) {

        Paint background = new GradientPaint(0, 0, backgroundGradientTop, 0, 1,
                backgroundGradientBottom);
        Paint bevelRight = new GradientPaint(0, 0, bevelRightGradientTop, 0, 1,
                bevelRightGradientBottom);

        // Create background painter without rounded corners.
        RectanglePainter backgroundPainter = new RectanglePainter();
        backgroundPainter.setRounded(true);
        backgroundPainter.setFillPaint(background);
        backgroundPainter.setRoundWidth(0);
        backgroundPainter.setRoundHeight(0);
        backgroundPainter.setInsets(new Insets(2,2,2,2));
        backgroundPainter.setBorderPaint(null);
        backgroundPainter.setPaintStretched(true);
        backgroundPainter.setFillVertical(true);
        backgroundPainter.setFillHorizontal(true);
        backgroundPainter.setAntialiasing(true);
        backgroundPainter.setCacheable(true);
        
        // Create border painter without rounded corners.  We specify shadow
        // accent to ensure that the entire region is painted.  We set the 
        // left inset so shadow appears only along right and bottom borders.
        BorderPainter borderPainter = new BorderPainter(0, 0,
                border,  bevelLeft,  bevelTop1,  bevelTop2, 
                bevelRight,  bevelBottom, AccentType.SHADOW);
        borderPainter.setInsets(new Insets(0, -1, 0, 0));
        
        // Return compound painter for background and border.
        CompoundPainter painter = new CompoundPainter();
        painter.setPainters(backgroundPainter, borderPainter);
        painter.setCacheable(true);
        return painter;
    }

    private static class GreenMessagePainterResources {
        @Resource protected Color backgroundGradientTop = PainterUtils.TRANSPARENT;
        @Resource protected Color backgroundGradientBottom = PainterUtils.TRANSPARENT;
        @Resource protected Color border = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelTop1 = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelTop2 = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelLeft = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelRightGradientTop = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelRightGradientBottom = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelBottom = PainterUtils.TRANSPARENT;

        public GreenMessagePainterResources() {
            GuiUtils.assignResources(this);
        }
    }

    private static class GrayMessagePainterResources {
        @Resource protected Color backgroundGradientTop = PainterUtils.TRANSPARENT;
        @Resource protected Color backgroundGradientBottom = PainterUtils.TRANSPARENT;
        @Resource protected Color border = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelTop1 = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelTop2 = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelLeft = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelRightGradientTop = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelRightGradientBottom = PainterUtils.TRANSPARENT;
        @Resource protected Color bevelBottom = PainterUtils.TRANSPARENT;

        public GrayMessagePainterResources() {
            GuiUtils.assignResources(this);
        }
    }
}
