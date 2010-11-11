package org.limewire.ui.swing.options;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;

@LazySingleton
public class UnsafeTypeOptionPanelStateManager extends OptionPanelStateManager {
    @Inject
    public UnsafeTypeOptionPanelStateManager() {
        registerSettingListener(LibrarySettings.ALLOW_PROGRAMS);
        registerSettingListener(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING);
    }
}
