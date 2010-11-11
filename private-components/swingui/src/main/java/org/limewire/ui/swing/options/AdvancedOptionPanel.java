package org.limewire.ui.swing.options;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Advanced Option View.
 */
public class AdvancedOptionPanel extends OptionPanel {

    public static final int MULTI_LINE_LABEL_WIDTH = 440;

    private static final String SYSTEM = I18n.tr("System");
    private static final String LISTENING_PORTS = I18n.tr("Listening Ports");
    private static final String PROXY = I18n.tr("Proxy");
    private static final String NETWORK_INTERFACE = I18n.tr("Network Interface");
    private static final String PERFORMANCE = I18n.tr("Performance");
    private static final String FILTERING = I18n.tr("Filtering");
    private static final String EXTERNAL_ACCESS = I18n.tr("External Access");

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JList list;
    private final Map<String, Provider<? extends OptionPanel>> providers = new HashMap<String, Provider<? extends OptionPanel>>();
    private final Map<String, OptionPanel> panels = new HashMap<String, OptionPanel>();

    @Inject
    public AdvancedOptionPanel(Provider<ListeningPortsOptionPanel> firewallOptionPanel,
            Provider<ProxyOptionPanel> proxyOptionPanel,
            Provider<NetworkInterfaceOptionPanel> networkInterfaceOptionPanel,
            Provider<PerformanceOptionPanel> performanceOptionPanel,
            Provider<FilteringOptionPanel> filteringOptionPanel,
            Provider<SystemOptionPanel> systemOptionPanel,
            Provider<ExternalAccessOptionPanel> externalAccessOptionPanel) {

        providers.put(SYSTEM, systemOptionPanel);
        providers.put(LISTENING_PORTS, firewallOptionPanel);
        providers.put(PROXY, proxyOptionPanel);
        providers.put(NETWORK_INTERFACE, networkInterfaceOptionPanel);
        providers.put(PERFORMANCE, performanceOptionPanel);
        providers.put(FILTERING, filteringOptionPanel);
        providers.put(EXTERNAL_ACCESS, externalAccessOptionPanel);

        setLayout(new MigLayout("insets 15, fill, gapy 10", "[][grow]", "[][]"));

        list = new JList();
        list.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setPreferredSize(new Dimension(150, 500));

        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setOpaque(false);

        cardPanel.setLayout(cardLayout);
        createPanel(SYSTEM);
        createList();

        add(
                new JLabel(
                        I18n
                                .tr("We recommend you don't touch these unless you really know what you're doing.")),
                "span 2, wrap");
        add(list, "growy");
        add(cardPanel, "grow");

        list.setSelectedIndex(0);
    }
    
    @Inject
    void register() {
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String name = (String) list.getModel().getElementAt(list.getSelectedIndex());
                if (!panels.containsKey(name)) {
                    createPanel(name);
                }
                cardLayout.show(cardPanel, name);
            }
        });
    }

    private void createPanel(String id) {
        panels.put(id, providers.get(id).get());
        cardPanel.add(panels.get(id), id);
        panels.get(id).initOptions();
    }

    private void createList() {
        DefaultListModel model = new DefaultListModel();
        model.addElement(SYSTEM);
        model.addElement(LISTENING_PORTS);
        model.addElement(PROXY);
        model.addElement(NETWORK_INTERFACE);
        model.addElement(PERFORMANCE);
        model.addElement(FILTERING);
        model.addElement(EXTERNAL_ACCESS);

        list.setModel(model);
    }

    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        boolean done = false;
        Iterator<OptionPanel> it = panels.values().iterator();
        while (it.hasNext() && !done) {
            OptionPanel panel = it.next();
            if (result == null)
                result = panel.applyOptions();
            else
                result.applyResult(panel.applyOptions());
            done = !result.isSuccessful();
        }
        
        return result;
    }

    @Override
    boolean hasChanged() {
        for (OptionPanel panel : panels.values()) {
            if (panel.hasChanged())
                return true;
        }
        return false;
    }

    @Override
    public void initOptions() {
        for (OptionPanel panel : panels.values()) {
            panel.initOptions();
        }
    }
}
