package org.limewire.ui.swing.downloads.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.malware.VirusEngine;
import org.limewire.setting.BooleanSetting;
import org.limewire.ui.swing.components.HTMLLabel;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * Content panel for the Anti-Virus Info dialog in the downloads table.
 */
public class AVInfoPanel extends JPanel {
    private static final String AVHOME = "#antivirus";
    private static final String AVHOME_URL = "http://www.avg.com/";
    private static final String DANGEROUS = "#dangerous";
    private static final String DANGEROUS_URL = "http://www.gnutellaforums.com/";

    @Resource private Font doNotShowFont;
    @Resource private Color doNotShowForeground;
    @Resource private Font headingFont;
    @Resource private Color headingForeground;
    @Resource private Font messageFont;
    @Resource private Color messageForeground;
    @Resource private Font threatHeadingFont;
    @Resource private Color threatHeadingForeground;
    @Resource private Icon threatIcon;
    @Resource private Icon vendorIcon;
    
    private JLabel headingLabel;
    private HTMLLabel messageLabel;
    
    private JButton okButton;
    private JCheckBox doNotShowCheckBox;
    private JLabel vendorLabel;
    
    /**
     * Constructs an AVInfoPanel.
     */
    @Inject
    public AVInfoPanel() {
        GuiUtils.assignResources(this);
        
        headingLabel = new JLabel();
        
        messageLabel = new HTMLLabel();
        messageLabel.setHtmlFont(messageFont);
        messageLabel.setHtmlForeground(messageForeground);
        messageLabel.setMargin(new Insets(0, 0, 0, 0));
        messageLabel.setOpaque(false);
        messageLabel.setOpenUrlsNatively(false);
        messageLabel.addHyperlinkListener(new MessageListener());
        
        okButton = new JButton();
        
        doNotShowCheckBox = new JCheckBox();
        doNotShowCheckBox.setFont(doNotShowFont);
        doNotShowCheckBox.setForeground(doNotShowForeground);
        doNotShowCheckBox.setOpaque(false);
        doNotShowCheckBox.setText(I18n.tr("Do not show message again"));
        
        vendorLabel = new JLabel(vendorIcon);
    }
    
    /**
     * Displays the AV vendor message.
     */
    public void showVendorMessage() {
        setLayout(new MigLayout("insets 25 15 15 15, gap 0 0"));
        
        headingLabel.setFont(headingFont);
        headingLabel.setForeground(headingForeground);
        headingLabel.setText(I18n.tr("Anti-Virus Protection"));
        
        // Set message text.  We also set the preferred size to control the
        // message and dialog width.
        String message = I18n.tr("LimeWire's Anti-Virus protection is powered by AVG.  AVG scans, detects and deletes files that are suspected to contain viruses or spyware.  <a href=\"{0}\">Learn more</a>", AVHOME);
        messageLabel.setText(message);
        messageLabel.setPreferredSize(new Dimension(300, messageLabel.getPreferredSize().height));
        
        // Set OK button action.
        okButton.setAction(new AbstractAction(I18n.tr("OK")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                disposeWindow();
            }
        });
        
        // Hide checkbox option.
        doNotShowCheckBox.setVisible(false);
        
        // Add components to container.
        add(headingLabel, "span, align left, wrap");
        add(messageLabel, "span, align left, wrap 15");
        add(okButton, "alignx left, aligny bottom");
        add(vendorLabel, "alignx right, aligny bottom, push");
        
        // Display as modal dialog.
        showDialog(I18n.tr("About AVG Anti-Virus"));
    }
    
    /**
     * Displays the dangerous file message.  This applies to files marked as 
     * hazardous by the internal dangerous file checker.
     * 
     * @param item the item representing the downloaded file
     * @param showOption true to show option to turn off warning
     */
    public void showDangerMessage(DownloadItem item, boolean showOption) {
        String heading = I18n.tr("Dangerous File");
        String message = I18n.tr("{0} is considered a dangerous file and has automatically been deleted for your protection.  <a href=\"{1}\">Learn more</a>", item.getFileName(), DANGEROUS);
        showWarningMessage(heading, message, showOption, false,
                SwingUiSettings.WARN_DOWNLOAD_DANGEROUS, SwingUiSettings.HIDE_DOWNLOAD_DANGEROUS);
    }
    
    /**
     * Displays the threat detected message.  This applies to infected files
     * as determined by the virus scanner.
     * 
     * @param item the item representing the downloaded file
     * @param showOption true to show option to turn off warning
     */
    public void showThreatMessage(DownloadItem item, boolean showOption) {
        String heading = I18n.tr("Threat Detected");
        String message = I18n.tr("{0} is suspected to contain a virus or spyware and has automatically been deleted for your protection.  LimeWire's Anti-Virus protection is powered by AVG.", item.getFileName());
        showWarningMessage(heading, message, showOption, true, 
                SwingUiSettings.WARN_DOWNLOAD_THREAT_FOUND, SwingUiSettings.HIDE_DOWNLOAD_THREAT_FOUND);
    }
    
    /**
     * Displays the failure message.  This applies to files that could not be 
     * scanned due to a problem with the virus scanner.
     * 
     * @param item the item representing the downloaded file
     * @param showOption true to show option to turn off warning
     */
    public void showFailureMessage(DownloadItem item, boolean showOption) {
        VirusEngine.HintReason reason = (VirusEngine.HintReason)item.getDownloadProperty(DownloadPropertyKey.ANTIVIRUS_FAIL_HINT);
        if(reason == null) {
            reason = VirusEngine.HintReason.NO_HINT;
        }
        
        String heading, message;
        switch(reason) {
        case NO_DEFINITIONS:
            heading = I18n.tr("Can't Scan Yet");
            message = I18n.tr("{0} could not be inspected because the virus scanner is still downloading its first set of virus definitions.  LimeWire Anti-Virus protection is powered by AVG.", item.getFileName());
            break;            
        case NOT_SUPPORTED:
            heading = I18n.tr("Unable to Scan");
            message = I18n.tr("{0} could not be inspected because the virus scanner cannot be loaded.  LimeWire Anti-Virus protection is powered by AVG.", item.getFileName());
            break;            
        default:
            heading = I18n.tr("Unable to Scan");
            message = I18n.tr("{0} could not be inspected due to a problem with the virus scanner.  LimeWire Anti-Virus protection is powered by AVG.", item.getFileName());
            break;            
        }        

        showWarningMessage(heading, message, showOption, true, 
                SwingUiSettings.WARN_DOWNLOAD_SCAN_FAILED, SwingUiSettings.HIDE_DOWNLOAD_SCAN_FAILED);
    }
    
    /**
     * Displays a warning message with the specified heading and message text.
     */
    private void showWarningMessage(String heading, String message, 
            boolean showOption, boolean showVendor, BooleanSetting warningSetting,
            BooleanSetting hideSetting) {
        // Skip if do-not-show option is visible and the warning is turned off.
        if (showOption && !warningSetting.getValue()) {
            return;
        }
        
        setLayout(new MigLayout("insets 25 15 15 15, gap 0 0, hidemode 3"));
        
        JLabel iconLabel = new JLabel(threatIcon);
        
        headingLabel.setFont(threatHeadingFont);
        headingLabel.setForeground(threatHeadingForeground);
        headingLabel.setText(heading);
        
        // Set message text.  We also set the preferred size to control the
        // message and dialog width.
        messageLabel.setText(message);
        messageLabel.setPreferredSize(new Dimension(330, messageLabel.getPreferredSize().height));
        
        // Set OK button action.
        okButton.setAction(new CloseAction(warningSetting, hideSetting));
        
        // Set up checkbox option.
        doNotShowCheckBox.setSelected(hideSetting.getValue());
        doNotShowCheckBox.setVisible(showOption);
        
        // Add components to container.
        add(iconLabel, "spany, alignx left, aligny top, gaptop 6, gapright 15"); 
        add(headingLabel, "span, align left, wrap");
        add(messageLabel, "span, align left, wrap 15");
        add(okButton, "alignx left, aligny bottom");
        add(doNotShowCheckBox, "alignx left, aligny bottom, gapleft 5");
        if (showVendor) add(vendorLabel, "alignx right, aligny bottom, push");
        
        // Display as modal dialog.
        showDialog(I18n.tr("Warning"));
    }
    
    /**
     * Displays this panel in a modal dialog.
     */
    private void showDialog(String title) {
        // Create dialog.
        final JFrame owner = GuiUtils.getMainFrame();
        final JDialog dialog = new LimeJDialog(owner, title, true);
        
        // Set dialog properties.
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        
        // Add content to dialog.
        setBackground(owner.getBackground());
        dialog.add(this, BorderLayout.CENTER);
        
        // Add listener to resize and position dialog after opening.  This is 
        // necessary because we don't know the actual height of the message
        // component (JEditorPane) until after the window is displayed.  This
        // will properly size the dialog for any message length.
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                dialog.pack();
                dialog.setLocationRelativeTo(owner);
            }
        });
        
        // Position and display dialog.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
    
    /**
     * Closes the window containing this panel.
     */
    private void disposeWindow() {
        Container ancestor = getTopLevelAncestor();
        if (ancestor instanceof Window) {
            ((Window) ancestor).dispose();
        }
    }
    
    /**
     * Action to close window.  If the do-not-show option is visible, it is
     * applied to the appropriate setting.
     */
    private class CloseAction extends AbstractAction {
        private final BooleanSetting warningSetting;
        private final BooleanSetting hideSetting;

        public CloseAction(BooleanSetting warningSetting, BooleanSetting hideSetting) {
            super(I18n.tr("OK"));
            
            this.warningSetting = warningSetting;
            this.hideSetting = hideSetting;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Apply "do not show" option if visible.
            if (doNotShowCheckBox.isVisible()) {
                warningSetting.setValue(!doNotShowCheckBox.isSelected());
                hideSetting.setValue(doNotShowCheckBox.isSelected());
            }
            
            // Dispose of parent window.
            disposeWindow();
        }
    }
    
    /**
     * Listener to handle hyperlink events in the message.
     */
    private class MessageListener implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (AVHOME.equals(e.getDescription())) {
                    NativeLaunchUtils.openURL(AVHOME_URL);
                } else if (DANGEROUS.equals(e.getDescription())) {
                    NativeLaunchUtils.openURL(DANGEROUS_URL);
                }
            }
        }
    }
}
