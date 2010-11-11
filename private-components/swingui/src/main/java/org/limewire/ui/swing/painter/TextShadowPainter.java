package org.limewire.ui.swing.painter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.util.PainterUtils;

public class TextShadowPainter<X extends Component> extends AbstractPainter<X> {
    
    private static final Paint DEFAULT_SHADOW = new Color(0,0,0,150);
    
    private Insets insets = PainterUtils.BLANK_INSETS;
    
    private final Paint shadow;
    
    
    public TextShadowPainter(Paint shadow, Insets insets, boolean cached) {
        this.setAntialiasing(true);
        this.setCacheable(cached);
        this.shadow = shadow;
        
        setInsets(insets);
    }
    
    public TextShadowPainter() {
        this.setAntialiasing(true);
        this.setCacheable(true);
        this.shadow = DEFAULT_SHADOW;
    }
    
    /**
     * Note: insets are not fully supported, at the moment only uses
     *        left and top to set an offset for painting
     */
    public void setInsets(Insets insets) {
        this.insets = insets;
    }
        
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {

        String label = getText(object);
        
        g.setFont(object.getFont());
        int h = g.getFontMetrics().getAscent();
           
        g.setPaint(shadow);
        g.drawString(label, insets.left + 1, insets.top + height/2 + h/2 - 1);
        g.setColor(object.getForeground());
        g.drawString(label, insets.left + 0, insets.top + height/2 + h/2 - 2);
    }
    
    /** 
     * Rip the displayable text out of a component without using providers
     *  or instanceof
     */
    private static String getText(Component object) {
        try {
            Method method = object.getClass().getMethod("getText");
            return method.invoke(object).toString();
        }
        catch (NoSuchMethodException e) {
            return "";
        }
        catch (InvocationTargetException e) {
            return "";
        }
        catch (IllegalAccessException e) {
            return "";
        }
    }
}