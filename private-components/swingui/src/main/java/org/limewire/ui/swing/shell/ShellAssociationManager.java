package org.limewire.ui.swing.shell;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;

import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.components.YesNoCheckBoxDialog;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

/**
 * Stores all the LimeAssociationOptions that LimeWire is set to use.
 */
public class ShellAssociationManager {

    /**
     * Runs through all the associations that this manager is handling and
     * checks to see if they can be enabled. If they can't be enabled but should
     * be, it checks the users warning settings. If the user has been selected
     * to be notified the user will be prompted if they want their associations
     * to be fixed.
     * 
     * @param frame frame for dialog to be relative to
     */
    public void validateFileAssociations(final JFrame frame) {
        final LimeAssociationOption torrentAssociationOption = LimeAssociations
                .getTorrentAssociation();
        applyAvailableAssociation(torrentAssociationOption, SwingUiSettings.HANDLE_TORRENTS);

        final LimeAssociationOption magnetAssociationOption = LimeAssociations
                .getMagnetAssociation();
        applyAvailableAssociation(magnetAssociationOption, SwingUiSettings.HANDLE_MAGNETS);

        boolean torrentsStolen = isSettingStolen(torrentAssociationOption,
                SwingUiSettings.HANDLE_TORRENTS);
        boolean magnetsStolen = isSettingStolen(magnetAssociationOption,
                SwingUiSettings.HANDLE_MAGNETS);

        if (SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES.getValue()
                && (torrentsStolen || magnetsStolen)) {

            String message = getMessage(torrentsStolen, magnetsStolen);

            final YesNoCheckBoxDialog yesNoCheckBoxDialog = new YesNoCheckBoxDialog(message, I18n
                    .tr("Warn me when other programs take LimeWire associations"),
                    SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES.getValue());
            yesNoCheckBoxDialog.setLocationRelativeTo(frame);
            yesNoCheckBoxDialog.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    SwingUiSettings.WARN_FILE_ASSOCIATION_CHANGES.setValue(yesNoCheckBoxDialog
                            .isCheckBoxSelected());
                    boolean shouldReAssociate = YesNoCheckBoxDialog.YES_COMMAND.equals(e
                            .getActionCommand());
                    if (shouldReAssociate) {
                        fixAssociation(torrentAssociationOption,
                                SwingUiSettings.HANDLE_TORRENTS);
                        fixAssociation(magnetAssociationOption, SwingUiSettings.HANDLE_MAGNETS);
                    } else {
                        updateSettings(torrentAssociationOption, magnetAssociationOption);
                    }
                }

            });
            yesNoCheckBoxDialog.setVisible(true);
        } else {
            updateSettings(torrentAssociationOption, magnetAssociationOption);
        }
    }

    private String getMessage(boolean torrentsStolen, boolean magnetsStolen) {
        if (torrentsStolen && magnetsStolen) {
            return I18n
                    .tr("Torrent files and magnet links are no longer associated with LimeWire. Would you like LimeWire to re-associate them?");
        } else if (torrentsStolen) {
            return I18n
                    .tr("Torrent files are no longer associated with LimeWire. Would you like LimeWire to re-associate them?");

        } else {
            return I18n
                    .tr("Magnet links are no longer associated with LimeWire. Would you like LimeWire to re-associate them?");
        }
    }

    /**
     * Helper method to update the settings to relfect the actual file
     * association when needed.
     */
    private void updateSettings(final LimeAssociationOption torrentAssociationOption,
            final LimeAssociationOption magnetAssociationOption) {
        updateSetting(torrentAssociationOption, SwingUiSettings.HANDLE_TORRENTS);
        updateSetting(magnetAssociationOption, SwingUiSettings.HANDLE_MAGNETS);
    }

    /**
     * Helper method to update a single setting to the value of its association.
     */
    private void updateSetting(LimeAssociationOption associationOption, BooleanSetting handleType) {
        handleType.setValue(associationOption != null && associationOption.isEnabled());
    }

    /**
     * Helper method to determine if a setting has been stolen by another
     * application.
     */
    private boolean isSettingStolen(LimeAssociationOption associationOption,
            BooleanSetting handleType) {
        return associationOption != null && handleType.getValue() && !associationOption.isEnabled();
    }

    /**
     * Updates an association based on the supplied BooleanSetting
     */
    private void fixAssociation(LimeAssociationOption associationOption, BooleanSetting handleType) {
        if (associationOption != null) {
            associationOption.setEnabled(handleType.getValue());
        }
    }

    /**
     * Enables an association if the supplied boolean setting is true and not
     * other application is using the association.
     */
    private void applyAvailableAssociation(LimeAssociationOption associationOption,
            BooleanSetting handleType) {
        if (associationOption != null) {
            if (!associationOption.isEnabled() && handleType.getValue()
                    && associationOption.isAvailable()) {
                associationOption.setEnabled(true);
            }
        }
    }
}
