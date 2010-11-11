package org.limewire.ui.swing.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;

/** A {@link javax.swing.ListModel} for {@link JList} that is based off a {@link List}. */
public class CollectionBackedListModel extends AbstractListModel {
    
    protected final List data;
    
    public CollectionBackedListModel(Collection<?> data) {
        if(data instanceof List<?>) {
            this.data = (List<?>)data;
        } else {
            this.data = new ArrayList<Object>(data);
        }
    }
    
    @Override
    public Object getElementAt(int index) {
        return data.get(index);
    }
    
    @Override
    public int getSize() {
        return data.size();
    }
}
