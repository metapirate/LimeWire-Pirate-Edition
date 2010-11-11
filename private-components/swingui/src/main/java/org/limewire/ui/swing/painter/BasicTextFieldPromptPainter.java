package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;

/**
 * Used to paint a prompt message on a component.  This 
 *  painter must be fired manually by a prompt ready
 *  component when needed.  It is not to be used as a 
 *  standard foreground painter because it has no logic for
 *  when and when not to paint the prompt.
 *  
 * Right now only supports Components that extend JTextField,
 *  specifically PromptTextField and PromptPasswordField.
 */
public class BasicTextFieldPromptPainter<X extends JTextField> extends AbstractPainter<X> {

    private final Paint promptForeground;
    
    public BasicTextFieldPromptPainter(Paint promptForeground) {
        
        this.promptForeground = promptForeground;
        
        setAntialiasing(true);
    }
    
    @Override
    protected void doPaint(Graphics2D g, X object, int width, int height) {
        g.setPaint(promptForeground);
        g.setFont(object.getFont());
        
        int dot  = object.getCaret().getDot();
        Rectangle r = null;
        
        // Find the carat position
        try {
            r = object.modelToView(dot);
        } catch (BadLocationException e) { 
            // Carat location could not be found 
            //  therefore do not attempt to print
            //  the prompt text since 
            //  it will not match properly with
            //  the text position
            return; 
        }
        
        int x = r.x;
        int y = r.y + r.height - 3;
        g.drawString(getPromptText(object), x, y);
    }
    
    private static String getPromptText(Object object) {
        if (object instanceof PromptTextField) {
            return ((PromptTextField) object).getPromptText();
        } 
        else if (object instanceof PromptPasswordField) {
            return ((PromptPasswordField) object).getPromptText();
        } 
        else {
            throw new IllegalArgumentException("Prompt painter does not yet support " + object.getClass());
        }
    }
    

    
}
