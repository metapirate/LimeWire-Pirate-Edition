package org.limewire.ui.swing.util;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.text.html.StyleSheet;

public class FontUtils {
    
    private FontUtils() {}
    
    public static void changeSize(JComponent component, float increment) {
        Font font = component.getFont();
        if (font == null) return;
        float newSize = font.getSize() + increment;
        component.setFont(font.deriveFont(newSize));
    }
    
    public static void setSize(JComponent component, int size) {
        Font font = component.getFont();
        if(font == null) return;
        component.setFont(font.deriveFont((float)size));
    }
    
    public static void changeStyle(JComponent component, int style) {
        component.setFont(component.getFont().deriveFont(style));
    }
    
    public static void bold(JComponent component) {
        changeStyle(component, Font.BOLD);
    }
    
    public static void plain(JComponent component) {
        changeStyle(component, Font.PLAIN);
    }

    public static void underline(JComponent component) {
        Font font = component.getFont();
        if(font != null) {  
            component.setFont(deriveUnderline(font, true));
        }
    }
    
    public static boolean isUnderlined(JComponent component) {
        Font font = component.getFont();
        if(font != null) {
            Map<TextAttribute, ?> map = font.getAttributes();
            return map.get(TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE_ON;
        } else {
            return false;
        }
    }
    
    public static void removeUnderline(JComponent component) {
        Font font = component.getFont();        
        if (font != null) {
            component.setFont(deriveUnderline(font, false));
        }
    }

    public static Font deriveUnderline(Font font, boolean underlined) {
        Map<TextAttribute, ?> map = font.getAttributes();
        Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
        newMap.put(TextAttribute.UNDERLINE, underlined ? TextAttribute.UNDERLINE_ON : Integer.valueOf(-1));
        return font.deriveFont(newMap);
    }
    
    /**
     * Determines if a font can display up to a point in the string.
     * <p>
     * Returns -1 if it can display the whole string.
     */
    public static boolean canDisplay(Font f, String s) {
        int upTo = f.canDisplayUpTo(s);
        if(upTo >= s.length() || upTo == -1)
            return true;
        else
            return false;
    }

    
    private static String unpackText(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof Action)
            return (String) ((Action) object).getValue(Action.NAME);
        else
            return object.toString();
    }

    public static Rectangle2D getLongestTextArea(Font font, Object... objects) {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        Rectangle2D largestRect = new Rectangle();
        for (int i = 0; i < objects.length; i++) {
            Object obj = objects[i];
            Rectangle2D currentRect = font.getStringBounds(unpackText(obj), frc);
            if(obj instanceof Action) {
                Icon icon = (Icon)((Action)obj).getValue(Action.SMALL_ICON);
                if(icon != null) {
                    // add some whitespace around the icons.
                    currentRect.setRect(currentRect.getX(), currentRect.getY(),
                            currentRect.getWidth()+icon.getIconWidth() + 10, currentRect.getHeight());
                }
            }
            if (currentRect.getWidth() > largestRect.getWidth()) {
                largestRect = currentRect;
            }
        }
        return largestRect;
    }
    
    /**
     * Truncates the given message to a maxWidth in pixels.
     */
    public static String getTruncatedMessage(String message, Font font, int maxWidth) {
        String ELIPSES = "...";
        while (getPixelWidth(message, font) > (maxWidth)) {
            message = message.substring(0, message.length() - (ELIPSES.length() + 1)) + ELIPSES;
        }
        return message;
    }

    /**
     * Returns the width of the message in the given font.
     */
    public static int getPixelWidth(String text, Font font) {
        StyleSheet css = new StyleSheet();
        FontMetrics fontMetrics = css.getFontMetrics(font);
        return fontMetrics.stringWidth(text);
    }
}
