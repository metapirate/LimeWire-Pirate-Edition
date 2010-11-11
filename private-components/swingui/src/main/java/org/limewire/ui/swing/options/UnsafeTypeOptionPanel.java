package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Application;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.Setting;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.options.OptionPanelStateManager.SettingChangedListener;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class UnsafeTypeOptionPanel extends OptionPanel {

    private JCheckBox programCheckBox;
    private JCheckBox documentCheckBox;
    private JButton okButton;

    private final LibraryManager libraryManager;
    private final SharedFileListManager shareListManager;
    private final OptionPanelStateManager manager;
   
    private final Map<Setting, JCheckBox> settingMap;
    
    @Inject
    public UnsafeTypeOptionPanel(LibraryManager libraryManager,
            SharedFileListManager shareListManager,
            UnsafeTypeOptionPanelStateManager manager,
            final Provider<ExtensionClassificationPanel> extensionClassificationPanelProvider,
            Application application) {
        
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.manager = manager;

        setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel(new MigLayout("nogrid"));
        contentPanel.setOpaque(false);
        
        programCheckBox = new JCheckBox(I18n.tr("Allow me to search for and share Programs with anyone"));
        programCheckBox.setContentAreaFilled(false);
        documentCheckBox = new JCheckBox(I18n.tr("Allow me to share Documents with the world"));
        documentCheckBox.setContentAreaFilled(false);
        okButton = new JButton(new OKDialogAction());
    
        settingMap = new HashMap<Setting, JCheckBox>();
        settingMap.put(LibrarySettings.ALLOW_PROGRAMS, programCheckBox);
        settingMap.put(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING, documentCheckBox);
        
        contentPanel.add(new JLabel("<html>" + I18n.tr("Enabling this setting makes you more prone " +
        		"to accidently sharing personal information")
                + "</html>"), "wrap");
        contentPanel.add(documentCheckBox, "gapleft 25");
        contentPanel.add(new HyperlinkButton(new AbstractAction(I18n.tr("What are Documents?")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExtensionClassificationPanel panel = extensionClassificationPanelProvider.get();
                panel.switchCategory(Category.DOCUMENT);
                panel.showDialogue();
            }
        }), "wrap");
        
        contentPanel.add(new JSeparator(), "growx, gaptop 5, gapbottom 5, wrap");
        
        contentPanel.add(new JLabel("<html>" + I18n.tr("Enabling this setting makes you more prone to viruses")
        + "</html>"), "wrap");
        contentPanel.add(programCheckBox, "gapleft 25");
        contentPanel.add(new HyperlinkButton(new AbstractAction(I18n.tr("What are Programs?")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExtensionClassificationPanel panel = extensionClassificationPanelProvider.get();
                panel.switchCategory(Category.PROGRAM);
                panel.showDialogue();
            }
        }), "wrap");
        
        add(contentPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new MigLayout("fill"));
        buttonPanel.setOpaque(false);
        
        buttonPanel.add(okButton, "tag ok");
        add(buttonPanel, BorderLayout.SOUTH);
        
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePendingSettings();
            }
        });
    }
    
    @Inject
    public void register() {
        manager.addSettingChangedListener(new SettingChangedListener() {
            @Override
            public void settingChanged(Setting setting) {
                settingMap.get(setting).setSelected((Boolean)UnsafeTypeOptionPanel.this.manager.getValue(setting));
            }
        });
    }
        
    @Override
    ApplyOptionResult applyOptions() {
        manager.saveSettings();

        if(!programCheckBox.isSelected()) {
            libraryManager.getLibraryManagedList().removeFiles(new Predicate<LocalFileItem>() {
               @Override
                public boolean apply(LocalFileItem localFileItem) {
                    return localFileItem.getCategory() == Category.PROGRAM;
                } 
            });
        }
        
        if (!documentCheckBox.isSelected()) {
            shareListManager.removeDocumentsFromPublicLists();
        }
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        return manager.hasPendingChanges();
    }

    @Override
    public void initOptions() {
        for ( Setting setting : settingMap.keySet() ) {
            settingMap.get(setting).setSelected((Boolean)manager.getValue(setting));
        }
    }
    
    private void savePendingSettings() {
        for ( Setting setting : settingMap.keySet() ) {
            manager.setValue(setting, settingMap.get(setting).isSelected());
        }
    }
}
