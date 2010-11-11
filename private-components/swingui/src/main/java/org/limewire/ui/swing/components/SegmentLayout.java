package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.LinkedList;
import java.util.List;

/**
 * A layout that vertically stacks the components and enforces 
 *  that they are given an equal amount of space. 
 *  
 * <p> This exists because miglayout cannot be trusted.
 */
public class SegmentLayout implements LayoutManager2 {
    
    private final List<Component> components = new LinkedList<Component>();
    
    @Override
    public void addLayoutComponent(String name, Component comp) {
        components.add(comp);
    }
    @Override
    public void layoutContainer(Container parent) {

        int height = parent.getHeight();
        int width = parent.getWidth();
        int cellHeight = height;
        if (components.size() != 0) {
            cellHeight /= components.size();
        }
        
        int cellStart = 0;
        for ( Component c : components ) {
            c.setBounds(0, cellStart, width, cellHeight);
            cellStart += cellHeight;
        }
        
    }
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return parent.getSize();
    }
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return parent.getSize();
    }
    @Override
    public void removeLayoutComponent(Component comp) {
        components.remove(comp);
    }
    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        components.add(comp);
    }
    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0;
    }
    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0;
    }
    @Override
    public void invalidateLayout(Container target) {
    }
    @Override
    public Dimension maximumLayoutSize(Container target) {
        return target.getMaximumSize();
    }
}
