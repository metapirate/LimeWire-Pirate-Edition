package org.limewire.ui.swing.search.resultpanel;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.util.I18n;

/**
 * Check settings and if applicable warn user using dialog if downloading unlicensed file.
 * If user chooses not to download, do not continue download.
 *
 */
public class LicenseWarningDownloadPreprocessor implements DownloadPreprocessor {

    // backward compatbility
    public static final int SKIP_WARNING_VALUE = 101;
    public static final int SHOW_WARNING_VALUE = 0;

    public boolean execute(VisualSearchResult vsr) {
        return hasConfirmedLicenseWarningIfNecessary(vsr);
    }

    private boolean hasConfirmedLicenseWarningIfNecessary(VisualSearchResult vsr) {

        boolean initialLicenseWarning = QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.getValue() != SKIP_WARNING_VALUE;

        if (!vsr.isLicensed() && initialLicenseWarning) {
            // show dialog.  if "no", don't download, just return.

            String licenseWarning = I18n.tr("LimeWire is unable to find a license for this file. " +
                    "Download it anyway?\n\nLimeWire cannot monitor or control " +
                    "the content of the P2P Network, so please respect your local copyright laws.");

            JPanel thePanel = new JPanel(new BorderLayout(0, 15));
            thePanel.setOpaque(false);
            JCheckBox option = new JCheckBox(I18n.tr("Do not warn me again"));
            option.setOpaque(false);
            JComponent lbl = new MultiLineLabel(licenseWarning, 400);
            thePanel.add(lbl, BorderLayout.NORTH);
            thePanel.add(option, BorderLayout.WEST);
            option.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    int skipWarningValue =
                            (e.getStateChange() == ItemEvent.SELECTED) ? SKIP_WARNING_VALUE : SHOW_WARNING_VALUE;
                    QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.setValue(skipWarningValue);
                }
            });

            if (FocusJOptionPane.showConfirmDialog(null, thePanel, I18n.tr("Warning"),
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                // User pressed "No". Ensure that we always show the license warning
                if (QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.getValue() == SKIP_WARNING_VALUE) {
                    QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.setValue(SHOW_WARNING_VALUE);
                }
                return false;
            }
        }
        return true;
    }
}
