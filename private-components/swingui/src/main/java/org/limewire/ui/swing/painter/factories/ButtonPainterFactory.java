package org.limewire.ui.swing.painter.factories;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter;
import org.limewire.ui.swing.painter.ButtonForegroundPainter;
import org.limewire.ui.swing.painter.DarkButtonBackgroundPainter;
import org.limewire.ui.swing.painter.FlatButtonBackgroundPainter;
import org.limewire.ui.swing.painter.GreenButtonBackgroundPainter;
import org.limewire.ui.swing.painter.LightButtonBackgroundPainter;
import org.limewire.ui.swing.painter.PopupButtonBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.ButtonBackgroundPainter.DrawMode;
import org.limewire.ui.swing.painter.ButtonForegroundPainter.FontTransform;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * Creates LW skinned foreground and background painters for JXButtons.
 *  Foreground painters manage the painting of the button text and icon,
 *  background painters draw the actual button, any borders, and 
 *  shadowing/accents.
 */
public class ButtonPainterFactory {
    
    @Resource private Color miniHoverTextForeground;
    @Resource private Color miniDownTextForeground;
    
    @Resource private Color darkFullHoverTextForeground;
    @Resource private Color darkFullDownTextForeground;
    @Resource private Color darkFullDisabledTextForeground;
    
    @Resource private int miniArcWidth;
    @Resource private int miniArcHeight;
    @Resource private Color miniBackgroundPressed;
    @Resource private Color miniBackgroundRollover;
    
    @Resource private int linkArcWidth;
    @Resource private int linkArcHeight;
    @Resource private Color linkBackgroundPressed;
    @Resource private Color linkBackgroundRollover;
    @Resource private Color linkDownTextForeground;
    
    @Inject
    ButtonPainterFactory() {
        GuiUtils.assignResources(this);
    }
    
    /**
     * Creates the foreground painter for pop up button style used
     *  in mini combo boxes. 
     */
    public ButtonForegroundPainter createMiniButtonForegroundPainter() {
        return new ButtonForegroundPainter(miniHoverTextForeground, miniDownTextForeground, Color.GRAY);
    }
    
    /**
     * Creates the foreground painter for pop up button style used
     *  in the link like combo boxes. 
     */
    public ButtonForegroundPainter createLinkButtonForegroundPainter() {
        return new ButtonForegroundPainter(null, linkDownTextForeground, null,
                FontTransform.NO_CHANGE, FontTransform.REMOVE_UNDERLINE, FontTransform.NO_CHANGE);
    }
    
    /**
     * Creates light styled foreground painter. 
     */
    public ButtonForegroundPainter createLightFullButtonForegroundPainter() {
        return new ButtonForegroundPainter();
    }
    
    /**
     * Creates the painter used to draw the text and icon on the dark styled button
     * across different states.
     */
    public ButtonForegroundPainter createDarkFullButtonForegroundPainter() {
        return new ButtonForegroundPainter(darkFullHoverTextForeground, darkFullDownTextForeground, 
                darkFullDisabledTextForeground);
    }

    /**
     * Creates the default background painter for the buttons used for mini combo
     *  boxes.  At this time these buttons have no background painted until a
     *  mouse over event.  When the mouse is over they pop out to indicate an 
     *  action can be made.
     */
    public PopupButtonBackgroundPainter createMiniButtonBackgroundPainter() {
        return new PopupButtonBackgroundPainter(miniBackgroundPressed,
                miniBackgroundRollover, miniArcWidth, miniArcHeight);
    }

    /**
     * Creates the default background painter for the buttons used for link
     *  like combo boxes. At this time these buttons have no background painted until a
     *  mouse over event.  When the mouse is over they pop out to indicate an 
     *  action can be made.
     */
    public PopupButtonBackgroundPainter createLinkButtonBackgroundPainter() {
        return new PopupButtonBackgroundPainter(linkBackgroundPressed, 
                linkBackgroundRollover, linkArcWidth, linkArcHeight);
    }
    
    /**
     * Creates a background painter for buttons with the lighter colour scheme.
     *  These are usually placed on components with generally lighter colouring
     *  that the dark buttons.
     */
    public ButtonBackgroundPainter createLightFullButtonBackgroundPainter() {
        return new LightButtonBackgroundPainter();
    }
    
    /**
     * Creates a background painter for buttons with the green colour scheme.
     *  These are usually placed on components with generally lighter colouring
     *  that the dark buttons.
     */
    public ButtonBackgroundPainter createGreenFullButtonBackgroundPainter() {
        return new GreenButtonBackgroundPainter();
    }
    
    /**
     * Creates a background painter for the buttons with darker colour schemes, however
     *  allows overriding of the default draw mode (left rounded, fully rounded, etc.) 
     *  and accent type (shadow, bubble, etc.).  Buttons with the dark scheme are usually
     *  used on the dark header bars where a certain accent type looks good.  This factory
     *  method is used for creating buttons to be used in other locations. 
     */
    public ButtonBackgroundPainter createDarkFullButtonBackgroundPainter(DrawMode mode, 
            AccentType accent) {
        return new DarkButtonBackgroundPainter(mode, accent);
    }
    
    /**
     * Creates a background painter for buttons that only need a rounded border and no
     *  mouseover or click effects.
     */
    public FlatButtonBackgroundPainter createFlatButtonBackgroundPainter() {
        return new FlatButtonBackgroundPainter();
    }
}
