package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Network Interface Option View.
 */
public class NetworkInterfaceOptionPanel extends OptionPanel {

    private ButtonGroup buttonGroup;
    
    private JRadioButton limewireChooseRadioButton;
    private JRadioButton meChooseRadioButton;
    private NetworkTable table;
    private JScrollPane scrollPane;
    
    private NetworkItem selectedItem;
    
    private EventList<NetworkItem> eventList;
    
    private final Provider<TorrentManager> torrentManager;
    private final TorrentManagerSettings torrentSettings;
    
    @Inject
    public NetworkInterfaceOptionPanel(Provider<TorrentManager> torrentManager, @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;
        
        setLayout(new MigLayout("insets 15, fillx, wrap"));
        setOpaque(false);
        
        add(getNetworkPanel(), "pushx, growx");
    }
    
    private JPanel getNetworkPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("fillx, gapy 10"));
        p.setOpaque(false);
        
        limewireChooseRadioButton = new JRadioButton(I18n.tr("Let LimeWire choose my network interface (Recommended)"));
        meChooseRadioButton = new JRadioButton(I18n.tr("Let me choose a specific network interface"));
        
        limewireChooseRadioButton.setOpaque(false);
        meChooseRadioButton.setOpaque(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireChooseRadioButton);
        buttonGroup.add(meChooseRadioButton);
        
        eventList = GlazedLists.threadSafeList(new BasicEventList<NetworkItem>());
        table = new NetworkTable(new DefaultEventTableModel<NetworkItem>(eventList, new NetworkTableFormat()));
        scrollPane = new JScrollPane(table);
        scrollPane.setVisible(false);
        
        meChooseRadioButton.addItemListener(new RadioListener(scrollPane, meChooseRadioButton));

        p.add(limewireChooseRadioButton, "wrap");
        
        p.add(meChooseRadioButton, "wrap");
        
        p.add(scrollPane, "grow, span");
        
        return p;
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        boolean customBefore = ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue();
        boolean customNow = meChooseRadioButton.isSelected();
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(customNow);
        String addressBefore = ConnectionSettings.CUSTOM_INETADRESS.get();
        String addressNow = null;
        for(NetworkItem item : eventList) {
            if(item.isSelected()) {
                addressNow = item.getAddress();
                ConnectionSettings.CUSTOM_INETADRESS.set(addressNow);
                break;
            }
        }
        boolean restart = customBefore != customNow || !addressBefore.equals(addressNow);
        
        if(torrentManager.get().isInitialized() && torrentManager.get().isValid()) {
            BackgroundExecutorService.execute(new Runnable() {
               @Override
                public void run() {
                   torrentManager.get().setTorrentManagerSettings(torrentSettings);
                } 
            });
        }
        return new ApplyOptionResult(restart, true);
    }

    @Override
    boolean hasChanged() {
        if(!ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue())
            return !limewireChooseRadioButton.isSelected();
        
        String expect = ConnectionSettings.CUSTOM_INETADRESS.get();
        for(NetworkItem item : eventList) {
            if(expect.equals(item.getAddress()));
                return false;
        }
        return true;
    }

    @Override
    public void initOptions() {
        eventList.clear();
        limewireChooseRadioButton.setSelected(!ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue());
        meChooseRadioButton.setSelected(ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue());
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            // Add the available interfaces / addresses
            while(interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if(address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isLoopbackAddress())
                        continue;
                    NetworkItem networkItem = new NetworkItem(address, ni.getDisplayName());
                    if(ConnectionSettings.CUSTOM_INETADRESS.get().equals(address.getHostAddress())) {
                        networkItem.setSelected(true);
                        selectedItem = networkItem;
                    }
                    eventList.add(networkItem);
                }
            }
            if(selectedItem == null && eventList.size() > 0) { 
                eventList.get(0).setSelected(true); 
                selectedItem = eventList.get(0);
            }
        } catch (SocketException e) {
            meChooseRadioButton.setEnabled(false);
        }
        if(eventList.size() == 0 )
            meChooseRadioButton.setEnabled(false);
    }
    
    private static class RadioListener implements ItemListener {

        private JScrollPane scrollPane;
        private JRadioButton radioButton;
        
        public RadioListener(JScrollPane scrollPane, JRadioButton button) {
            this.scrollPane = scrollPane;
            this.radioButton = button;
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            scrollPane.setVisible(radioButton.isSelected());
        }
    }
    
    private class NetworkTable extends JXTable {
        
        public NetworkTable(DefaultEventTableModel<NetworkItem> model) {
            super(model);
            setShowGrid(false, false);
            setColumnSelectionAllowed(false);
            getColumn(0).setCellRenderer(new CheckBoxRenderer());
            getColumn(0).setCellEditor(new CheckBoxRenderer());
            getColumn(0).setMaxWidth(30);
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
                return false;
            }
            return getColumnModel().getColumn(col).getCellEditor() != null;
        }
    }
    
    private static class NetworkTableFormat extends AbstractTableFormat<NetworkItem> {

        private static final int CHECK_INDEX = 0;
        private static final int ADDRESS_INDEX = 1;
        private static final int NAME_INDEX = 2;
        
        public NetworkTableFormat() {
            super("", I18n.tr("Address"), I18n.tr("Name"));
        }

        @Override
        public Object getColumnValue(NetworkItem baseObject, int column) {
            switch(column) {
                case CHECK_INDEX: return baseObject;
                case ADDRESS_INDEX: return baseObject.getAddress();
                case NAME_INDEX: return baseObject.getName();
            }
                
            throw new IllegalStateException("Unknown column:" + column);
        }   
    }
    
    private static class NetworkItem {       
        private boolean isSelected = false;
        private InetAddress address;
        private String displayName;
        
        public NetworkItem(InetAddress address, String displayName) {
            this.address = address;
            this.displayName = displayName;
        }
        
        public String getAddress() {
            return address.getHostAddress();
        }
        
        public String getName() {
            return displayName;
        }
        
        public boolean isSelected() {
            return isSelected;
        }
        
        public void setSelected(boolean value) {
            this.isSelected = value;
        }
    }
    
    private class CheckBoxRenderer extends JRadioButton implements TableCellRenderer, TableCellEditor {

        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        
        public CheckBoxRenderer() {
            addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetworkItem currentItem = eventList.get(table.getSelectedRow());
                    if(!currentItem.getAddress().equals(selectedItem.getAddress())) {
                        selectedItem.setSelected(false);
                        selectedItem = currentItem;
                    }
                    currentItem.setSelected(true);
                    cancelCellEditing();
                    table.repaint();
                }
            });
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            if(value instanceof NetworkItem) {
                NetworkItem item = (NetworkItem) value;
                this.setSelected(item.isSelected);
            } else {
                this.setSelected(false);
            }
            
            if(isSelected) 
                setBackground(table.getSelectionBackground());
            else    
                setBackground(table.getBackground());
            
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellRendererComponent(table, value, true, true, row, column);
        }

        @Override
        public void cancelCellEditing() {
            synchronized (listeners) {
                for (int i=0, N=listeners.size(); i<N; i++) {
                    listeners.get(i).editingCanceled(new ChangeEvent(this));
                }
            }
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (listeners.contains(lis)) listeners.remove(lis);
            }
        }
        
        @Override
        public void addCellEditorListener(CellEditorListener lis) {
            synchronized (listeners) {
                if (!listeners.contains(lis)) listeners.add(lis);
            }
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }
    }
}
