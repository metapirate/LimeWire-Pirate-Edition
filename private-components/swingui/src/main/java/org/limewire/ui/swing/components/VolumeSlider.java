package org.limewire.ui.swing.components;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.KeyStroke;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A volume control based on JSlider.  VolumeSlider installs a custom UI 
 * delegate to render a slider with a stylized appearance.  The default
 * orientation is vertical.
 */
public class VolumeSlider extends JSlider {

    @Resource private Color background;
    @Resource private Color borderForeground;
    
    /**
     * Constructs a VolumeSlider with default minimum and maximum values of 0
     * and 100.
     */
    public VolumeSlider() {
        this(0, 100);
    }

    /**
     * Constructs a VolumeSlider with the specified minimum and maximum values.
     */
    public VolumeSlider(int min, int max) {
        super(min, max);
        GuiUtils.assignResources(this);
        initSlider();
    }
    
    /**
     * Initializes the slider by setting default properties.
     */
    private void initSlider() {
        setBackground(background);
        setOrientation(VERTICAL);
        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("HOME"), "maxScroll");
        inputMap.put(KeyStroke.getKeyStroke("END"), "minScroll");
    }

    /**
     * Overrides superclass method to install a custom UI delegate.
     */
    @Override
    public void updateUI() {
        setUI(new VolumeSliderUI(this));
        // Update UI for slider labels.  This must be called after updating the
        // UI of the slider.  Refer to JSlider.updateUI().
        updateLabelUIs();
    }
    
    /**
     * Creates a new popup window containing this slider.
     */
    public JPopupMenu createPopup() {
        // Create popup.
        JPopupMenu popup = new JPopupMenu() {
            @Override
            public void requestFocus() {
                VolumeSlider.this.requestFocus();
            }
        };
        
        // Set attributes and add slider.
        popup.setBorder(BorderFactory.createLineBorder(borderForeground));
        popup.setLayout(new BorderLayout());
        popup.add(this, BorderLayout.CENTER);
        
        return popup;
    }
}
