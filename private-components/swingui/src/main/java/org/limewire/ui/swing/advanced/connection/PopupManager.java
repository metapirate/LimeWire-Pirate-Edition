package org.limewire.ui.swing.advanced.connection;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * A manager for a popup window.  PopupManager is associated with a 
 * PopupProvider that supplies the content for the popup window. 
 */
public class PopupManager {
    private static final int POPUP_DURATION = 4000;
    
    private final PopupProvider popupProvider;
    private JPopupMenu popup;
    private Timer exitTimer;

    /**
     * Constructs a PopupManager with the specified popup content provider.
     */
    public PopupManager(PopupProvider popupProvider) {
        this.popupProvider = popupProvider;
    }

    /**
     * Displays a popup window over the specified owner component.  The 
     * specified location is relative to the component origin.  The popup is 
     * automatically dismissed after four seconds, or when the popup is
     * clicked. 
     */
    public void showTimedPopup(Component owner, int x, int y) {
        // Get popup content.
        Component content = popupProvider.getPopupContent();
        
        if ((popup == null) && (content != null)) {
            // Add listener to dismiss popup on mouse click.
            content.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    hidePopup();
                }
            });

            // Create popup and display.
            popup = createPopup(content);
            popup.show(owner, x, y);
            
            // Start timer to hide popup.
            startExitTimer();
        }
    }
    
    /**
     * Hides the popup window.
     */
    public void hidePopup() {
        // Hide existing popup and reset.
        if (popup != null) {
            popup.setVisible(false);
            popup = null;
        }
        
        // Stop exit timer.
        stopExitTimer();
    }
    
    /**
     * Creates a popup window containing the specified component.
     */
    private JPopupMenu createPopup(Component component) {
        // Create popup.  We only display the border for the popup component,
        // so we remove it from the popup container.
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        popupMenu.setFocusable(false);
        popupMenu.add(component);
        
        // Add listener to clear reference when popup is hidden.
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                popup = null;
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        return popupMenu;
    }
    
    /**
     * Starts the timer to hide the popup window.
     */
    private void startExitTimer() {
        // Stop existing timer.
        stopExitTimer();

        // Create new timer to hide popup.
        exitTimer = new Timer(POPUP_DURATION, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hidePopup();
            }
        });
        
        // Start timer.
        exitTimer.start();
    }
    
    /**
     * Stops the timer to hide the popup window.
     */
    private void stopExitTimer() {
        if (exitTimer != null) {
            exitTimer.stop();
            exitTimer = null;
        }
    }
    
    /**
     * Defines a component that provides content for a popup window. 
     */
    public static interface PopupProvider {
        /**
         * Returns the content for a popup window. 
         */
        public Component getPopupContent();
    }
}
