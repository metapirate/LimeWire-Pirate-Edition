package org.limewire.ui.swing.components.decorators;

import java.awt.Paint;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.factories.TextFieldPainterFactory;
import org.limewire.ui.swing.util.SwingHacks;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * A class used to apply the limewire look and feel to various text input components.
 */
@Singleton
public class TextFieldDecorator {
    
    private final Provider<TextFieldPainterFactory> painterFactory;
    private TextFieldPainterFactory factory;
    
    @Inject
    TextFieldDecorator(Provider<TextFieldPainterFactory> painterFactory) {
        this.painterFactory = painterFactory;
    }
    
    /**
     * Decorates the specified text field using a background painter that 
     * includes an X icon to clear the field.
     */
    public void decorateClearablePromptField(PromptTextField field, AccentType accent) {
        field.setBackgroundPainter(getTextFieldFactory().createClearableBackgroundPainter(field, accent));
        // Get installed border, and restore it at the end.  This has a larger
        // right margin to prevent text from running into the reset icon.
        Border border = field.getBorder();
        decorateGeneralText(field);
        field.setBorder(border);
    }
    
    /**
     * Applies the lw look and feel to a prompt text field, however allows the accent
     *  and border to be overridden for allowing it to blend in and be used on non default panels.
     */
    public void decoratePromptField(PromptTextField field, AccentType accent, Paint border) {
        field.setBackgroundPainter(getTextFieldFactory().createBasicBackgroundPainter(accent, border));
        decorateGeneralText(field);
    }
    
    /**
     * Applies the LimeWire look and feel to a prompt text field, however allows the accent
     *  to be overridden for allowing it to blend in and be used on non default panels.
     */
    public void decoratePromptField(PromptTextField field, AccentType accent) {
        field.setBackgroundPainter(getTextFieldFactory().createBasicBackgroundPainter(accent));
        decorateGeneralText(field);
    }
   
    /**
     * Applies the LimeWire look and feel to a password prompt text field, however allows the accent
     *  and border to be overridden for allowing it to blend in and be used on non default panels.
     */
    public void decoratePromptField(PromptPasswordField field, AccentType accent, Paint border) {
        field.setBackgroundPainter(getTextFieldFactory().createBasicBackgroundPainter(accent, border));
        decorateGeneralPassword(field);
    }
    
    /**
     * Applies the LimeWire look and feel to a password prompt text field, however allows the accent
     *  to be overridden for allowing it to blend in and be used on non default panels.
     */
    public void decoratePromptField(PromptPasswordField field, AccentType accent) {
        field.setBackgroundPainter(getTextFieldFactory().createBasicBackgroundPainter(accent));
        decorateGeneralPassword(field);
    }
    
    private void decorateGeneralText(PromptTextField field) {
        field.setPromptPainter(getTextFieldFactory().createBasicPromptPainter());
        decorateGeneral(field);
    }
    
    private void decorateGeneralPassword(PromptPasswordField field) {
        field.setPromptPainter(getTextFieldFactory().createBasicPromptPainter());
        decorateGeneral(field);
    }
    
    private void decorateGeneral(JTextField field) {
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(2,10,2,12));
        SwingHacks.fixTextFieldScrollClippingWithNonDefaultInsets(field);
    }
    
    private TextFieldPainterFactory getTextFieldFactory() {
        if(factory == null)
            factory = painterFactory.get();
        return factory;
    }

}
