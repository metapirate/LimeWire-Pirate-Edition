/**
 * 
 */
package org.limewire.ui.swing.components;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


public class PanelResizer extends ComponentAdapter {
    private final Resizable target;
    
    public PanelResizer(Resizable target) {
        this.target = target;
    }
    
    @Override
    public void componentResized(ComponentEvent e) {
        if(target.isVisible()) {
            target.resize();
        }
    }
}