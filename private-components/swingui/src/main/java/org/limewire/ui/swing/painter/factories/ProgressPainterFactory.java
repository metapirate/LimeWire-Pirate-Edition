package org.limewire.ui.swing.painter.factories;

import javax.swing.JComponent;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.painter.AbstractPainter;

public interface ProgressPainterFactory {
    public AbstractPainter<JComponent> createBackgroundPainter();
    public AbstractPainter<JProgressBar> createForegroundPainter();
}
