package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * External Access view for Advanced Options.
 */
class ExternalAccessOptionPanel extends OptionPanel {

    private MultiLineLabel localAccessLabel;
    private JCheckBox localAccessCheckBox;

    /**
     * Constructs an ExternalAccessOptionPanel.
     */
    @Inject
    public ExternalAccessOptionPanel() {
        setLayout(new MigLayout("hidemode 3, insets 15, fillx, wrap"));
        setOpaque(false);
        
        add(getAccessPanel(), "pushx, growx");
    }
    
    /**
     * Returns the container for access options.
     */
    private JPanel getAccessPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setLayout(new MigLayout("fillx"));
        panel.setOpaque(false);
        
        localAccessLabel = new MultiLineLabel();
        localAccessLabel.setMaxLineSpan(AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH);
        localAccessLabel.setText(I18n.tr("LimeWire can allow other processes to make use of its services, like search and download, via a REST interface."));
        
        localAccessCheckBox = new JCheckBox(I18n.tr("Enable local access to REST service"));
        localAccessCheckBox.setOpaque(false);
        
        panel.add(localAccessLabel, "growx, wrap");
        panel.add(localAccessCheckBox, "gapleft 25, wrap");
        
        return panel;
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.setValue(localAccessCheckBox.isSelected());
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        return localAccessCheckBox.isSelected() != ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue();
    }

    @Override
    public void initOptions() {
        localAccessCheckBox.setSelected(ApplicationSettings.LOCAL_REST_ACCESS_ENABLED.getValue());
    }
}
