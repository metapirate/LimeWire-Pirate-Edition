package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * A component that draws a line.
 */
public class Line extends JComponent {
    
    public static Line createHorizontalLine(Color color, int thickness) {
        return new Line(true, color, thickness);
    }

    public static Line createHorizontalLine(Color color) {
        return new Line(true, color, 1);
    }
    
    public static Line createHorizontalLine() {
        return new Line(true, Color.BLACK, 1);
    }
    
    public static Line createVerticalLine(Color color, int thickness) {
        return new Line(false, color, thickness);
    }
    
    public static Line createVerticalLine(Color color) {
        return new Line(false, color, 1);
    }
    
    public static Line createVerticalLine() {
        return new Line(false, Color.BLACK, 1);
    }

    private boolean horizontal;
    private Color color;

    private Line(boolean horizontal, Color color, int thickness) {
        if(color == null)
            throw new IllegalArgumentException("color must not be null");
        
        this.horizontal = horizontal;
        this.color = color;
        initSize(horizontal, thickness);
    }
       
    private void initSize(boolean horizontal, int thickness) {
        if(horizontal) {
            setPreferredSize(new Dimension(1, thickness));
            setMinimumSize(new Dimension(0, thickness));
            setMaximumSize(new Dimension(Short.MAX_VALUE, thickness));
        } else {
            setPreferredSize(new Dimension(thickness, 1));
            setMinimumSize(new Dimension(thickness, 0));
            setMaximumSize(new Dimension(thickness, Short.MAX_VALUE));       
        }
	}
    
    public void setThickness(int thickness) {
        initSize(horizontal, thickness);
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
 
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color oldColor = g.getColor();
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(oldColor);
    }
    
}