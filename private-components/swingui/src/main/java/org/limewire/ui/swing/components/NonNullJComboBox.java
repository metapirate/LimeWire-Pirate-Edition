package org.limewire.ui.swing.components;

import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * This is workaround for two different OSX bugs. If the JComboBox will
 * not appear in OSX, there is no need to use this particular subclass.
 * Similarly, if a null state is acceptable for this particular JComboBox,
 * there is no reason to use this subclass.
 * <p>
 * The first bug is related to reselecting non-String based models.
 * When using a non-String based model, selecting an item and then
 * reselecting that item on the next action will cause the combo box
 * to be set to null. The renderer will show an empty String and 
 * calling getSelectedItem() will return null. The expected behaviour 
 * is for this not return null.
 * <p>
 * The second bug, I'm not sure if this is a bug or OSX feature though I
 * would qualify it as a bug either way, when holding the command key
 * and making a selection in a combo box, the item will be set to null.
 * Again the renderer will show an empty String and calling getSelectedItem()
 * will return null.
 * <p>
 * If these are ever fixed on OSX, this class may be removed.
 */
public class NonNullJComboBox extends JComboBox {
    
    public NonNullJComboBox() {
        super();
    }
    
    public NonNullJComboBox(Vector<?> items) {
        super(items);
    }
    
    public NonNullJComboBox(Object items[]) {
        super(items);
    }
    
    public NonNullJComboBox(ComboBoxModel aModel) {
        super(aModel);
    }
    
    @Override
    public void setSelectedIndex(int index) {
        if(index < 0 || index >= getModel().getSize())
            return;
        super.setSelectedIndex(index);
    }
    
    @Override
    public void setSelectedItem(Object o) {
        if(o == null)
            return;
        super.setSelectedItem(o);
    }
}
