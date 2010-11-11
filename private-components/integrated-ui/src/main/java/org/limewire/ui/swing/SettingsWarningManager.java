package org.limewire.ui.swing;

import org.limewire.i18n.I18nMarker;
import org.limewire.service.MessageService;

import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Provides methods to display notifications for common settings problems.
 */
public class SettingsWarningManager {

    /**
     * Warn about temporary directories.
     */
    public static void checkTemporaryDirectoryUsage() {
        if (LimeWireUtils.isTemporaryDirectoryInUse()) {
            MessageService
                    .showMessage(I18nMarker
                            .marktr("LimeWire was unable to create your settings folder and is using a temporary folder.  Your settings may be deleted when you close LimeWire. "));
        }
    }

    /**
     * Warn about load/save problems.
     */
    public static void checkSettingsLoadSaveFailure() {
        if (LimeWireUtils.hasSettingsLoadSaveFailures()) {
            LimeWireUtils.resetSettingsLoadSaveFailures();
            MessageService
                    .showMessage(I18nMarker
                            .marktr("LimeWire has encountered problems in managing your settings.  Your settings changes may not be saved on shutdown."));
        }
    }

}
