package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IP;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Filtering Option View.
 */
public class FilteringOptionPanel extends OptionPanel {
    
    private SpamManager spamManager;
    
    private BlockHostsPanel blockHostPanel;
    private AllowHostsPanel allowHostsPanel;
    
    @Inject
    public FilteringOptionPanel(SpamManager spamManager) {
        super();
        this.spamManager = spamManager;
        
        setLayout(new MigLayout("insets 15, fillx, wrap"));
        setOpaque(false);
        
        add(getBlockHostsPanel(), "pushx, growx");
        add(getAllowHostsPanel(), "pushx, growx");
    }
    
    private OptionPanel getBlockHostsPanel() {
        if(blockHostPanel == null) {
            blockHostPanel = new BlockHostsPanel();
            
        }
        
        return blockHostPanel;
    }
    
    private OptionPanel getAllowHostsPanel() {
        if(allowHostsPanel == null) {
            allowHostsPanel = new AllowHostsPanel();
            
        }
        
        return allowHostsPanel;
    }
    
    @Override
    void setOptionTabItem(OptionTabItem tab) {
        super.setOptionTabItem(tab);
        getAllowHostsPanel().setOptionTabItem(tab);
        getBlockHostsPanel().setOptionTabItem(tab);
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        
        result = getBlockHostsPanel().applyOptions();
        if (result.isSuccessful())
            result.applyResult(getAllowHostsPanel().applyOptions());
       
        return result;
    }
    
    @Override
    boolean hasChanged() {
        return getBlockHostsPanel().hasChanged() || getAllowHostsPanel().hasChanged();
    }
    
    @Override
    public void initOptions() {
        getBlockHostsPanel().initOptions();
        getAllowHostsPanel().initOptions();
    }
    
    private class BlockHostsPanel extends OptionPanel {
    
        private JTextField addressTextField;
        private JButton addButton;
        private FilteringTable filterTable;
        private JCheckBox backListCheckBox;
        private String description = I18n.tr("Use LimeWire's blacklist to protect you from harmful people");
        
        public BlockHostsPanel() {
            super(I18n.tr("Block Hosts"));
            
            setLayout(new MigLayout("gapy 10", "[sg 1][sg 1][]", ""));
            
            addressTextField = new JTextField(26);
            TextFieldClipboardControl.install(addressTextField);
            addButton = new JButton(I18n.tr("Add Address"));
            filterTable = new FilteringTable();
            backListCheckBox = new JCheckBox();
            backListCheckBox.setOpaque(false);
            addButton.addActionListener(new AddAction(addressTextField, filterTable));
            
            add(new JLabel("<html>"+I18n.tr("Block contact with specific people by adding their IP address")+"</html>"), "span, growx, wrap");
            add(addressTextField, "gapright 10");
            add(addButton, "wrap");
            add(new JScrollPane(filterTable), "growx, span 2, wrap");
            add(backListCheckBox, "span, split");
            add(new MultiLineLabel(description, AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "span, growx");
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            List<String> list = filterTable.getFilterModel().getModel();

            FilterSettings.USE_NETWORK_FILTER.setValue(backListCheckBox.isSelected());
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(list.toArray(new String[list.size()]));
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    spamManager.reloadIPFilter();
                }
            });
            return new ApplyOptionResult(false, true);
        }
    
        @Override
        boolean hasChanged() {
            List model = Arrays.asList(FilterSettings.BLACK_LISTED_IP_ADDRESSES.get());
            return backListCheckBox.isSelected() != FilterSettings.USE_NETWORK_FILTER.getValue()
                    || !model.equals(filterTable.getFilterModel().getModel());
        }
    
        @Override
        public void initOptions() {
            String[] bannedIps = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();           
            FilterModel model = new FilterModel(new ArrayList<String>(Arrays.asList(bannedIps)));
            filterTable.setModel(model);
            
            backListCheckBox.setSelected(FilterSettings.USE_NETWORK_FILTER.getValue());
        }
    }
    
    private class AllowHostsPanel extends OptionPanel {
    
        private JTextField addressTextField;
        private JButton addButton;
        private FilteringTable filterTable;
        
        private final String description = I18n.tr("Override the block list and allow specific people by adding their IP address");
        
        public AllowHostsPanel() {
            super(I18n.tr("Allow Hosts"));
            
            setLayout(new MigLayout("gapy 10", "[sg 1][sg 1][]", ""));
            
            addressTextField = new JTextField(26);
            TextFieldClipboardControl.install(addressTextField);
            addButton = new JButton(I18n.tr("Add Address"));
            filterTable = new FilteringTable();
            addButton.addActionListener(new AddAction(addressTextField, filterTable));
            
            add(new MultiLineLabel(description, AdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "span, growx, wrap");
            add(addressTextField, "gapright 10");
            add(addButton, "wrap");
            add(new JScrollPane(filterTable), "growx, span 2");
        }
        
        @Override
        ApplyOptionResult applyOptions() {
            List<String> list = filterTable.getFilterModel().getModel();

            FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(list.toArray(new String[list.size()]));
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    spamManager.reloadIPFilter();
                }
            });
            return new ApplyOptionResult(false, true);
        }
    
        @Override
        boolean hasChanged() {
            List model = Arrays.asList(FilterSettings.WHITE_LISTED_IP_ADDRESSES.get());
            return !model.equals(filterTable.getFilterModel().getModel());
        }
    
        @Override
        public void initOptions() {
            String[] allowedIps = FilterSettings.WHITE_LISTED_IP_ADDRESSES.get();
            FilterModel model = new FilterModel(new ArrayList<String>(Arrays.asList(allowedIps)));
            filterTable.setModel(model);
        }
    }
    
    private class FilteringTable extends JXTable {
        
        private FilterModel model;
        
        public FilteringTable() {   
            setTableHeader(null);
            setShowGrid(false);
            setSelectionMode(0);
            setSelectionBackground(getBackground());
            setSelectionForeground(getForeground());
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups

                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());

                    if (row >= 0 && col >= 0) {
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
                            // force update editor colors
                            prepareEditor(editor, row, col);
                            // editor.repaint() takes about a second to show sometimes
                            repaint();
                        }
                    }
                }
                
            });
        }
        
        public void setModel(FilterModel model) {
            super.setModel(model);
            this.model = model;
            getColumn(1).setCellRenderer(new RemoveButtonRenderer(this));    
            getColumn(1).setCellEditor(new RemoveButtonRenderer(this)); 
        }
        
        public FilterModel getFilterModel() {
            return model;
        }
        
        public void addIp(String ip) {
            if(model != null) {
                model.addIP(ip);
            }
        }
        
        @Override
        public boolean isCellEditable(int row, int col) {
            if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
                return false;
            }
            return getColumnModel().getColumn(col).getCellEditor() != null;
        }
    }
    
    private class RemoveButtonRenderer extends JButton implements TableCellRenderer, TableCellEditor {
        private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
        
        public RemoveButtonRenderer(final FilteringTable table) {
            // lower case since hyperlink
            super(I18n.tr("remove"));
            
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            FontUtils.underline(this);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    table.getFilterModel().removeIP(table.getSelectedRow());
                    cancelCellEditing();
                    table.repaint();
                }
            });
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return this;
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
    
    private class FilterModel extends AbstractTableModel {

        private List<String> filters;
        
        public FilterModel(List<String> filters) {
            this.filters = filters;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return filters.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return filters.get(rowIndex);
        }
        
        public List<String> getModel() {
            return filters;
        }

        public void addIP(String ip) {
            if(!filters.contains(ip))
                filters.add(ip);
            repaint();
        }
        
        public void removeIP(int index) {
            if(index < 0 || index >= filters.size())
                return;
            filters.remove(index);
        }
    }
    
    public static class AddAction implements ActionListener {

        private JTextField textField;
        private FilteringTable table;
        
        public AddAction(JTextField textField, FilteringTable table) {
            this.table = table;
            this.textField = textField;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(textField.getText() != null && textField.getText().length() > 0) {
                try {
                    new IP(textField.getText());
                    table.addIp(textField.getText());
                    table.invalidate();
                } catch(IllegalArgumentException iae) {
                }
                textField.setText("");
            }
        }
    }

}