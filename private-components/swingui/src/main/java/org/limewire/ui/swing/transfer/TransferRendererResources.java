package org.limewire.ui.swing.transfer;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Container for resources used by the Download and Upload table renderers.
 */
public class TransferRendererResources {

    @Resource private Font font;
    @Resource private Color foreground;
    @Resource private Color disabledForeground;

    @Resource private int progressBarHeight;
    @Resource private int progressBarWidth;
    /**the progress bar disappears when the column width is less than this value*/
    @Resource private int progressBarCutoffWidth;
    @Resource private Color progressBarBorder;
    
    @Resource(key="DownloadCancelRendererEditor.cancelIcon") private Icon cancelIcon;
    @Resource(key="DownloadCancelRendererEditor.cancelIconPressed") private Icon cancelIconPressed;
    @Resource(key="DownloadCancelRendererEditor.cancelIconRollover") private Icon cancelIconRollover;

    /**
     * Constructs a TransferRendererResources object.
     */
    public TransferRendererResources() {
        GuiUtils.assignResources(this);
    }
    
    /**
     * Sets the icons for the specified cancel button.
     */
    public void decorateCancelButton(JButton button) {
        button.setIcon(cancelIcon);
        button.setPressedIcon(cancelIconPressed);
        button.setRolloverIcon(cancelIconRollover);
    }
    
    /**
     * Sets the font and foreground color for the specified component.
     */
    public void decorateComponent(JComponent component) {
        component.setFont(font);
        component.setForeground(foreground);
    }
    
    public Font getFont() {
        return font;
    }
    
    public Color getForeground() {
        return foreground;
    }
    
    public Color getDisabledForeground() {
        return disabledForeground;
    }
    
    public int getProgressBarHeight() {
        return progressBarHeight;
    }
    
    public int getProgressBarWidth() {
        return progressBarWidth;
    }
    
    public int getProgressBarCutoffWidth() {
        return progressBarCutoffWidth;
    }
    
    public Color getProgressBarBorderColor() {
        return progressBarBorder;
    }
}
