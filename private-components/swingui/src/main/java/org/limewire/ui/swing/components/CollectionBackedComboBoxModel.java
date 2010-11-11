package org.limewire.ui.swing.components;

import java.util.Collection;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;

/**
 * A {@link ComboBoxModel} for {@link JComboBox} that is based off a
 * {@link List}.
 */
public class CollectionBackedComboBoxModel extends CollectionBackedListModel implements
        MutableComboBoxModel {

    private Object selectedObject;

    public CollectionBackedComboBoxModel(Collection<?> data) {
        super(data);
        if (getSize() > 0) {
            setSelectedItem(getElementAt(0));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addElement(Object obj) {
        data.add(obj);
        fireIntervalAdded(this, data.size() - 1, data.size() - 1);
        if (data.size() == 1 && selectedObject == null && obj != null) {
            setSelectedItem(obj);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void insertElementAt(Object obj, int index) {
        data.add(index, obj);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void removeElement(Object obj) {
        int index = data.indexOf(obj);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    @Override
    public void removeElementAt(int index) {
        if (getElementAt(index) == selectedObject) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }

        data.remove(index);

        fireIntervalRemoved(this, index, index);
    }

    @Override
    public Object getSelectedItem() {
        return selectedObject;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if ((selectedObject != null && !selectedObject.equals(anItem)) || selectedObject == null
                && anItem != null) {
            selectedObject = anItem;
            fireContentsChanged(this, -1, -1);
        }
    }

}
