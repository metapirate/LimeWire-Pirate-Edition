package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.daap.DaapManager;
import org.limewire.core.settings.DaapSettings;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ITunesOptionPanel extends OptionPanel {

    private final JCheckBox addToITunesCheckBox;
    private final DaapManager daapManager;
    
    private OptionPanel iTunesSharePanel;
    
    @Inject
    public ITunesOptionPanel(DaapManager daapManager) {
        
        this.daapManager = daapManager;
        
        setLayout(new MigLayout("gapy 10"));
        
        addToITunesCheckBox = new JCheckBox(I18n.tr("Add audio files I downloaded from LimeWire to iTunes"));
        addToITunesCheckBox.setContentAreaFilled(false);
             
        add(addToITunesCheckBox, "wrap");
        add(getITunesSharePanel(), "wrap");
        
        add(new JButton(new OKDialogAction()), "tag ok, gapleft push");
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(addToITunesCheckBox.isSelected());;
        
        return getITunesSharePanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue() != addToITunesCheckBox.isSelected() 
            || getITunesSharePanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getITunesSharePanel().initOptions();
        addToITunesCheckBox.setSelected(iTunesSettings.ITUNES_SUPPORT_ENABLED.getValue());
    }

    private OptionPanel getITunesSharePanel() {
        if(iTunesSharePanel == null) {
            iTunesSharePanel = new ITunesSharePanel();
        }
        
        return iTunesSharePanel;
    }
    
    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getITunesSharePanel().setOptionTabItem(tab);
    }
    
    private class ITunesSharePanel extends OptionPanel {

        private JCheckBox shareWithITunesCheckBox;
        private JCheckBox requirePassWordCheckBox;
        private JPasswordField passwordField;
        
        public ITunesSharePanel() {
            
            setLayout(new MigLayout("insets 0, gap 0"));
            
            setOpaque(false);
            
            shareWithITunesCheckBox = new JCheckBox(I18n.tr("Share audio and video files in Public Shared list on my local network with iTunes"));
            shareWithITunesCheckBox.setContentAreaFilled(false);
            shareWithITunesCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setPasswordVisible(shareWithITunesCheckBox.isSelected());
                }
            });
            requirePassWordCheckBox = new JCheckBox(I18n.tr("Require password"));
            requirePassWordCheckBox.setContentAreaFilled(false);
            requirePassWordCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    passwordField.setEnabled(requirePassWordCheckBox.isSelected());
                }
            });
            passwordField = new JPasswordField(30);
            passwordField.setEnabled(false);
            
            add(shareWithITunesCheckBox, "split, wrap");
            
            add(requirePassWordCheckBox, "gapleft 25, split");
            add(passwordField);
            
            setPasswordVisible(false);
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            if(hasChanged()) {
                final boolean prevEnabled = DaapSettings.DAAP_ENABLED.getValue();
               
                final boolean prevRequiresPassword = DaapSettings.DAAP_REQUIRES_PASSWORD.getValue();
                final String prevPassword = DaapSettings.DAAP_PASSWORD.get();
                
                final boolean requiresPassword = requirePassWordCheckBox.isSelected();
                String password = new String(passwordField.getPassword());
                
                if (password.equals("") && requiresPassword) { 
                    FocusJOptionPane.showMessageDialog(ITunesOptionPanel.this, 
                            I18n.tr("Daap Password cannot be null, iTunes settings not saved"),
                            I18n.tr("iTunes Error"),
                            JOptionPane.ERROR_MESSAGE);
                    
                    initOptions();
                    return new ApplyOptionResult(false,true);
                }
                
                //enable daap setting
                DaapSettings.DAAP_ENABLED.setValue(shareWithITunesCheckBox.isSelected());
                
                //save password value
                if (!DaapSettings.DAAP_PASSWORD.equals(password)) {
                    DaapSettings.DAAP_PASSWORD.set(password);
                }           
      
                try {               
                    if (requiresPassword != prevRequiresPassword || (requiresPassword && !password.equals(prevPassword))) {
                        DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(requiresPassword);
        
                        // A password is required now or password has changed, 
                        // disconnect all users...
                        if (requiresPassword) { 
                            daapManager.disconnectAll();
                        }
                        daapManager.updateService();
        
                    }
                    
                    if (shareWithITunesCheckBox.isSelected()) {              
                        if (prevEnabled) { 
                            daapManager.restart();
                        } else if(!prevEnabled) {
                            daapManager.start();
                        }
                    } else if (prevEnabled) {
                        daapManager.stop();
                    }
                    
                } catch (IOException err) {               
                    DaapSettings.DAAP_ENABLED.setValue(prevEnabled);
                    DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(prevRequiresPassword);
                    DaapSettings.DAAP_PASSWORD.set(prevPassword);
    
                    daapManager.stop();
                    initOptions();
    
                    FocusJOptionPane.showMessageDialog(ITunesOptionPanel.this, 
                            I18n.tr("Could not restart the Daap connection"),
                            I18n.tr("Daap Error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return  DaapSettings.DAAP_ENABLED.getValue() != shareWithITunesCheckBox.isSelected() ||
                    DaapSettings.DAAP_REQUIRES_PASSWORD.getValue() != requirePassWordCheckBox.isSelected() ||
                    (requirePassWordCheckBox.isSelected() && DaapSettings.DAAP_PASSWORD.get() != requirePassWordCheckBox.getText());
        }

        @Override
        public void initOptions() {
            shareWithITunesCheckBox.setSelected(DaapSettings.DAAP_ENABLED.getValue());

            requirePassWordCheckBox.setSelected(DaapSettings.DAAP_REQUIRES_PASSWORD.getValue());
            if(requirePassWordCheckBox.isSelected()) {
                passwordField.setText(DaapSettings.DAAP_PASSWORD.get());
            }
            
            setPasswordVisible(shareWithITunesCheckBox.isSelected());
        }
        
        private void setPasswordVisible(boolean value) {
            requirePassWordCheckBox.setVisible(value);
            passwordField.setVisible(value);
        }
    }
}
