package org.limewire.ui.swing.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * UI delegate for the volume slider control.
 */
class VolumeSliderUI extends BasicSliderUI {

    @Resource private int preferredWidth;
    @Resource private int preferredHeight;
    @Resource private Icon thumbIcon;
    @Resource private Icon trackIcon;
    
    /** Thumb image for vertical orientation. */
    private BufferedImage verticalThumbImage;
    /** Track image for vertical orientation. */
    private BufferedImage verticalTrackImage;
    /** Thumb image for horizontal orientation. */
    private BufferedImage horizontalThumbImage;
    /** Track image for horizontal orientation. */
    private BufferedImage horizontalTrackImage;

    /**
     * Constructs a VolumeSliderUI for the specified slider component.
     */
    public VolumeSliderUI(JSlider b) {
        super(b);
        GuiUtils.assignResources(this);
        initResources();
    }
    
    /**
     * Initializes UI resources.
     */
    private void initResources() {
        // Convert thumb icon to images if possible.  The original icon must be
        // drawn for vertical orientation.
        if (thumbIcon instanceof ImageIcon) {
            verticalThumbImage = new BufferedImage(thumbIcon.getIconWidth(), 
                thumbIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            
            Graphics2D g2d = verticalThumbImage.createGraphics();
            g2d.drawImage(((ImageIcon) thumbIcon).getImage(), 0, 0, null);
            g2d.dispose();
            
            // Create rotated image for horizontal orientation.
            horizontalThumbImage = new BufferedImage(thumbIcon.getIconHeight(), 
                    thumbIcon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
                   
            g2d = horizontalThumbImage.createGraphics();
            g2d.translate(0, thumbIcon.getIconWidth());
            g2d.rotate(-Math.PI / 2);
            g2d.drawImage(((ImageIcon) thumbIcon).getImage(), 0, 0, null);
            g2d.dispose();
        }
        
        // Convert track icon to images if possible.  The original icon must be
        // drawn for vertical orientation.
        if (trackIcon instanceof ImageIcon) {
            verticalTrackImage = new BufferedImage(trackIcon.getIconWidth(), 
                trackIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            
            Graphics2D g2d = verticalTrackImage.createGraphics();
            g2d.drawImage(((ImageIcon) trackIcon).getImage(), 0, 0, null);
            g2d.dispose();
            
            // Create rotated image for horizontal orientation.
            horizontalTrackImage = new BufferedImage(trackIcon.getIconHeight(), 
                    trackIcon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
                   
            g2d = horizontalTrackImage.createGraphics();
            g2d.translate(0, trackIcon.getIconWidth());
            g2d.rotate(-Math.PI / 2);
            g2d.drawImage(((ImageIcon) trackIcon).getImage(), 0, 0, null);
            g2d.dispose();
        }
    }
    
    /**
     * Returns the preferred size for horizontal orientation based on the image
     * sizes.
     */
    @Override
    public Dimension getPreferredHorizontalSize() {
        if ((horizontalThumbImage != null) && (horizontalTrackImage != null)) {
            return new Dimension(Math.max(horizontalTrackImage.getWidth(), preferredHeight),
                    Math.max(horizontalThumbImage.getHeight(), preferredWidth)); 
        } else {
            return new Dimension(preferredHeight, preferredWidth);
        }
    }

    /**
     * Returns the preferred size for vertical orientation based on the image
     * sizes.
     */
    @Override
    public Dimension getPreferredVerticalSize() {
        if ((verticalThumbImage != null) && (verticalTrackImage != null)) {
            return new Dimension(Math.max(verticalThumbImage.getWidth(), preferredWidth),
                    Math.max(verticalTrackImage.getHeight(), preferredHeight)); 
        } else {
            return new Dimension(preferredWidth, preferredHeight);
        }
    }
    
    /**
     * Returns the minimum size of the slider component.
     */
    @Override
    public Dimension getMinimumSize(JComponent c) {
        // Calculate minimum size.
        Dimension minimumSize = super.getMinimumSize(c);
        
        if (slider.getOrientation() == JSlider.VERTICAL) {
            // For vertical slider, size must be as wide as the thumb image, 
            // and as tall as the track image.
            if (verticalThumbImage != null) {
                minimumSize.width = Math.max(verticalThumbImage.getWidth(), minimumSize.width);
            }
            if (verticalTrackImage != null) {
                minimumSize.height = Math.max(verticalTrackImage.getHeight(), minimumSize.height);
            }
            
        } else {
            // For horizontal slider, size must be as tall as the thumb image, 
            // and as wide as the track image.
            if (horizontalThumbImage != null) {
                minimumSize.height = Math.max(horizontalThumbImage.getHeight(), minimumSize.height);
            }
            if (horizontalTrackImage != null) {
                minimumSize.width = Math.max(horizontalTrackImage.getWidth(), minimumSize.width);
            }
        }
        
        return minimumSize;
    }
    
    /**
     * Returns the preferred size of the slider component.
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        // Calculate preferred size.
        Dimension preferredSize = super.getPreferredSize(c);
        
        // Adjust calculated sizes.  The superclass method recalculates 
        // vertical width or horizontal height, and we want to ensure these are
        // at least as big as the default preferred sizes. 
        if (slider.getOrientation() == JSlider.VERTICAL) {
            preferredSize.width = Math.max(preferredWidth, preferredSize.width);
        } else {
            preferredSize.height = Math.max(preferredWidth, preferredSize.height);
        }
        
        return preferredSize;
    }
    
    /**
     * Calculates the track rectangle.  The track rectangle defines the sliding
     * range for the thumb.  This is basically the content rectangle indented 
     * on the ends by the "track buffer", which is usually half of the thumb
     * size.
     */
    @Override
    protected void calculateTrackRect() {
        if (verticalTrackImage == null) {
            super.calculateTrackRect();
            return;
        }
        
        // used to center sliders added using BorderLayout.CENTER (bug 4275631)
        int centerSpacing = 0;
        
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            centerSpacing = thumbRect.height;
            if (slider.getPaintTicks()) centerSpacing += getTickLength();
            if (slider.getPaintLabels()) centerSpacing += getHeightOfTallestLabel();
            
            // Determine track position and size.  The track length is equal to
            // the track image length, minus half of the thumb image length. 
            trackRect.x = contentRect.x + (contentRect.width - 
                (horizontalTrackImage.getWidth() - horizontalThumbImage.getWidth() / 2)) / 2;
            trackRect.y = contentRect.y + (contentRect.height - centerSpacing) / 2;
            
            trackRect.width = horizontalTrackImage.getWidth() - horizontalThumbImage.getWidth() / 2;
            trackRect.height = thumbRect.height;
            
        } else {
            centerSpacing = thumbRect.width;
            if (slider.getComponentOrientation().isLeftToRight()) {
                if (slider.getPaintTicks()) centerSpacing += getTickLength();
                if (slider.getPaintLabels()) centerSpacing += getWidthOfWidestLabel();
            } else {
                if (slider.getPaintTicks()) centerSpacing -= getTickLength();
                if (slider.getPaintLabels()) centerSpacing -= getWidthOfWidestLabel();
            }
            
            // Determine track position and size.  The track length is equal to
            // the track image length, minus half of the thumb image length. 
            trackRect.x = contentRect.x + (contentRect.width - centerSpacing) / 2;
            trackRect.y = contentRect.y + (contentRect.height - 
                (verticalTrackImage.getHeight() - verticalThumbImage.getHeight() / 2)) / 2;
            
            trackRect.width = thumbRect.width;
            trackRect.height = verticalTrackImage.getHeight() - verticalThumbImage.getHeight() / 2;
        }
    }

    /**
     * Returns the size of a thumb.
     */
    @Override
    protected Dimension getThumbSize() {
        BufferedImage thumbImage = (slider.getOrientation() == JSlider.VERTICAL) ?
                verticalThumbImage : horizontalThumbImage;
        
        if (thumbImage != null) {
            return new Dimension(thumbImage.getWidth(), thumbImage.getHeight());
        } else {
            return super.getThumbSize();
        }
    }
    
    /**
     * Paints the focus highlight.  Overrides superclass method to do nothing.
     */
    @Override
    public void paintFocus(Graphics g) {
        // Do nothing.
    }
    
    /**
     * Paints the track.  This overrides the superclass method to use the track
     * image.
     */
    @Override
    public void paintTrack(Graphics g) {
        // Get track image.
        BufferedImage trackImage = (slider.getOrientation() == JSlider.VERTICAL) ? 
                verticalTrackImage : horizontalTrackImage;

        if (trackImage != null) {
            // Use content rectangle.
            Rectangle trackBounds = contentRect;

            // Determine position offset to center track image within content.
            int cx = (trackBounds.width - trackImage.getWidth()) / 2;
            int cy = (trackBounds.height - trackImage.getHeight()) / 2;

            // Create graphics copy.
            Graphics gTemp = g.create();

            // Draw track image.
            gTemp.translate(trackBounds.x + cx, trackBounds.y + cy);
            gTemp.drawImage(trackImage, 0, 0, null);
            gTemp.dispose();
        
        } else {
            super.paintTrack(g);
        }
    }
    
    /**
     * Paints the thumb.  This overrides the superclass method to use the thumb
     * image.
     */
    @Override
    public void paintThumb(Graphics g) {
        // Get thumb image.
        BufferedImage thumbImage = (slider.getOrientation() == JSlider.VERTICAL) ? 
                verticalThumbImage : horizontalThumbImage;
        
        if (thumbImage != null) {
            // Get thumb position.
            Rectangle thumbBounds = thumbRect;
            
            // Create graphics copy.
            Graphics gTemp = g.create();

            // Draw thumb image.
            gTemp.translate(thumbBounds.x, thumbBounds.y);
            gTemp.drawImage(thumbImage, 0, 0, null);
            gTemp.dispose();
            
        } else {
            super.paintThumb(g);
        }
    }
}
