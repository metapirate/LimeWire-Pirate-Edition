package org.limewire.ui.swing.components;

import java.awt.Graphics2D;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;

/**
 * A workaround for MattePainter not being generic (and therefore not being able
 * to be applied to a button.
 */
public class ButtonMattePainter implements Painter<JXButton> {
    
    private final MattePainter mattePainter;
    
    public ButtonMattePainter(MattePainter mattePainter) {
        this.mattePainter = mattePainter;
    }
    
    @Override
    public void paint(Graphics2D g, JXButton object, int width, int height) {
        mattePainter.paint(g, object, width, height);
    }

}
