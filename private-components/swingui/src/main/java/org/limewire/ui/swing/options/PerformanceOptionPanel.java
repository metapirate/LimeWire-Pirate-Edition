package org.limewire.ui.swing.options;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.network.NetworkManager;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Performance Option View.
 */
public class PerformanceOptionPanel extends OptionPanel {

    private final NetworkManager networkManager;
    private final GnutellaConnectionManager connectionManager;
    private final String firstMultiLineLabel = I18n.tr("If your computer has a fast internet connection, LimeWire may act as an \"Ultrapeer\" or connect to the Mojito DHT. You may disable these if you notice performance issues.");
    private final String secondMultiLineLabel = I18n.tr("LimeWire uses a secure communications mode called TLS, which may use more CPU resources.");
    private final String thirdMultiLineLabel = I18n.tr("Out-of-band Searching helps deliver faster search results to you, but some internet connections may not work well with this feature.");
    
    private JCheckBox disableUltraPeerCheckBox;
    private JCheckBox disableMojitoCheckBox;
    private JCheckBox disableTLS;
    private JCheckBox disableOutOfBandSearchCheckBox;
    
    @Inject
    public PerformanceOptionPanel(NetworkManager networkManager, GnutellaConnectionManager connectionManager) {
        this.networkManager = networkManager;
        this.connectionManager = connectionManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        setOpaque(false);
        
        add(getPerformancePanel(), "pushx, growx");
    }
    
    private JPanel getPerformancePanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("fillx"));
        p.setOpaque(false);
        
        disableUltraPeerCheckBox = new JCheckBox(I18n.tr("Disable Ultrapeer capabilities"));
        disableMojitoCheckBox = new JCheckBox(I18n.tr("Disable connecting to the Mojito DHT"));
        disableTLS = new JCheckBox(I18n.tr("Disable TLS capabilities"));
        disableOutOfBandSearchCheckBox = new JCheckBox(I18n.tr("Disable Out-of-band searching"));

        disableUltraPeerCheckBox.setOpaque(false);
        disableMojitoCheckBox.setOpaque(false);
        disableTLS.setOpaque(false);
        disableOutOfBandSearchCheckBox.setOpaque(false);
        
        p.add(new MultiLineLabel(firstMultiLineLabel, AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "span, growx, wrap");
        
        p.add(disableUltraPeerCheckBox, "gapleft 25, split, wrap");
        
        p.add(disableMojitoCheckBox, "gapleft 25, split, wrap");
        
        p.add(new MultiLineLabel(secondMultiLineLabel, AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "span, growx, gaptop 18, wrap");
        
        p.add(disableTLS, "gapleft 25, split, wrap");
        
        p.add(new MultiLineLabel(thirdMultiLineLabel, AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "span, growx, gaptop 18, wrap");
        
        p.add(disableOutOfBandSearchCheckBox, "gapleft 25, split, wrap");
        
        return p;
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        
        boolean upChanged = UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue() != disableUltraPeerCheckBox.isSelected();
        boolean tlsServerChanged = disableTLS.isSelected() != !networkManager.isIncomingTLSEnabled();
        boolean isSupernode = connectionManager.isUltrapeer();
        
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(disableUltraPeerCheckBox.isSelected());
        DHTSettings.DISABLE_DHT_USER.setValue(disableMojitoCheckBox.isSelected());
        
        networkManager.setIncomingTLSEnabled(!disableTLS.isSelected());
        networkManager.setOutgoingTLSEnabled(!disableTLS.isSelected());
        
        SearchSettings.OOB_ENABLED.setValue(!disableOutOfBandSearchCheckBox.isSelected());
        
        if((tlsServerChanged || (upChanged && disableUltraPeerCheckBox.isSelected()) && isSupernode)) {
            if(tlsServerChanged && !disableTLS.isSelected()) {
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        networkManager.validateTLS();
                        connectionManager.restart();
                    }
                });
            }
        }
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        return UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue() != disableUltraPeerCheckBox.isSelected() 
        || DHTSettings.DISABLE_DHT_USER.getValue() != disableMojitoCheckBox.isSelected()
        || (!networkManager.isIncomingTLSEnabled() && !networkManager.isOutgoingTLSEnabled()) != disableTLS.isSelected()
        || SearchSettings.OOB_ENABLED.getValue() == disableOutOfBandSearchCheckBox.isSelected();
    }

    @Override
    public void initOptions() {
        disableUltraPeerCheckBox.setSelected(UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue());
        disableMojitoCheckBox.setSelected(DHTSettings.DISABLE_DHT_USER.getValue());
        disableTLS.setSelected(!networkManager.isIncomingTLSEnabled() || !networkManager.isOutgoingTLSEnabled());
        disableOutOfBandSearchCheckBox.setSelected(!SearchSettings.OOB_ENABLED.getValue());
    }

}