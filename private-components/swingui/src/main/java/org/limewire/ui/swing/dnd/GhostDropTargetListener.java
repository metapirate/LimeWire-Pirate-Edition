package org.limewire.ui.swing.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Listens to drag and drop events. When files are dragged onto a
 * component implementing this listener, a semi-transparent image
 * will be appear next to mouse to give better feedback as to what
 * action the drop will result in.
 * <p>
 * This class is responsible for loading the glass pane, making it
 * visible, displaying the transparent drag, and hiding the glass
 * pane when the drag exits or completes.
 */
public class GhostDropTargetListener implements DropTargetListener {

    private final GhostDragGlassPane ghostDragGlassPane;
    private final JComponent parent;
    private Point offset = new Point(8,15);
    
    public GhostDropTargetListener(JComponent parent, GhostDragGlassPane ghostDragGlassPane) {
        this.parent = parent;
        this.ghostDragGlassPane = ghostDragGlassPane;
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        Component component = getGlassPane();

        // something is already currently occupying the glass pane and its visible
        if(!(component instanceof GhostDragGlassPane) && component.isVisible()) 
            return;
        
        // the ghost glass pane is not occupying the glass pane
        if(!(component instanceof GhostDragGlassPane)) {
            SwingUtilities.getRootPane(parent).setGlassPane(ghostDragGlassPane);
        } 
        updateText(dtde, ghostDragGlassPane);
        ghostDragGlassPane.repaint();
    }
    
    /**
     * Converts the mouse coordinates on the component, to mouse coordinates
     * on the glass pane, positions the glass pane, then updates the image.
     */
    private void updateText(DropTargetDragEvent dtde, GhostDragGlassPane ghostPane) {
        Point p = (Point) dtde.getLocation().clone();

        SwingUtilities.convertPointToScreen(p, parent);
        SwingUtilities.convertPointFromScreen(p, ghostPane); 

        ghostPane.setPoint(new Point(p.x + offset.x, p.y + offset.y));
    }

    /**
     * When the drag exits, hide the glass pane.
     */
    @Override
    public void dragExit(DropTargetEvent dte) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();
        glassPane.setVisible(false);
    }

    /**
     * As a drag occurs over this component, update the image's position
     * as the mouse moves.
     */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();

        Point p = (Point) dtde.getLocation().clone();
        SwingUtilities.convertPointToScreen(p, parent);
        SwingUtilities.convertPointFromScreen(p, glassPane); 
        glassPane.setPoint(new Point(p.x + offset.x, p.y + offset.y));

        glassPane.repaint(glassPane.getRepaintRect());
    }

    /**
     * When a drop occurs, hide the glass pane.
     */
    @Override
    public void drop(DropTargetDropEvent dtde) {
        if(!(getGlassPane() instanceof GhostDragGlassPane))
            return;
        GhostDragGlassPane glassPane = (GhostDragGlassPane) getGlassPane();
        glassPane.setVisible(false);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}
    
    private Component getGlassPane() {
        return SwingUtilities.getRootPane(parent).getGlassPane();
    }
}
