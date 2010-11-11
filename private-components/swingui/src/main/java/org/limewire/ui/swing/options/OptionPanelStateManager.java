package org.limewire.ui.swing.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.limewire.setting.Setting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.google.inject.Inject;

/**
 * Shared class to allow the options panel to use an {@link OptionPanel}
 *  in multiple locations.
 *  
 * TODO: an option panel should not be used in two places!
 */
public class OptionPanelStateManager {
    
    private final Map<Setting, Object> activeSettingMap;
    private final Map<Setting, SettingListener> settingListeners;
    private final Set<SettingChangedListener> listeners;
    
    @Inject
    public OptionPanelStateManager() {
        activeSettingMap = new HashMap<Setting, Object>();
        settingListeners = new HashMap<Setting, SettingListener>();
        listeners = new HashSet<SettingChangedListener>();
    }
    
    public Object getValue(Setting setting) {
        Object value = activeSettingMap.get(setting);
        if (value != null) {
            return value;
        }
        else {
            return setting.get();
        }
    }
    
    public void setValue(Setting setting, Object value) {
        Object savedValue = activeSettingMap.get(setting); 
        if (savedValue == null || !savedValue.equals(value)) {
            activeSettingMap.put(setting, value);
            fireChanges(setting);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void saveSettings() {
        for ( Setting key : activeSettingMap.keySet() ) {
            Object value = activeSettingMap.get(key);
            if(value != null) {
                key.set(value);
            }
        }
    }
    
    public boolean hasPendingChanges() {
        for ( Setting key : activeSettingMap.keySet() ) {
            Object savedValue = activeSettingMap.get(key); 
            if (savedValue != null && !savedValue.equals(key.get())) {
                return true;
            }
        }
        return false;
    }
    
    private void fireChanges(Setting setting) {
        for ( SettingChangedListener listener : listeners ) {
            listener.settingChanged(setting);
        }
    }
    
    public void addSettingChangedListener(SettingChangedListener listener) {
        listeners.add(listener);
    }
    
    public void removeSettingChangedListener(SettingChangedListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Interface to report pending changes to a given setting.
     */
    public static interface SettingChangedListener {
        public void settingChanged(Setting setting);
    }

    public void registerSettingListener(final Setting setting) {
        SettingListener settingListener = settingListeners.get(setting);
        if(settingListener == null) {
            settingListener = new SettingListener() {
                @Override
                public void settingChanged(final SettingEvent evt) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                         public void run() {
                            setValue(setting, setting.get());
                         } 
                    }); 
                }
            };
            setting.addSettingListener(settingListener);
            settingListeners.put(setting, settingListener);
        }
    }
    
    /**
     * Reinitializes the settings map.
     */
    public void initOptions() {
        for(Setting setting : activeSettingMap.keySet()) {
            setValue(setting, setting.get());
        }
    }
}
