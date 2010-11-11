package org.limewire.ui.swing.library.sharing;

import java.util.Collections;
import java.util.List;

import org.limewire.friend.api.Friend;

class EditableSharingData {

    private final Friend friend;
    private boolean selected;
    private List<String> ids;
    
    public EditableSharingData(Friend friend, boolean isSelected) {
        this.friend = friend;
        this.selected = isSelected;
        this.ids = null;
    }
    
    public EditableSharingData(List<String> ids, boolean selected) {
        this.ids = ids;
        this.friend = null;
        this.selected = selected;
    }
    
    public Friend getFriend() {
        return friend;
    }
    
    public List<String> getIds() {
        if(ids == null) {
            return Collections.singletonList(friend.getId());
        } else {
            return ids;
        }
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean value) {
        this.selected = value;
    }
}
