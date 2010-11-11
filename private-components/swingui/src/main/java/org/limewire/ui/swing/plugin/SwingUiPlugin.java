package org.limewire.ui.swing.plugin;

import javax.swing.JComponent;

/** A UI Plugin that has a renderable area and can be started or stopped. */
public interface SwingUiPlugin {
    
    /** Returns an area the plugin can render itself in. */
    JComponent getPluginComponent();
    
    /** Starts the plugin. */
    void startPlugin();
    
    /** Stops the plugin. */
    void stopPlugin();
    
    /** Returns the name of the plugin. */
    String getPluginName();

}
