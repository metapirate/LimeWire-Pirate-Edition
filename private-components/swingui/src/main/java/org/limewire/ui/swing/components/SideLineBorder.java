package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

/** A border that draws a line on one side. */
public class SideLineBorder extends AbstractBorder {

    public static enum Side {
        LEFT, RIGHT, TOP, BOTTOM
    };

    private final Side side;

    private final int thickness;

    private final Color color;

    public SideLineBorder(Color color, Side side) {
        this(color, 1, side);
    }

    public SideLineBorder(Color color, int thickness, Side side) {
        this.color = color;
        this.thickness = thickness;
        this.side = side;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();

        g.setColor(color);
        switch(side) {
        case TOP:
            g.fillRect(x, y, width - 1, thickness);
            break;
        case LEFT:
            g.fillRect(x, y, thickness, height);
            break;
        case BOTTOM:
            g.fillRect(x, y + height - 1 - thickness, width - 1, thickness);
            break;
        case RIGHT:
            g.fillRect(x + width - thickness, y, thickness, height);
            break;
        }
        g.setColor(oldColor);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = 0;
        switch(side) {
        case BOTTOM:
            insets.bottom = thickness;
            break;
        case LEFT:
            insets.left = thickness;
            break;
        case RIGHT:
            insets.right = thickness;
            break;
        case TOP:
            insets.top = thickness;
            break;
        }
        return insets;
    }

    public Color getLineColor() {
        return color;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

}
