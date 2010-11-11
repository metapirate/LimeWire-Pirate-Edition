package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.api.network.NetworkManager;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.ui.swing.components.EmbeddedComponentLabel;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Listening Ports Option Panel.
 */
public class ListeningPortsOptionPanel extends OptionPanel {

    private final NetworkManager networkManager;
    private final Provider<TorrentManager> torrentManager;
    private final TorrentListeningPortsOptionPanel torrentListeningPorts;
    private final GnutellaListeningPortsOptionPanel gnutellaListeningPorts;
    private final @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings;

    @Inject
    public ListeningPortsOptionPanel(NetworkManager networkManager,
            Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        setLayout(new MigLayout("insets 15, fillx"));
        setOpaque(false);
        
        this.networkManager = networkManager;
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;

        gnutellaListeningPorts = new GnutellaListeningPortsOptionPanel();
        torrentListeningPorts = new TorrentListeningPortsOptionPanel();
        
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("fillx"));
        p.setOpaque(false);
        
        p.add(new MultiLineLabel(I18n.tr("You can set the local network port that listens for incoming connections. This port may be changed in case of conflict with another program or if a specific port number is required for direct incoming connections by your firewall. You must also configure your router when choosing \"manual port forward\" or \"do nothing\""),
                        AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "pad 0, growx, wrap");

        p.add(gnutellaListeningPorts, "gaptop 10, growx, wrap");
        p.add(torrentListeningPorts, "gaptop 10, growx, wrap");
        
        add(p, "pushx, growx");
    }
    
    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getTorrentListeningPorts().setOptionTabItem(tab);
        getGnutellaListeningPorts().setOptionTabItem(tab);
    }
    

    private TorrentListeningPortsOptionPanel getTorrentListeningPorts() {
        return torrentListeningPorts;
    }
    



    private GnutellaListeningPortsOptionPanel getGnutellaListeningPorts() {
        return gnutellaListeningPorts;
    }
    



    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        
        result = gnutellaListeningPorts.applyOptions();
        if (result.isSuccessful())
            result.applyResult(torrentListeningPorts.applyOptions());
        
        return result;
    }

    @Override
    boolean hasChanged() {
        return gnutellaListeningPorts.hasChanged() || torrentListeningPorts.hasChanged();
    }

    @Override
    public void initOptions() {
        gnutellaListeningPorts.initOptions();
        torrentListeningPorts.initOptions();
    }

    private class GnutellaListeningPortsOptionPanel extends OptionPanel {
        private final JRadioButton gnutellaPlugAndPlayRadioButton;
        private final JRadioButton gnutellaPortForwardRadioButton;
        private final NumericTextField gnutellaForcePortTextField;
        private final JLabel gnutellaManualConfigurationWarning;
        private final JRadioButton gnutellaDoNothingRadioButton;
        private final NumericTextField gnutellaPortField;
        private int gnutellaPort;

        public GnutellaListeningPortsOptionPanel() {
            setLayout(new MigLayout("insets 0, nogrid"));
            setOpaque(false);
            
            add(new JLabel(I18n.tr("Gnutella port:")), "split");

            gnutellaPortField = new NumericTextField(5, 1, 0xFFFF);
            add(gnutellaPortField, "wrap");

            gnutellaPlugAndPlayRadioButton = new JRadioButton(I18n
                    .tr("Use Universal Plug n' Play (Recommended)"));
            gnutellaPortForwardRadioButton = new JRadioButton(I18n.tr("Manual Port Forward:"));
            gnutellaDoNothingRadioButton = new JRadioButton(I18n.tr("Do Nothing"));

            gnutellaPlugAndPlayRadioButton.setOpaque(false);
            gnutellaPortForwardRadioButton.setOpaque(false);
            gnutellaDoNothingRadioButton.setOpaque(false);

            ButtonGroup gnutellaButtonGroup = new ButtonGroup();
            gnutellaButtonGroup.add(gnutellaPlugAndPlayRadioButton);
            gnutellaButtonGroup.add(gnutellaPortForwardRadioButton);
            gnutellaButtonGroup.add(gnutellaDoNothingRadioButton);

            add(gnutellaPlugAndPlayRadioButton, "split, wrap");
            add(gnutellaPortForwardRadioButton, "split");

            gnutellaForcePortTextField = new NumericTextField(5, 1, 0xFFFF);
            gnutellaManualConfigurationWarning = new JLabel(I18n
                    .tr("* You must also configure your router"));
            gnutellaManualConfigurationWarning.setVisible(false);

            add(gnutellaForcePortTextField, "wrap");
            add(gnutellaManualConfigurationWarning, "gapleft 85, wrap, hidemode 2");
            add(gnutellaDoNothingRadioButton, "split, wrap");

            gnutellaPortForwardRadioButton.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    updateTextField();
                }
            });
        }

        @Override
        ApplyOptionResult applyOptions() {
            int newGnutellaPort = gnutellaPortField.getValue(gnutellaPort);
            if (newGnutellaPort != gnutellaPort) {
                try {
                    NetworkSettings.PORT.setValue(newGnutellaPort);
                    networkManager.setListeningPort(newGnutellaPort);
                    gnutellaPort = newGnutellaPort;
                    networkManager.portChanged();
                } catch (IOException ioe) {
                    FocusJOptionPane.showMessageDialog(ListeningPortsOptionPanel.this, I18n.tr(
                            "The port chosen {0}, is already in use.", newGnutellaPort), I18n
                            .tr("Network Port Error"), JOptionPane.ERROR_MESSAGE);
                    NetworkSettings.PORT.setValue(gnutellaPort);
                    gnutellaPortField.setValue(gnutellaPort);
                }
            }

            boolean restart = false;
            boolean oldUPNP = ConnectionSettings.DISABLE_UPNP.getValue();
            final int oldForcedPort = ConnectionSettings.FORCED_PORT.getValue();
            final boolean oldForce = ConnectionSettings.FORCE_IP_ADDRESS.getValue();

            if (gnutellaPlugAndPlayRadioButton.isSelected()) {
                if (!ConnectionSettings.UPNP_IN_USE.getValue()) {
                    ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
                }
                ConnectionSettings.DISABLE_UPNP.setValue(false);
                if (oldUPNP || oldForce) {
                    restart = true;
                }
            } else if (gnutellaDoNothingRadioButton.isSelected()) {
                ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
                ConnectionSettings.DISABLE_UPNP.setValue(true);
            } else { // PORT.isSelected()
                int gnutellaForcedPort = gnutellaForcePortTextField.getValue(oldForcedPort);

                ConnectionSettings.DISABLE_UPNP.setValue(false);
                ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
                ConnectionSettings.UPNP_IN_USE.setValue(false);
                ConnectionSettings.FORCED_PORT.setValue(gnutellaForcedPort);
            }

            // Notify that the address changed if:
            // 1) The 'forced address' status changed.
            // or 2) We're forcing and the ports are different.
            final boolean newForce = ConnectionSettings.FORCE_IP_ADDRESS.getValue();
            final int newForcedPort = ConnectionSettings.FORCED_PORT.getValue();
            
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (oldForce != newForce) {
                        networkManager.addressChanged();
                    }
                    if (newForce && (oldForcedPort != newForcedPort)) {
                        networkManager.portChanged();
                    }
                }
            });

            return new ApplyOptionResult(restart, true);
        }

        @Override
        boolean hasChanged() {
            if (ConnectionSettings.FORCE_IP_ADDRESS.getValue()
                    && !ConnectionSettings.UPNP_IN_USE.getValue()) {
                if (!gnutellaPortForwardRadioButton.isSelected())
                    return true;
            } else if (ConnectionSettings.DISABLE_UPNP.getValue()) {
                if (!gnutellaDoNothingRadioButton.isSelected())
                    return true;
            } else {
                if (!gnutellaPlugAndPlayRadioButton.isSelected())
                    return true;
            }
            int forcedPortSetting = ConnectionSettings.FORCED_PORT.getValue();
            int portSetting = NetworkSettings.PORT.getValue();

            return portSetting != gnutellaPortField.getValue(portSetting)
                    || (gnutellaPortForwardRadioButton.isSelected() && gnutellaForcePortTextField
                            .getValue(forcedPortSetting) != forcedPortSetting);
        }

        @Override
        public void initOptions() {
            gnutellaPort = NetworkSettings.PORT.getValue();
            gnutellaPortField.setValue(gnutellaPort);

            if (ConnectionSettings.FORCE_IP_ADDRESS.getValue()
                    && !ConnectionSettings.UPNP_IN_USE.getValue()) {
                gnutellaPortForwardRadioButton.setSelected(true);
            } else if (ConnectionSettings.DISABLE_UPNP.getValue()) {
                gnutellaDoNothingRadioButton.setSelected(true);
            } else {
                gnutellaPlugAndPlayRadioButton.setSelected(true);
            }

            gnutellaForcePortTextField.setValue(ConnectionSettings.FORCED_PORT.getValue());

            updateTextField();
        }

        private void updateTextField() {
            gnutellaForcePortTextField.setEnabled(gnutellaPortForwardRadioButton.isSelected());
            gnutellaForcePortTextField.setEditable(gnutellaPortForwardRadioButton.isSelected());
            gnutellaManualConfigurationWarning.setVisible(gnutellaPortForwardRadioButton
                    .isSelected());
        }
    }

    private class TorrentListeningPortsOptionPanel extends OptionPanel {

        private final JRadioButton torrentPlugAndPlayRadioButton;
        private final JRadioButton torrentDoNothingRadioButton;
        private final JComponent torrentPortController;
        private final NumericTextField torrentStartPortField;
        private final NumericTextField torrentEndPortField;

        public TorrentListeningPortsOptionPanel() {
            setLayout(new MigLayout("insets 0, nogrid"));
            setOpaque(false);

            torrentStartPortField = new NumericTextField(5, 1, 0xFFFF);
            torrentEndPortField = new NumericTextField(5, 1, 0xFFFF);

            torrentPortController = new EmbeddedComponentLabel(I18n
                    .tr("BitTorrent ports: {c} to {c}"), torrentStartPortField, torrentEndPortField);
            add(torrentPortController, "wrap");

            torrentPlugAndPlayRadioButton = new JRadioButton(I18n
                    .tr("Use Universal Plug n' Play (Recommended)"));
            torrentDoNothingRadioButton = new JRadioButton(I18n.tr("Do Nothing"));

            torrentPlugAndPlayRadioButton.setOpaque(false);
            torrentDoNothingRadioButton.setOpaque(false);

            ButtonGroup torrentButtonGroup = new ButtonGroup();
            torrentButtonGroup.add(torrentPlugAndPlayRadioButton);
            torrentButtonGroup.add(torrentDoNothingRadioButton);

            add(torrentPlugAndPlayRadioButton, "wrap");
            add(torrentDoNothingRadioButton, "wrap");
        }

        @Override
        ApplyOptionResult applyOptions() {
            int torrentStartPort = torrentStartPortField
                    .getValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue());
            int torrentEndPort = torrentEndPortField
                    .getValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue());

            if (torrentStartPort > torrentEndPort) {
                // swap start and end port if start port is larger.
                int temp = torrentStartPort;
                torrentStartPort = torrentEndPort;
                torrentEndPort = temp;
            }

            BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.setValue(torrentStartPort);
            BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.setValue(torrentEndPort);
            BittorrentSettings.TORRENT_USE_UPNP
                    .setValue(torrentPlugAndPlayRadioButton.isSelected());

            if (torrentManager.get().isInitialized() && torrentManager.get().isValid()) {
                BackgroundExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        torrentManager.get().setTorrentManagerSettings(torrentSettings);
                        if(BittorrentSettings.TORRENT_USE_UPNP.getValue()) {
                            torrentManager.get().startUPnP();
                        } else {
                            torrentManager.get().stopUPnP();
                        }
                    }
                });
            }

            return new ApplyOptionResult(false, true);
        }

        @Override
        boolean hasChanged() {
            return torrentStartPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT
                    .getValue()) != BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue()
                    || torrentEndPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT
                            .getValue()) != BittorrentSettings.LIBTORRENT_LISTEN_END_PORT
                            .getValue()
                    || torrentPlugAndPlayRadioButton.isSelected() != BittorrentSettings.TORRENT_USE_UPNP
                            .getValue();
        }

        @Override
        public void initOptions() {
            torrentStartPortField.setValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT
                    .getValue());
            torrentEndPortField.setValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue());

            torrentPlugAndPlayRadioButton.setSelected(BittorrentSettings.TORRENT_USE_UPNP
                    .getValue());
            torrentDoNothingRadioButton
                    .setSelected(!BittorrentSettings.TORRENT_USE_UPNP.getValue());

        }

    }

}
