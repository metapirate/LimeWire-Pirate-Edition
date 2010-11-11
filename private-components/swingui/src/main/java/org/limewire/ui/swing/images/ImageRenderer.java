package org.limewire.ui.swing.images;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;


import com.google.inject.Inject;

/**
 * Renderers a thumbnail based on the specs of ThumbnailManager.WIDTH and
 * ThumbnailManager.HEIGHT.
 */
class ImageRenderer extends JComponent {

    private final JLabel label;
    private Icon icon;
    
    @Inject
    public ImageRenderer() {
        setPreferredSize(new Dimension(ThumbnailManager.WIDTH, ThumbnailManager.HEIGHT));
        setSize(getPreferredSize());
        
        setLayout(null);
        setOpaque(false);
        
        label = new JLabel();
        label.setVisible(false);
        label.setOpaque(false);
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        calculateLabelDimnensions();
        add(label);
    }
    
    private void calculateLabelDimnensions() {
        int x = (getWidth() - getInsets().left - getInsets().right - ThumbnailManager.WIDTH)/2 + getInsets().left;
        int y = getInsets().top + ThumbnailManager.HEIGHT/2 + 15;
        label.setBounds(x, y, ThumbnailManager.WIDTH, 26);
    }
    
    public void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    public void setText(String text) {
        label.setText(text);
        if(text == null || text.length() == 0)
            label.setVisible(false);
        else
            label.setVisible(true);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if(isVisible()&& icon != null) {
            int iconX = (getWidth() - icon.getIconWidth())/2;
            int iconY = (ThumbnailManager.HEIGHT - icon.getIconHeight())/2;
         
            icon.paintIcon(this, g, 0 + iconX, 0 + iconY);
        }
    }
}
