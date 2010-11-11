package org.limewire.ui.swing.wizard;

import javax.swing.JCheckBox;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.LimeAssociationOption;
import org.limewire.ui.swing.shell.LimeAssociations;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.WindowsUtils;
import org.limewire.util.OSUtils;

public class SetupPage1 extends WizardPage {

    private final JCheckBox associationFileTypeCheckBox;
    private final JCheckBox launchAtStartupCheckBox;
    
    public SetupPage1(SetupComponentDecorator decorator){
        super(decorator);
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0 14 0 0, gap 0, nogrid"));       
   
        associationFileTypeCheckBox = createAndDecorateCheckBox(true);
        launchAtStartupCheckBox = createAndDecorateCheckBox(true);

        //File Associations
        addFileAssociations();        
        initSettings();
    }
    
    /**
     * Adds header for file association and any appropriate checkboxes and text
     */
    private void addFileAssociations() {
        if (LimeAssociations.isMagnetAssociationSupported() 
                || LimeAssociations.isTorrentAssociationSupported()
                || shouldShowStartOnStartupWindow()) {
             add(createAndDecorateHeader(I18n.tr("File Associations and Startup")), "gaptop 20, span, wrap");

             if (LimeAssociations.isMagnetAssociationSupported() 
                     || LimeAssociations.isTorrentAssociationSupported()) {
                 add(associationFileTypeCheckBox, "gaptop 5, gapleft 26");
                 add(createAndDecorateMultiLine(I18n.tr("Associate magnet links and torrent files with LimeWire"), associationFileTypeCheckBox), "gaptop 5, gapleft 5, wrap");
             }
             
             if (shouldShowStartOnStartupWindow()) {
                 add(launchAtStartupCheckBox, "gaptop 5, gapleft 26");
                 add(createAndDecorateMultiLine(I18n.tr("Launch LimeWire at system startup"), launchAtStartupCheckBox), "gaptop 5, gapleft 5, wrap");
             }
        }
    }
    
    private void initSettings() {
        associationFileTypeCheckBox.setSelected(SwingUiSettings.HANDLE_MAGNETS.getValue());
        launchAtStartupCheckBox.setSelected(StartupSettings.RUN_ON_STARTUP.getValue());
    }

    @Override
    public String getLine1() {
        return I18n.tr("Please take a minute to configure these options before moving on.");
    }
    
    @Override
    public String getLine2() {
        return "";
    }
    
    @Override
    public String getFooter() {
        return OSUtils.isMacOSX() ? I18n.tr("All settings can be changed later from LimeWire > Preferences") :
            I18n.tr("All settings can be changed later in Tools > Options");
    }
    
    @Override
    public void applySettings() {
        // File Associations
        SwingUiSettings.HANDLE_MAGNETS.setValue(associationFileTypeCheckBox.isSelected());
        LimeAssociationOption magnetAssociationOption = LimeAssociations.getMagnetAssociation();
        if (magnetAssociationOption != null) {
            magnetAssociationOption.setEnabled(associationFileTypeCheckBox.isSelected());
        }

        SwingUiSettings.HANDLE_TORRENTS.setValue(associationFileTypeCheckBox.isSelected());
        LimeAssociationOption torrentAssociationOption = LimeAssociations.getTorrentAssociation();
        if (torrentAssociationOption != null) {
            torrentAssociationOption.setEnabled(associationFileTypeCheckBox.isSelected());
        }

        InstallSettings.ASSOCIATION_OPTION.setValue(2);

        // launch at startup
        if (shouldShowStartOnStartupWindow()) {
            if (OSUtils.isMacOSX())
                MacOSXUtils.setLoginStatus(launchAtStartupCheckBox.isSelected());
            else if (WindowsUtils.isLoginStatusAvailable())
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        WindowsUtils.setLoginStatus(launchAtStartupCheckBox.isSelected());
                    }
                });

            StartupSettings.RUN_ON_STARTUP.setValue(launchAtStartupCheckBox.isSelected());
        } else
            StartupSettings.RUN_ON_STARTUP.setValue(false);
        InstallSettings.START_STARTUP.setValue(true);
    }
    
    /**
     * Determines if the Start On Startup option is available.
     */
    private boolean shouldShowStartOnStartupWindow() {
        return OSUtils.isMacOSX() || OSUtils.isGoodWindows();
    }
}
