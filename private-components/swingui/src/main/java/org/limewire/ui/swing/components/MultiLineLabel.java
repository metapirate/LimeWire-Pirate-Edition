package org.limewire.ui.swing.components;

import org.jdesktop.swingx.JXLabel;

public class MultiLineLabel extends JXLabel {
    
    public MultiLineLabel() {
        super();
        setLineWrap(true);
    }
    
    public MultiLineLabel(String text) {
        super(text);
        setLineWrap(true);
    }

    public MultiLineLabel(String text, int lineWidth) {
        super(text);
        setMaxLineSpan(lineWidth);
        setLineWrap(true);
    }
}
