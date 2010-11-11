package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;
import javax.swing.JButton;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink.
 */
public class HyperlinkButton extends JButton implements MouseListener {
    
    private final HyperlinkButtonResources r = new HyperlinkButtonResources();
    
    public HyperlinkButton() {
        initialize();
    }
    
    public HyperlinkButton(Action action) {
        super(action);
        initialize();
    }

    public HyperlinkButton(String text) {
        initialize();
        setText(text);
    }
    
    public HyperlinkButton(String text, Action action) {
        super(action);
        setHideActionText(true);
        setText(text);
        initialize();
    }
    
    private void initialize() {
        this.setUI(new CustomHyperlinkUI());
        addMouseListener(this);
        FontUtils.underline(this);
    }
    
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if(!FontUtils.isUnderlined(this)) {
            FontUtils.underline(this);
        }
    }
    
    public void removeUnderline() {
        Font font = getFont();
        if (font != null) {
            super.setFont(FontUtils.deriveUnderline(font, false));
        }
    }
    
    /**
     * Shared method to update foreground according to mouse and enabled status.
     */
    private void updateForeground() {
        if (isEnabled()) {
            if (getModel().isRollover()) {
                super.setForeground(r.rolloverForeground);
            } else {
                super.setForeground(r.foreground);
            }
        } else {
            super.setForeground(r.disabledForeground);
        }
    }
    
     // Separated setting the foreground temporarily and setting the foreground 
     //  permanently to avoid limewire l&f violations
     //
    public void setNormalForeground(Color color) {
        r.foreground = color;
        updateForeground();
    }
    
    public void setRolloverForeground(Color color) {
        r.rolloverForeground = color;
        updateForeground();
    }
    
    public void setDisabledForeground(Color color) {
        r.disabledForeground = color;
        updateForeground();
    }
    
    // Uncomment to expose lw look and feel violations 
    // @Override
    // public void setForeground(Color c) {
    //     new IllegalArgumentException().printStackTrace();
    // }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if(r != null) {
            updateForeground();
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        updateForeground();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        updateForeground();
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    
    private class HyperlinkButtonResources {
        @Resource Color rolloverForeground;
        @Resource Color foreground;
        @Resource Color disabledForeground;
        
        public HyperlinkButtonResources() {
            GuiUtils.assignResources(this);
            HyperlinkButton.super.setForeground(foreground);
        }
    }
    
}