package org.limewire.ui.swing.player;

import java.awt.Container;

/** Defines a factory for creating the video player panel.
*/
interface VideoPanelFactory {
   
   /**
    * Creates a new VideoPanel using the specified videoRenderer.
    */
   public VideoPanel createVideoPanel(Container videoRenderer);
}
