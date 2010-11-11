package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;

/**
 * An extension of JWindow that can be used as a popup window.  PopupWindow
 * may be displayed using an animation that opens and closes the popup like a 
 * window shade.
 * 
 * Note that when a PopupWindow is opened, it installs a mouse listener on 
 * the glass pane of its owner window.  This handles mouse pressed events
 * to automatically close the popup.
 */
public class PopupWindow extends JWindow {
    private static final String CLOSE_ACTION_KEY = "closeWindow";
    private static final int DURATION = 250;
    private static final int RESOLUTION = 30;

    private final OwnerListener ownerListener = new OwnerListener();
    
    private boolean animated = true;
    private Animator animator;
    
    /**
     * Creates a window with no specified owner.
     */
    public PopupWindow() {
        super();
        initialize();
    }

    /**
     * Creates a window with the specified owner frame.
     */
    public PopupWindow(Frame owner) {
        super(owner);
        initialize();
    }

    /**
     * Creates a window with the specified GraphicsConfiguration of a screen
     * device.
     */
    public PopupWindow(GraphicsConfiguration gc) {
        super(gc);
        initialize();
    }

    /**
     * Creates a window with the specified owner window.
     */
    public PopupWindow(Window owner) {
        super(owner);
        initialize();
    }

    /**
     * Creates a window with the specified owner window and GraphicsConfiguration
     * of a screen device.
     */
    public PopupWindow(Window owner, GraphicsConfiguration gc) {
        super(owner, gc);
        initialize();
    }
    
    /**
     * Initializes the window by installing listeners.
     */
    private void initialize() {
        // Add window listener to install actions when opened, and clean up
        // listeners when closed.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                uninstallInputActions();
                Window owner = getOwner();
                if (owner instanceof RootPaneContainer) {
                    Component glassPane = ((RootPaneContainer) owner).getGlassPane();
                    owner.removeComponentListener(ownerListener);
                    glassPane.removeMouseListener(ownerListener);
                    glassPane.setVisible(false);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                installInputActions();
                Window owner = getOwner();
                if (owner instanceof RootPaneContainer) {
                    Component glassPane = ((RootPaneContainer) owner).getGlassPane();
                    owner.addComponentListener(ownerListener);
                    glassPane.addMouseListener(ownerListener);
                    glassPane.setVisible(true);
                }
            }
        });
    }
    
    /**
     * Installs input actions in content pane.
     */
    private void installInputActions() {
        Container container = getContentPane();
        if (container instanceof JComponent) {
            JComponent contentPane = (JComponent) container;
            // Create Escape key binding to close window.
            contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_ACTION_KEY);
            contentPane.getActionMap().put(CLOSE_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
        }
    }
    
    /**
     * Uninstalls input actions in content pane.
     */
    private void uninstallInputActions() {
        Container container = getContentPane();
        if (container instanceof JComponent) {
            JComponent contentPane = (JComponent) container;
            contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
            contentPane.getActionMap().remove(CLOSE_ACTION_KEY);
        }
    }
    
    /**
     * Creates a popup window with the specified parent component, content
     * pane and screen location.
     */
    public static PopupWindow createPopupWindow(JComponent parent, 
            JComponent contentPane, Point location) {
        // Declare new popup window.
        PopupWindow popupWindow;

        // Create popup window for parent container.
        Container ancestor = parent.getTopLevelAncestor();
        if (ancestor instanceof Frame) {
            popupWindow = new PopupWindow((Frame) ancestor);
        } else if (ancestor instanceof Window) {
            popupWindow = new PopupWindow((Window) ancestor);
        } else {
            popupWindow = new PopupWindow();
        }

        // Calculate window size and location.
        popupWindow.setContentPane(contentPane);
        popupWindow.pack();
        popupWindow.setLocation(location);

        // Return the window.
        return popupWindow;
    }
    
    /**
     * Sets an indicator to animate the popup window.  The built-in animation
     * opens and closes the popup like a window shade.
     */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }
    
    /**
     * Overrides superclass method to animate the display.
     */
    @Override
    public void setVisible(boolean visible) {
        if (animated) {
            startAnimation(visible);
        } else {
            super.setVisible(visible);
            // Always call dispose() when hidden to ensure windowClosed event
            // is fired.  The event is used to remove the owner listener.
            if (!visible) {
                dispose();
            }
        }
    }
    
    /**
     * Starts an animation to show or hide the popup window.
     */
    private void startAnimation(boolean visible) {
        if (isVisible() != visible) {
            // Stop existing animator.
            stopAnimator();
            
            // Save preferred size.
            Dimension initialSize = visible ? getPreferredSize() : getSize();
            
            // Make visible if requested with zero height.  The animation will
            // change the height to make the window grow.
            if (!isVisible() && visible) {
                setSize(new Dimension(initialSize.width, 0));
                super.setVisible(true);
            }
            
            // Create new animator and start.
            animator = new Animator(DURATION, new AnimationTarget(visible, initialSize));
            animator.setResolution(RESOLUTION);
            animator.start();
        }
    }
    
    /**
     * Stops the animation.
     */
    private void stopAnimator() {
        if (animator != null) {
            animator.stop();
            animator = null;
        }
    }
    
    /**
     * Listener to handle events on the popup owner.  The popup is closed
     * when the owner is moved, or when the mouse is pressed on the owner's
     * glass pane.
     */
    private class OwnerListener extends MouseAdapter implements ComponentListener {

        @Override
        public void componentHidden(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            dispose();
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            setVisible(false);
        }
    }
    
    /**
     * Animation target to handle timing events.
     */
    private class AnimationTarget extends TimingTargetAdapter {
        private final boolean makeVisible;
        private final Dimension initialSize;
        
        public AnimationTarget(boolean makeVisible, Dimension initialSize) {
            this.makeVisible = makeVisible;
            this.initialSize = initialSize;
        }
        
        @Override
        public void timingEvent(float fraction) {
            // Determine size percentage.
            float sizePct = makeVisible ? fraction : 1.0f - fraction;
            
            // Stop timer when we reach the end.
            if (makeVisible && sizePct > 0.98f) {
                stopAnimator();
                sizePct = 1.0f;
                
            } else if (!makeVisible && sizePct < 0.02f) {
                stopAnimator();
                sizePct = 0.0f;
                dispose();
            }
            
            // Set window size.
            setSize(new Dimension(initialSize.width,
                    (int) (initialSize.getHeight() * sizePct)));
            
            // Request repaint.
            repaint();
        }        
    }
}
