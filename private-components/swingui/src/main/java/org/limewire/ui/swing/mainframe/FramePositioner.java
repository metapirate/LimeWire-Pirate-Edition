package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.JFrame;

import org.limewire.ui.swing.settings.SwingUiSettings;

class FramePositioner {

    /**
     * The last state of the X/Y location and the time it was set.
     * This is necessary to preserve the maximize size & prior size,
     * as on Windows a move event is occasionally triggered when
     * maximizing, prior to the state actually becoming maximized.
     */
    private WindowState lastState;
    
    /** The frame this is listening to. */
    private JFrame frame;
    
    void initialize(JFrame frame) {
        assert this.frame == null : "already initialized on a frame!";
        this.frame = frame;
        listenToWindowPosition();
    }

    
    void setWindowPosition() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        
        int appWidth  = Math.min(screenSize.width-insets.left-insets.right, SwingUiSettings.APP_WIDTH.getValue());
        int appHeight = Math.min(screenSize.height-insets.top-insets.bottom, SwingUiSettings.APP_HEIGHT.getValue());

        // TODO: Get a real minimum size? 
        frame.setMinimumSize(new Dimension(500, 500));
        frame.setSize(new Dimension(appWidth, appHeight));
        
        // Set the location of our window based on whether or not
        // the user has run the program before, and therefore may have 
        // modified the location of the main window.
        if(SwingUiSettings.POSITIONS_SET.getValue()) {
            
            int locX = Math.max(insets.left, SwingUiSettings.WINDOW_X.getValue());
            int locY = Math.max(insets.top, SwingUiSettings.WINDOW_Y.getValue());
        
            // Make sure the Window is visible and not for example 
            // somewhere in the very bottom right corner.
            if (locX+appWidth > screenSize.width) {
                locX = Math.max(insets.left, screenSize.width - insets.left - insets.right - appWidth);
            }
            
            if (locY+appHeight > screenSize.height) {
                locY = Math.max(insets.top, screenSize.height - insets.top - insets.bottom - appHeight);
            }
        
            frame.setLocation(locX, locY);
            
        } else {
            frame.setLocationRelativeTo(null);
        }
        
        //re-maximize if we shutdown while maximized.
        if(SwingUiSettings.MAXIMIZE_WINDOW.getValue() 
                && Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
        
        frame.validate();
    }    
    
    private void listenToWindowPosition() {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                lastState = new WindowState();
                saveWindowState();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                saveWindowState();
            }
        });
        
        frame.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                saveWindowState();
            }
        });
        
        // TODO: Is this listener necessary?  I don't think it is.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowState();
            }
        });
    }    
    
    /** Saves the state of the Window to settings. */
    private void saveWindowState() {
        int state = frame.getExtendedState();
        if(state == Frame.NORMAL) {
            // save the screen size and location 
            Dimension dim = frame.getSize();
            if((dim.height > 100) && (dim.width > 100)) {
                Point loc = frame.getLocation();
                SwingUiSettings.APP_WIDTH.setValue(dim.width);
                SwingUiSettings.APP_HEIGHT.setValue(dim.height);
                SwingUiSettings.WINDOW_X.setValue(loc.x);
                SwingUiSettings.WINDOW_Y.setValue(loc.y);
                SwingUiSettings.MAXIMIZE_WINDOW.setValue(false);
                SwingUiSettings.POSITIONS_SET.setValue(true);
            }
        } else if( (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
            SwingUiSettings.MAXIMIZE_WINDOW.setValue(true);
            if(lastState != null && lastState.time == System.currentTimeMillis()) {
                SwingUiSettings.POSITIONS_SET.setValue(true);
                SwingUiSettings.WINDOW_X.setValue(lastState.x);
                SwingUiSettings.WINDOW_Y.setValue(lastState.y);
                lastState = null;
            }
        }
    }    
   
    /** simple state. */
    private static class WindowState {
        private final int x;
        private final int y;
        private final long time;
        WindowState() {
            x = SwingUiSettings.WINDOW_X.getValue();
            y = SwingUiSettings.WINDOW_Y.getValue();
            time = System.currentTimeMillis();
        }
    }
}
