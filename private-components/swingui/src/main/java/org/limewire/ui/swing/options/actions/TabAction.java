package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.limewire.ui.swing.options.OptionTabItem;
import org.limewire.ui.swing.options.TabItemListener;

public class TabAction extends AbstractAction {
    private OptionTabItem item;
    
    public TabAction(Icon icon, final OptionTabItem item) {
        super(item.getId(), icon);
        
        this.item = item;
        item.addTabItemListener(new TabItemListener(){
            @Override
            public void itemSelected(boolean selected) {
                putValue(SELECTED_KEY, selected);
            }
        });
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        item.select();
    }
    
    public OptionTabItem getTabItem() {
        return item;
    }
}
