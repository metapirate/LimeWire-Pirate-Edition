/**
 * 
 */
package org.limewire.ui.swing.util;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

public class SearchSettingListener implements SettingListener {
    private final JComboBox combo;
    private final BooleanSetting booleanSetting;
    private final SearchCategory searchCategory;
    private boolean oldValue;
    

    /**
     * Listener tracking changes to the given boolean setting, depending on the setting 
     * value the provided search category is added or removed from the given combo box.
     */
    public SearchSettingListener(BooleanSetting booleanSetting, SearchCategory searchCategory, JComboBox combo) {
        this.booleanSetting = booleanSetting;
        this.searchCategory = searchCategory;
        this.combo = combo;
        oldValue = booleanSetting.getValue();
        if (!booleanSetting.getValue()) {
            combo.removeItem(searchCategory);
        }
    }

    @Override
    public void settingChanged(SettingEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean newValue = booleanSetting.getValue();
                if(oldValue != newValue) {
                    if(newValue) {
                        combo.addItem(searchCategory);
                    } else {
                        if(combo.getSelectedItem() == searchCategory) {
                            combo.setSelectedItem(SearchCategory.ALL);
                        }
                        combo.removeItem(searchCategory);
                    }
                    oldValue = newValue;
                }     
            }
        });
    }
}