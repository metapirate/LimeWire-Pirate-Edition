package org.limewire.ui.swing.components.decorators;

import java.awt.Cursor;

import javax.swing.BorderFactory;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Factory (and decorator used when extending LimeComboBox) that creates the three
 *  types of of "combo boxes" we use in the LimeWire UI.
 *  
 *  Types:
 *  <xmp>
 *    Full - The full combo box type with a selectable slot.  These work mostly 
 *            the same as regular JComboBoxes.  
 *            ie.  Search category dropdown.
 *            
 *   Mini - These boxes act more as toggles for drop down menu's of actions.
 *           ie.  The More button, the from widget, etc.
 *  
 *  Colour Scheme:
 *  
 *    Dark - Use the a "dark" colour scheme.  These are usually found
 *                        ontop of dark panels such at the header bars
 *                      
 *    Light - Use a ligher colour sheme.  These are usually found on top of 
 *                        lightly coloured panels.  In this case mostly 
 *                        the top search bar
 *                        
 *                        
 *                        
 * </xmp>
 */
@Singleton
public class ComboBoxDecorator {
    
    private final ButtonDecorator buttonDecorator;
    
    @Resource private Icon miniRegIcon;
    @Resource private Icon miniHoverIcon;
    @Resource private Icon miniDownIcon;
    @Resource private Icon lightFullIcon;
    @Resource private Icon darkFullIcon;
    
    @Inject
    ComboBoxDecorator(ButtonDecorator buttonDecorator) {
        GuiUtils.assignResources(this);  
        
        this.buttonDecorator = buttonDecorator;        
    }
    
    public void decorateDarkFullComboBox(JXButton box) {
        buttonDecorator.decorateDarkFullButton(box);
        box.setIcon(darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
    }    
    
    /**
     * Decorates a combo box in the default style however allows the
     *  accent to be overridden.
     */
    public void decorateDarkFullComboBox(JXButton box, AccentType accentType) {
        buttonDecorator.decorateDarkFullButton(box, accentType);
        box.setIcon(darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
    }
    
    public void decorateLightFullComboBox(JXButton box) {
        buttonDecorator.decorateLightFullButton(box);
        box.setIcon(lightFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
    }
    
    public void decorateMiniComboBox(JXButton box) {
        buttonDecorator.decorateMiniButton(box);
        box.setIcon(miniRegIcon);
        box.setRolloverIcon(miniHoverIcon);
        box.setPressedIcon(miniDownIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,6,3,6));
        
        tryInstallHandCursor(box);
    }
    
    public void decorateLinkComboBox(JXButton box) {
        buttonDecorator.decorateLinkButton(box);
        box.setIcon(miniRegIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,6,3,6));
        
        tryInstallHandCursor(box);
    }
    
    /**
     * Decorates the specified combobox button by removing all background and
     * border elements so that only its icons are displayed.
     */
    public void decorateIconComboBox(JXButton box) {
        IconButton.setIconButtonProperties(box);
        tryInstallHandCursor(box);
    }

    private void tryInstallHandCursor(JXButton box) {
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
