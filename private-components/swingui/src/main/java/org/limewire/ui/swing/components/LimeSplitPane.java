package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.limewire.util.OSUtils;

/**
 * SplitPane where the divider component can be specified.
 *
 */
public class LimeSplitPane extends JSplitPane{

    private boolean currentlyDraggable = true;
    
    public LimeSplitPane(int orientation, boolean continuousLayout, Component leftComponent, Component rightComponent, JComponent dividerComponent){
        super(orientation, continuousLayout, leftComponent, rightComponent);
        
        BasicSplitPaneUI splitUI = new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new CustomDivider(this);
            }
        };        
        setUI(splitUI);

        BasicSplitPaneDivider divider = splitUI.getDivider(); 
        divider.setBorder(BorderFactory.createEmptyBorder());
        divider.setLayout(new BorderLayout());
        divider.removeAll();
        divider.add(dividerComponent);
        
        setDividerSize(dividerComponent.getPreferredSize().height);
        
        // The upper panel was flickering on OS X due to some strange interactions with the browser.
        // As a temporary solution to the flickering problem, we're turned off automatic repainting
        // until we can solve the underlying problem.
        if (OSUtils.isMacOSX()) {
            setContinuousLayout(false);
        }
    }
    
    /**
     * Sets up a certain component as a drag controller for
     *  the position of the divider.
     */
    public void setDragComponent(JComponent component) {
        assertUISafeForDragabilityChanges();
        
        CustomDivider divider = (CustomDivider) ((BasicSplitPaneUI)getUI()).getDivider();
        
        CustomDivider.ReroutedMouseHandler reroutedMouseHandler
            = divider.new ReroutedMouseHandler(component);
        
        component.addMouseListener(reroutedMouseHandler);
        component.addMouseMotionListener(reroutedMouseHandler);
    }
    
    /**
     * Enables or disables divider position changes by drags on the main
     *  divider component.  This will not apply to secondary drag components
     *  set up using {@link #setDragComponent} 
     */
    public void setDividerDraggable(boolean draggable) {
        assertUISafeForDragabilityChanges();
            
        CustomDivider divider = (CustomDivider) ((BasicSplitPaneUI)getUI()).getDivider();
        
        if (!draggable) {
            if (currentlyDraggable) {
                currentlyDraggable = false;
                divider.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            
                divider.removeMouseListener(divider.getMouseHandler());
                divider.removeMouseMotionListener(divider.getMouseHandler());
                removeMouseListener(divider.getMouseHandler());
                removeMouseMotionListener(divider.getMouseHandler());
            }
        } 
        else {
            if (!currentlyDraggable) {
                currentlyDraggable = true;
                divider.setCursor((orientation == JSplitPane.HORIZONTAL_SPLIT) ?
                    Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR) :
                    Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            
                divider.addMouseListener(divider.getMouseHandler());
                divider.addMouseMotionListener(divider.getMouseHandler());
                addMouseListener(divider.getMouseHandler());
                addMouseMotionListener(divider.getMouseHandler());
            }
        }
    }

    
    /**
     * Used to Ensure (soft assertion) that the modified ui elements are still
     *  installed before attempting to make changes.
     */
    private void assertUISafeForDragabilityChanges() {
        if (!(getUI() instanceof BasicSplitPaneUI)) {
            throw new IllegalStateException("Can't change the divider draggability if the UI has been modified");
        }
        
        if (!(((BasicSplitPaneUI)getUI()).getDivider() instanceof CustomDivider)) {
            throw new IllegalStateException("Can't change the divider draggability if the divider has been modified");
        }
    }
    
    /**
     * A wrapper class that allows access to protected fields and methods
     *  in order to extend the drag functionality.
     */
    private static class CustomDivider extends BasicSplitPaneDivider {

        public CustomDivider(BasicSplitPaneUI ui) {
            super(ui);
        }
    
        /**
         * Peeks the enternal mouseHandler element used to control dragging
         *  on the primary drag source.  Can be used to detach it if necessary.
         */
        public MouseHandler getMouseHandler() {
            return mouseHandler;
        }
    
        /**
         * An adapter to use the normal drag mechanism with a nub rather than the
         *  entire slider component.
         *  
         * <p> Done by rerouting the message source to its parent component
         *      if it matches the drag component.  This in essence relaxes
         *      the drag preconditions so it can be initiated by a specific
         *      component.  Without this modification the superclass requires
         *      the event source to be the actual divider for anything to happen.  
         */
        public class ReroutedMouseHandler extends MouseHandler {
            
            private final JComponent rerouteComponent;
            
            public ReroutedMouseHandler(JComponent rerouteComponent) {
                this.rerouteComponent = rerouteComponent;
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(rerouteEvent(e));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(rerouteEvent(e));
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(rerouteEvent(e));
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(rerouteEvent(e));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(rerouteEvent(e));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(rerouteEvent(e));
            }
            
            private MouseEvent rerouteEvent(MouseEvent e) {
                if (e.getSource() == rerouteComponent) {
                    return new MouseEvent(CustomDivider.this, e.getID(), 
                        e.getWhen(), e.getModifiers(), e.getX(), 
                        e.getY(), e.getClickCount(),
                        e.isPopupTrigger());
                }
                else {
                    return e;
                }
            }
        }
    }
    
    
}
