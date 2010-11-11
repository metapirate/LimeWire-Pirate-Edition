package org.limewire.ui.swing.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JSlider;

import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * An extension of JSlider to accept a foreground and background painter.
 */
public class LimeSliderBar extends JSlider implements MouseListener {
    private AbstractPainter<JComponent> backgroundPainter;
    private AbstractPainter<JSlider> foregroundPainter;
    
    
    /**
     * Creates a default unskinned instance of this component. 
     */
    public LimeSliderBar() {
        addMouseListener(this);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundPainter != null && foregroundPainter != null) {
            backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
            foregroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }
        else {
            super.paintComponent(g);
        }
    }

    /**
     * Sets a painter for painting the progress portion.  This painter
     *  should also include the drag nob if it is desired.
     * <p>
     * Both background and foreground painter must be set to have an effect.
     */
    public void setForegroundPainter(AbstractPainter<JSlider> painter) {
        this.foregroundPainter = painter;
    }
    
    /**
     * Sets the painter that will be used to draw the components background
     *  and border.
     * <p>
     * Both background and foreground painter must be set to have an effect.
     */
    public void setBackgroundPainter(AbstractPainter<JComponent> painter) {
        this.backgroundPainter = painter;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        // Used to possibly repaint the drag nob
        //  if it is available
        repaint();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        // Used to possibly repaint the drag nob
        //  if it is available
        repaint();
    }
    @Override
    public void mousePressed(MouseEvent e) {
    }
    @Override
    public void mouseReleased(MouseEvent e) {
    }
}
