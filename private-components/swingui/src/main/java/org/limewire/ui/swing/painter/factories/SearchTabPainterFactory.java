package org.limewire.ui.swing.painter.factories;

import java.awt.Color;
import java.awt.GradientPaint;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.SearchTabPainter;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;


/**
 * Factory for creating search tab painters in the lw default
 *  colour scheme. 
 */
public class SearchTabPainterFactory {
    @Resource private Color selectionTopBevelBackground;
    @Resource private Color selectionTopBevelBorder;
    @Resource private Color selectionBackgroundTopGradient;
    @Resource private Color selectionBackgroundBottomGradient;
    
    @Resource private Color highlightTopBevelBackground;
    @Resource private Color highlightTopBevelBorder;
    @Resource private Color highlightBackgroundTopGradient;
    @Resource private Color highlightBackgroundBottomGradient;
    
    @Resource private Color normalTopBevelBackground;
    @Resource private Color normalTopBevelBorder;
    @Resource private Color normalBackgroundTopGradient;
    @Resource private Color normalBackgroundBottomGradient;
    
    /**
     * Constructs a SearchTabPainterFactory.
     */
    @Inject
    public SearchTabPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    /**
     * Creates a Painter to render search tabs in the selected state.
     */
    public SearchTabPainter createSelectionPainter() {
        return new SearchTabPainter(selectionTopBevelBackground, 
                selectionTopBevelBorder,
                new GradientPaint(0, 0, selectionBackgroundTopGradient, 
                        0, 1, selectionBackgroundBottomGradient));
    }
    
    /**
     * Creates a Painter to render search tabs in the highlighted (hover) state.
     */
    public SearchTabPainter createHighlightPainter() {
        return new SearchTabPainter(highlightTopBevelBackground, 
                highlightTopBevelBorder,
                new GradientPaint(0, 0, highlightBackgroundTopGradient, 
                        0, 1, highlightBackgroundBottomGradient), true);
    }
    
    /**
     * Creates a Painter to render search tabs in the normal state.
     */
    public SearchTabPainter createNormalPainter() {
        return new SearchTabPainter(normalTopBevelBackground,
                normalTopBevelBorder,
                new GradientPaint(0, 0, normalBackgroundTopGradient, 
                        0, 1, normalBackgroundBottomGradient), true);
    }
}
