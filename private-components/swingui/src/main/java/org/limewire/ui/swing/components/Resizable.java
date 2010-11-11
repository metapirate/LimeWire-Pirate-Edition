package org.limewire.ui.swing.components;

/** A component that can be resized */
public interface Resizable {
    
    /** Instructs the component to resize. */
    public void resize();
    
    /** Determines if the component is currently visible. */
    public boolean isVisible();
}
