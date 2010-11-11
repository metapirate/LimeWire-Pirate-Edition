package org.limewire.ui.swing.components;

import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;

/**
 * Creates a widget to display RemoteHosts and perform actions on the hosts.
 */
public interface RemoteHostWidgetFactory {
    
    /** Creates a RemoteHostWidget. How data is displayed changes
      * based on the type of RemoteWidgetType.
      */
    RemoteHostWidget create(RemoteWidgetType type);
}
