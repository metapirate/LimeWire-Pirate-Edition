package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentTracker;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.library.table.RemoveButton;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.StripedJXTable;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTrackersPanel implements FileInfoPanel {

    private static final int URL_COLUMN = 0;
    private static final int TIER_COLUMN = 1;
    private static final int REMOVE_COLUMN = 2;
    
    private final Torrent torrent;
    private final JPanel component;
    
    private List<TorrentTracker> trackerList = null;
    private StripedJXTable table;
    
    public FileInfoTrackersPanel(PropertiableFile propertiableFile, TableDecorator tableDecorator) {
        component = new JPanel(new MigLayout("fillx, gap 0, insets 7 7 20 7"));
        
        torrent = (Torrent)propertiableFile.getProperty(FilePropertyKey.TORRENT);
        if(torrent == null) {
            table = null;
            return;
        }
        
        trackerList = torrent.getTrackers();
        
        createTable(tableDecorator); 
        
        component.add(new JScrollPane(table), "gaptop 10, gapbottom 6, span, grow, wrap");
        
        if(torrent.isValid() && torrent.isEditable()) {
            JLabel addTrackerLabel = new JLabel(I18n.tr("Add Tracker:"));
            final JTextField trackerUrlTextField = new JTextField(100);
            JLabel tierLabel = new JLabel(I18n.tr("Tier:"));
            final JSpinner tierSpinner = new JSpinner(new SpinnerNumberModel(0,0,20,1));
            final JLabel errorLabel = new JLabel(I18n.tr("Tracker URL Invalid"));
            errorLabel.setForeground(Color.RED);
            errorLabel.setVisible(false);
            
            JButton addButton = new JButton(new AbstractAction(I18n.tr("Add")) {
                @Override
                public void actionPerformed(ActionEvent event) {
                    String url = trackerUrlTextField.getText().trim();
                    int tier = ((Integer)tierSpinner.getValue()).intValue();
                    
                    if (addTracker(url, tier)) {
                        refreshTable();
                        trackerUrlTextField.setText("");
                        tierSpinner.setValue(0);
                        errorLabel.setVisible(false);
                    } 
                    else {
                        errorLabel.setVisible(true);
                    }
                }
            });
            
            TextFieldClipboardControl.install(trackerUrlTextField);
            JFormattedTextField tierSpinnerTextField = ((JSpinner.DefaultEditor)tierSpinner.getEditor()).getTextField();
            tierSpinnerTextField.setEditable(false);
            
            component.add(addTrackerLabel, "gapright 5");
            component.add(trackerUrlTextField, "growx, gapright 10");
            component.add(tierLabel, "gapright 5");
            component.add(tierSpinner, "gapright 10");
            component.add(addButton, "wrap");
            component.add(errorLabel, "hidemode 3");
        } else {
            TableColumn removeColumn = table.getColumn(REMOVE_COLUMN);
            ((TableColumnExt)removeColumn).setVisible(false);
        }
    }
    
    private void createTable(TableDecorator tableDecorator) {
      AbstractTableModel model = new AbstractTableModel() {
            
            @Override
            public String getColumnName(int columnIndex) {
                if (columnIndex == URL_COLUMN) { 
                    return I18n.tr("URL");
                }
                else if (columnIndex == TIER_COLUMN) {
                    return I18n.tr("Tier");
                }
                
                return "";
            }
            
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == URL_COLUMN) { 
                    return trackerList.get(rowIndex).getURI();
                }
                else if (columnIndex == TIER_COLUMN) {
                    return trackerList.get(rowIndex).getTier();
                } else if (columnIndex == REMOVE_COLUMN) {
                    return rowIndex;
                }
                
                throw new IllegalArgumentException("Invalid Column Used");
            }
            @Override
            public int getRowCount() {
                return trackerList.size();
            }
            @Override
            public int getColumnCount() {
                return 3;
            }
        };
        
        table = new StripedJXTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == REMOVE_COLUMN;
            }
        
        };
      
        tableDecorator.decorate(table);
        
        table.setCellSelectionEnabled(false);
        table.setShowGrid(false, false);
        table.setSortable(false);
        
        TableColumn tierColumn = table.getColumn(TIER_COLUMN);
        tierColumn.setMaxWidth(45);
        tierColumn.setMinWidth(25);
        tierColumn.setWidth(40);
        tierColumn.setPreferredWidth(40);
        
        TableColumn removeColumn = table.getColumn(REMOVE_COLUMN);
        removeColumn.setCellRenderer(new RemoveRendererEditor());
        removeColumn.setCellEditor(new RemoveRendererEditor());
        removeColumn.setMaxWidth(12);
        removeColumn.setMinWidth(12);
        removeColumn.setWidth(12);
        removeColumn.setPreferredWidth(12);
        
        DefaultLimeTableCellRenderer tierRenderer = new DefaultLimeTableCellRenderer();
        tierRenderer.setHorizontalAlignment(JLabel.CENTER);
        tierColumn.setCellRenderer(tierRenderer);
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
    }

    @Override
    public void dispose() {
    }
    
    /**
     * Pulls new tracker data and refreshes the model.
     */
    private void refreshTable() {
        trackerList = torrent.getTrackers();
        table.repaint();
    }
    
    /**
     * Attempts to add a tracker.
     * 
     * @return true if the add was successful and the tracker was valid.
     */
    private boolean addTracker(String url, int tier) {
        boolean duplicate = false;
        for ( TorrentTracker tracker : trackerList ) {
            if (tracker.getURI().equals(url)) {
                duplicate = true;
                break;
            }
        }
        
        if (!duplicate) {
            try {
                new URL(url);
                torrent.addTracker(url, tier);
                return true;
            }
            catch (MalformedURLException e) {
            }
        }
        
        return false;
    }
    
    /**
     * Removes the tracker at the given row from the model.
     * 
     * @return true if a row was actually removed.
     */
    private boolean removeTracker(int row) {
        
        if (row == 0) {
            return false;
        }
        
        TorrentTracker tracker = trackerList.get(row);
        
        if (tracker != null) {
            torrent.removeTracker(tracker.getURI().toASCIIString(), tracker.getTier());
            return true;
        }
        return false;   
    }
    
    private class RemoveRendererEditor extends TableRendererEditor {
        private int currentRow = -1;

        private final RemoveButton button;
        
        public RemoveRendererEditor() {       
            button = new RemoveButton();
            button.removeActionHandListener();
            button.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (removeTracker(currentRow)) {
                        cancelCellEditing();
                        refreshTable();
                    }
                }
            });
            add(button);
        }

        @Override
        protected Component doTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if (value != null) {
                currentRow = ((Integer)value).intValue();
                button.setVisible(currentRow != 0);
            }
            
            return this;
        }

        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                int currentRow = ((Integer)value).intValue();
                button.setVisible(currentRow != 0);
            }
            
            return this;
        }
    }

   
}
