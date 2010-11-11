package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * This class is simply a JPanel that uses a BoxLayout with the orientation 
 * specified in the constructor.  The default constructor creates a panel 
 * oriented along the y axis.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class BoxPanel extends JPanel {
    
    /**
     * Constant for specifying that the underlying <tt>BoxLayout</tt> should
     * be oriented along the x axis.
     */
    public static final int X_AXIS = BoxLayout.X_AXIS;

    /**
     * Constant for specifying that the underlying <tt>BoxLayout</tt> should
     * be oriented along the y axis.
     */
    public static final int Y_AXIS = BoxLayout.Y_AXIS;

    public static final Dimension HORIZONTAL_COMPONENT_GAP = new Dimension(6, 0);
    
    public static final Dimension VERTICAL_COMPONENT_GAP = new Dimension(0, 6);
    
    public static final Dimension LINE_GAP = new Dimension(0, 10);
    
    /**
     * Creates a default <tt>BoxPanel</tt> with a <tt>BoxLayout</tt> oriented 
     * along the y axis.
     */
    public BoxPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    /**
     * Creates a <tt>BoxPanel</tt> with a <tt>BoxLayout</tt> that uses the 
     * specified orientation.
     *
     * @param orientation the orientation to use for the layout, which should
     *                    be either BoxPanel.X_AXIS or BoxPanel.Y_AXIS
     *
     * @throws IllegalArgumentException if the <tt>orientation</tt> is not 
     *         a valid <tt>BoxPanel</tt> orientation
     */
    public BoxPanel(int orientation) {
        if (orientation != X_AXIS && orientation != Y_AXIS)
            throw new IllegalArgumentException("Illegal BoxPanel orientation");
        setLayout(new BoxLayout(this, orientation));
    }

    /**
     * Sets the orientation that the panel uses for laying out components.
     *
     * @param orientation the orientation to use for the layout, which should
     *                    be either BoxPanel.X_AXIS or BoxPanel.Y_AXIS
     *
     * @throws IllegalArgumentException if the <tt>orientation</tt> is not 
     *         a valid <tt>BoxPanel</tt> orientation
     */
    public void setOrientation(int orientation) {
        if(orientation != X_AXIS && orientation != Y_AXIS)
            throw new IllegalArgumentException("Illegal BoxPanel orientation");
        setLayout(new BoxLayout(this, orientation));
    }

    public void addLineGap() {
        add(Box.createRigidArea(BoxPanel.LINE_GAP));
    }

    public void addHorizontalComponentGap() {
        add(Box.createRigidArea(BoxPanel.HORIZONTAL_COMPONENT_GAP));
    }

    public void addVerticalComponentGap() {
        add(Box.createRigidArea(BoxPanel.VERTICAL_COMPONENT_GAP));
    }

    public void addLeft(JComponent component) {
        component.setAlignmentX(Component.RIGHT_ALIGNMENT);
        super.add(component);
    }

    public void addCenter(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        super.add(component);
    }

    public void addRight(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        super.add(component);
    }

}
