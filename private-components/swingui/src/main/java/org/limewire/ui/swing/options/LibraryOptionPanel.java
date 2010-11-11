package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.options.actions.DialogDisplayAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/** Library Option View */
public class LibraryOptionPanel extends OptionPanel {
    
    @Resource private Icon p2pSharedListIcon;
    @Resource private Icon sharingMyFilesIcon;
    @Resource private Icon sharingArrowIcon;
    
    private final UsePlayerPanel playerPanel;
    
    private OptionPanel iTunesPanel;
    private OptionPanel sharingPanel;
    
    private final Provider<UnsafeTypeOptionPanel> unsafeOptionPanelProvider;
    private final Provider<ITunesOptionPanel> iTunesOptionPanelProvider;
    
    @Inject
    public LibraryOptionPanel(Provider<UnsafeTypeOptionPanel> unsafeTypeOptionPanelProvider,
            Provider<ITunesOptionPanel> iTunesOptionPanelProvider) {
        this.unsafeOptionPanelProvider = unsafeTypeOptionPanelProvider;
        this.iTunesOptionPanelProvider = iTunesOptionPanelProvider;
        
        GuiUtils.assignResources(this);
        
        this.playerPanel = new UsePlayerPanel();
        
        setLayout(new MigLayout("insets 15, fillx, gap 4"));

        add(getSharingPanel(), "growx, wrap");

        if(OSUtils.isMacOSX() || OSUtils.isWindows()) {
            add(getITunesPanel(), "growx, wrap");
        } else {
            iTunesPanel = null;
        }
        
        add(playerPanel, "wrap");
    }

    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        
        result = playerPanel.applyOptions();
        if (result.isSuccessful())
            result.applyResult(getSharingPanel().applyOptions());
        
        if (iTunesPanel != null && result.isSuccessful())
            result.applyResult(iTunesPanel.applyOptions());
        
        return result;
    }

    @Override
    boolean hasChanged() {
        return playerPanel.hasChanged() || getSharingPanel().hasChanged()
            || iTunesPanel != null ? iTunesPanel.hasChanged() : false;
    }

    @Override
    public void initOptions() {
        getSharingPanel().initOptions();
        playerPanel.initOptions();
        if (iTunesPanel != null) {
            iTunesPanel.initOptions();
        }
    }

    private class ITunesPanel extends OptionPanel {

        private ITunesOptionPanel iTunesOptionPanel; 
        
        public ITunesPanel() {
            super("iTunes");

            iTunesOptionPanel = iTunesOptionPanelProvider.get();
            
            JButton configureButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    iTunesOptionPanel, I18n.tr("iTunes Configuration"),
                    I18n.tr("Configure..."), I18n.tr("Configure iTunes")));
            
            add(new JLabel(I18n.tr("Configure how files in your LimeWire interact with iTunes")));
            add(configureButton, "gapleft push");
        }

        @Override
        ApplyOptionResult applyOptions() {
            return iTunesOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return iTunesOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            iTunesOptionPanel.initOptions();
        }
    }

    
    /** Do you want to use the LW player? */
    private static class UsePlayerPanel extends OptionPanel {

        private JCheckBox useLimeWirePlayer;

        public UsePlayerPanel() {
            super("");
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new MigLayout("ins 0 0 0 0, gap 0"));

            useLimeWirePlayer = new JCheckBox(I18n
                    .tr("Use the LimeWire player when I play Audio and Video files"));
            useLimeWirePlayer.setOpaque(false);

            add(useLimeWirePlayer);
        }

        @Override
        ApplyOptionResult applyOptions() {
            SwingUiSettings.PLAYER_ENABLED.setValue(useLimeWirePlayer.isSelected());
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return useLimeWirePlayer.isSelected() != SwingUiSettings.PLAYER_ENABLED.getValue();
        }

        @Override
        public void initOptions() {
            useLimeWirePlayer.setSelected(SwingUiSettings.PLAYER_ENABLED.getValue());
        }
    }
    
    private OptionPanel getSharingPanel() {
        if(sharingPanel == null) {
            sharingPanel = new SharingPanel();
        }
        
        return sharingPanel;
    }
    
    private OptionPanel getITunesPanel() {
        if (iTunesPanel == null) {
            iTunesPanel = new ITunesPanel();
        }
        return iTunesPanel;
    }
    
    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getITunesPanel().setOptionTabItem(tab);
        getSharingPanel().setOptionTabItem(tab);
        this.playerPanel.setOptionTabItem(tab);

    }
    
    private class SharingPanel extends OptionPanel {
        
        private final JButton configureButton; 
        private final JCheckBox shareP2PdownloadedFilesCheckBox;
        private final UnsafeTypeOptionPanel unsafeTypeOptionPanel;
        private JLabel unsafeMessageLabel;
        
        public SharingPanel() {
            super(I18n.tr("Sharing"));
            
            unsafeTypeOptionPanel = unsafeOptionPanelProvider.get();
            
            shareP2PdownloadedFilesCheckBox = new JCheckBox("<html>"+I18n.tr("Add files I download from P2P Users to my Public Shared List")+"</html>");
            shareP2PdownloadedFilesCheckBox.setOpaque(false);
            
            configureButton = new JButton(new DialogDisplayAction(LibraryOptionPanel.this,
                    unsafeTypeOptionPanel, I18n.tr("Unsafe File Sharing"),
                    I18n.tr("Configure..."), I18n.tr("Configure unsafe file sharing settings")));
            final String learnMoreUrl = "http://www.gnutellaforums.com/";
            HyperlinkButton learnMoreButton = new LearnMoreButton(learnMoreUrl);
            
            addModifyInfo();                        
            
            add(shareP2PdownloadedFilesCheckBox);
            add(new LearnMoreButton("http://www.gnutellaforums.com/"), "gapleft 15, wrap");
            
            unsafeMessageLabel = new JLabel(I18n.tr("Configure unsafe sharing settings"));
            add(unsafeMessageLabel);
            add(learnMoreButton, "gapleft 15");
            add(configureButton, "gapleft 15");
        }
        
        private void addModifyInfo() {            
            JPanel modifyInfoPanel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
            modifyInfoPanel.setOpaque(false);
                       
            JLabel myFiles = new JLabel(I18n.tr("My Files"), sharingMyFilesIcon, JLabel.CENTER);
            myFiles.setVerticalTextPosition(JLabel.BOTTOM);
            myFiles.setHorizontalTextPosition(JLabel.CENTER);

            modifyInfoPanel.add(new JLabel(I18n.tr("To see or modify files in your Public Shared list, go to:")), 
                    "gapbottom 10, wrap");
            
            modifyInfoPanel.add(myFiles);
            modifyInfoPanel.add(new JLabel(sharingArrowIcon), "aligny top, gaptop 17");
            
            
            modifyInfoPanel.add(new JLabel(I18n.tr("Public Shared"), p2pSharedListIcon, JLabel.RIGHT), "aligny top, gaptop 15");

            add(modifyInfoPanel, "wrap");
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.setValue(shareP2PdownloadedFilesCheckBox.isSelected());
            SharingSettings.ALLOW_PARTIAL_SHARING.setValue(shareP2PdownloadedFilesCheckBox.isSelected());
            return unsafeTypeOptionPanel.applyOptions();
        }

        @Override
        boolean hasChanged() {
            return SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue() != shareP2PdownloadedFilesCheckBox.isSelected()
                || SharingSettings.ALLOW_PARTIAL_SHARING.getValue() != shareP2PdownloadedFilesCheckBox.isSelected()
                || unsafeTypeOptionPanel.hasChanged();
        }

        @Override
        public void initOptions() {
            unsafeTypeOptionPanel.initOptions();
            
            shareP2PdownloadedFilesCheckBox.setSelected(
                    SharingSettings.SHARE_DOWNLOADED_FILES_IN_NON_SHARED_DIRECTORIES.getValue());
        }
    }

}
