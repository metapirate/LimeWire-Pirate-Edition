package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Panel;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLayeredPane;

public abstract class OverlayPopupPanel extends Panel 
    implements Resizable, ComponentListener, Disposable {
    // heavy weight so it can be on top of other heavy weight components

    protected final JLayeredPane layeredPane;
    private final Component childPanel;

    public OverlayPopupPanel(JLayeredPane layeredPane) {
        this(layeredPane, null);
    }
    
    public OverlayPopupPanel(JLayeredPane layeredPane, Component childPanel) {
        
        this.layeredPane = layeredPane;
        this.childPanel = childPanel;

        if (childPanel != null) {
            setLayout(new BorderLayout());
            add(childPanel, BorderLayout.CENTER);

            // Match visiblity of the child
            childPanel.addComponentListener(new ComponentListener() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    setVisible(false);                
                }
                @Override
                public void componentMoved(ComponentEvent e) {
                }
                @Override
                public void componentResized(ComponentEvent e) {
                }
                @Override
                public void componentShown(ComponentEvent e) {
                    setVisible(true);
                }
            });

            // Should hide if the child is hidden
            if (!childPanel.isVisible()) {
                setVisible(false);
            }
        }
        
        layeredPane.add(this, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(this);

        resize();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (childPanel != null) {
            childPanel.setVisible(b);
        }
    }
    
    @Override
    public void dispose() {
        layeredPane.removeComponentListener(this);
        layeredPane.remove(this);
            
        if (childPanel instanceof Disposable) {
            ((Disposable) childPanel).dispose();
        }
    }
    
    public abstract void resize();

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
        resize();
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }
}
