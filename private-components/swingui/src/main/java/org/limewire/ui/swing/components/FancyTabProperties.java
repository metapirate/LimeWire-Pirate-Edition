package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.I18n;

public class FancyTabProperties implements Cloneable {
    
    private Painter<?> highlightPainter;
    private Painter<?> normalPainter;
    private Painter<?> selectedPainter;
    private Color selectionColor;
    private Color normalColor;
    private Font textFont;
    private boolean removable;
    private String closeOneText;
    private String closeAllText;
    private String closeOtherText;
    private boolean underlineEnabled;
    private Insets insets;

    FancyTabProperties() {
        highlightPainter = new RectanglePainter<JXButton>(2, 2, 2, 2, 5, 5, true, Color.YELLOW, 0f, Color.LIGHT_GRAY);
        selectedPainter = new RectanglePainter<JXButton>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY);
        normalPainter = null;
        selectionColor = new Color(0, 100, 0);
        normalColor = Color.BLUE;
        removable = false;
        closeOneText = I18n.tr("Close Tab");
        closeAllText = I18n.tr("Close All Tabs");
        closeOtherText = I18n.tr("Close Other Tabs");
        underlineEnabled = true;
        insets = null;
    }
    
    @Override
    public FancyTabProperties clone() {
        try {
            return (FancyTabProperties)super.clone();
        } catch(CloneNotSupportedException cnse) {
            throw new Error(cnse);
        }
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public Painter<?> getHighlightPainter() {
        return highlightPainter;
    }

    public void setHighlightPainter(Painter<?> highlightPainter) {
        this.highlightPainter = highlightPainter;
    }

    public Painter<?> getNormalPainter() {
        return normalPainter;
    }

    public void setNormalPainter(Painter<?> normalPainter) {
        this.normalPainter = normalPainter;
    }

    public Painter<?> getSelectedPainter() {
        return selectedPainter;
    }

    public void setSelectedPainter(Painter<?> selectedPainter) {
        this.selectedPainter = selectedPainter;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public void setSelectionColor(Color selectionColor) {
        this.selectionColor = selectionColor;
    }

    public Color getNormalColor() {
        return normalColor;
    }

    public void setNormalColor(Color normalColor) {
        this.normalColor = normalColor;
    }

    public Font getTextFont() {
        return textFont;
    }

    public void setTextFont(Font textFont) {
        this.textFont = textFont;
    }

    public String getCloseOneText() {
        return closeOneText;
    }

    public String getCloseAllText() {
        return closeAllText;
    }

    public String getCloseOtherText() {
        return closeOtherText;
    }
    
    public void setCloseOneText(String closeOneText) {
        this.closeOneText = closeOneText;
    }

    public void setCloseAllText(String closeAllText) {
        this.closeAllText = closeAllText;
    }

    public void setCloseOtherText(String closeOtherText) {
        this.closeOtherText = closeOtherText;
    }

    public boolean isUnderlineEnabled() {
        return underlineEnabled;
    }

    public void setUnderlineEnabled(boolean underlineEnabled) {
        this.underlineEnabled = underlineEnabled;
    }
    
    public void setInsets(Insets insets) {
        this.insets = insets;
    }
    
    public Insets getInsets() {
        return this.insets;
    }
}
