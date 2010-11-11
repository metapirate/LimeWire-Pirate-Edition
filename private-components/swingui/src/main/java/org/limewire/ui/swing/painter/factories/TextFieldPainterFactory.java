package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.Paint;

import javax.swing.JTextField;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.BasicTextFieldPromptPainter;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.FilterPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class TextFieldPainterFactory {

    @Resource private Color promptForeground; 
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color border;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    @Inject
    TextFieldPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    public AbstractPainter<JTextField> createBasicBackgroundPainter(AccentType accentType) {
        return new ComponentBackgroundPainter<JTextField>(Color.WHITE, border, bevelLeft, bevelTop1,
                bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                accentType);
    }
    
    public AbstractPainter<JTextField> createBasicBackgroundPainter(AccentType accentType, Paint border) {
        return new ComponentBackgroundPainter<JTextField>(Color.WHITE, border, bevelLeft, bevelTop1,
                bevelTop2, bevelRight, bevelBottom, arcWidth, arcHeight,
                accentType);
    }

    /**
     * Creates a background painter for the specified text field using the 
     * specified accent type.  The returned painter includes an X icon that may
     * be clicked to clear the field, and is suitable for filter fields. 
     */
    public Painter<JTextField> createClearableBackgroundPainter(
            PromptTextField textField, AccentType accentType) {
        
        // Create filter painter.
        FilterPainter<JTextField> filterPainter = new FilterPainter<JTextField>(
                arcWidth, arcHeight);
        
        // Install filter painter on text field.
        filterPainter.install(textField);
        
        // Create compound painter using area and border painters.
        CompoundPainter<JTextField> painter = new CompoundPainter<JTextField>();
        painter.setPainters(filterPainter, createBorderPainter(accentType));
        painter.setCacheable(true);
        
        return painter;
    }
    
    public BasicTextFieldPromptPainter<JTextField> createBasicPromptPainter() {
        return new BasicTextFieldPromptPainter<JTextField>(promptForeground);
    }

    /**
     * Creates a BorderPainter using the specified accent type.
     */
    private BorderPainter<JTextField> createBorderPainter(AccentType accentType) {
        return new BorderPainter<JTextField>(arcWidth, arcHeight, border,
                bevelLeft,  bevelTop1,  bevelTop2, bevelRight,  bevelBottom,
                accentType);
    }
}
