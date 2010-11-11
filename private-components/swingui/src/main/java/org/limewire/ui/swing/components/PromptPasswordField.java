package org.limewire.ui.swing.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.jdesktop.swingx.painter.Painter;

/**
 * A standard JPasswordField that can be skinned and can take a prompt message to 
 *  show when the text field is empty of user input.
 */
public class PromptPasswordField extends JPasswordField implements FocusListener {
    
    private String promptText;
    
    private Painter<JTextField> backgroundPainter;
    private Painter<JTextField> promptPainter;
   
    /**
     * Creates a default instance of the text box with 
     *  no skin and no prompt message.
     */
    public PromptPasswordField() {
        this.init();
    }
  
    /**
     * Creates a default skinless instance using the given 
     *  prompt message.
     */
    public PromptPasswordField(String promptText) {
        this.setPromptText(promptText);        
        this.init();
    }

    /**
     * Creates a default skinless instance using the given 
     *  prompt message and initial number of columns.
     */
    public PromptPasswordField(String promptText, int columns) {
        this.promptText = promptText;
        this.setColumns(columns);        
        init();
    }
    
    private void init() {

        TextFieldClipboardControl.install(this);
        this.addFocusListener(this);
                
        this.setMinimumSize(new Dimension(150,22));
        this.setPreferredSize(this.getMinimumSize());
    }
    
    /**
     * Sets the prompt message to be used when there is no 
     *  user input in the field.
     */
    public void setPromptText(String text){
        this.promptText = text;
    }
    
    /**
     * Returns the prompt message to be used when there is no 
     *  user input in the field.
     */
    public String getPromptText() {
        return this.promptText;
    }
    
    /**
     * Repaints this component when focus is gained
     * so default text can be removed.
     */
    @Override
    public void focusGained(FocusEvent e) {
        // Select all text if focus gained from another component.
        // But don't do it if it's temporary -- this is a hack to allow
        // focus to gain on the search field when clicking a search tab,
        // without selecting it all.
        if (e.getOppositeComponent() != null && !e.isTemporary()) {
            selectAll();
        }
        repaint();
    }
    
    /**
     * Repaints this component when focus is lost
     * so default text can be displayed if no text has been entered.
     */
    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }
    
    /**
     * Sets the background painter for this component.
     */
    public void setBackgroundPainter(Painter<JTextField> painter) {
        this.backgroundPainter = painter;
    }
    
    /**
     * Sets the painter on this component that manages painting of 
     *  the prompt text when the text field is empty.
     */
    public void setPromptPainter(Painter<JTextField> painter) {
        this.promptPainter = painter;
    }
    
    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {

        if (backgroundPainter != null) {
            this.backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }
            
        super.paintComponent(g);
    
        if (promptPainter != null && !hasFocus() && new String(getPassword()).isEmpty() && promptText != null) {
            promptPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }

    }
 }
