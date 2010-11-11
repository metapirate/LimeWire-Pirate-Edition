package org.limewire.ui.swing.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.SortKey;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.EventListTableSortFormat;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.Objects;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

public class FileInfoBittorrentPanel implements FileInfoPanel, EventListener<TorrentEvent> {

    @Resource private Font selectFont;
    
    public static final String TORRENT_FILE_ENTRY_SELECTED = "torrentFileEntrySelected";
    
    private static final int DONT_DOWNLOAD = 0;

    private static final int LOWEST_PRIORITY = 1;

    private static final int NORMAL_PRIORITY = 2;

    private static final int HIGHEST_PRIORITY = 3;

    private final Torrent torrent;

    private final JPanel component;

    private BitTorrentTable table;
    
    private EventListJXTableSorting tableSorting;

    /**
     * Items in the eventList are expected to be in the order that they are
     * returned from the Torrent instance. This is so we can pull items out by
     * the matching index in the TorrentFileEntry.
     */
    private EventList<TorrentFileEntryWrapper> eventList;

    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public FileInfoBittorrentPanel(Torrent torrent) {
        GuiUtils.assignResources(this);
        this.torrent = torrent;

        component = new JPanel(new MigLayout("fill", "[grow]", "[][grow]"));

        init();
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        // do nothing
    }

    private void init() {
        component.setOpaque(false);

        ObservableElementList.Connector<TorrentFileEntryWrapper> torrentFileEntryConnector = GlazedLists
                .beanConnector(TorrentFileEntryWrapper.class);
        eventList = GlazedListsFactory.observableElementList(
                new BasicEventList<TorrentFileEntryWrapper>(), torrentFileEntryConnector);
        
        List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
        for (TorrentFileEntry entry : fileEntries) {
            eventList.add(new TorrentFileEntryWrapper(entry));
        }

        
        // NOTE: this sortedList should never be used for iterating over torrent values, its strictly used to
        // sort the table
        SortedList<TorrentFileEntryWrapper> sortedList = GlazedListsFactory.sortedList(eventList, null);
        
        BitTorrentTableFormat tableFormat = new BitTorrentTableFormat();
        table = new BitTorrentTable(new DefaultEventTableModel<TorrentFileEntryWrapper>(sortedList, tableFormat));
        
        tableSorting = EventListJXTableSorting.install(table, sortedList, tableFormat);

        JLabel selectLabel = new JLabel(I18n.tr("Select"));
        selectLabel.setFont(selectFont);
        
        HyperlinkButton allButton = new HyperlinkButton(new SelectAll(I18n.tr("all")));
        allButton.setFont(selectFont);
        
        HyperlinkButton noneButton = new HyperlinkButton(new SelectNone(I18n.tr("none")));
        noneButton.setFont(selectFont);
        
        // only show add/remove all buttons if is downloadng
        if(!torrent.isFinished()) {
            component.add(selectLabel, "gapleft 5, gaptop 2, split 3");
            component.add(allButton, "gapleft 6, gaptop 2");
            component.add(noneButton, "gapleft 6, gaptop 2, wrap");
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        configureEnclosingScrollPane(scrollPane);
        
        component.add(scrollPane, "grow, gaptop 7");
        
        torrent.addListener(this);
    }

    @Override
    public boolean hasChanged() {
        boolean hasChanged = false;
        for (TorrentFileEntryWrapper wrapper : eventList) {
            if (wrapper.hasChanged()) {
                hasChanged = true;
                break;
            }
        }
        return hasChanged;
    }

    @Override
    public void save() {
        if (!torrent.isEditable()) {
            return;
        }
        torrent.getLock().lock();
        try {
            if (hasChanged() && !torrent.isFinished() && torrent.isValid()) {
                for (TorrentFileEntryWrapper wrapper : eventList) {
                    torrent.setTorrenFileEntryPriority(wrapper.getTorrentFileEntry(), wrapper
                            .getPriority());
                }
            }
        } finally {
            torrent.getLock().unlock();
        }
    }

    @Override
    public void dispose() {
        tableSorting.uninstall();
        torrent.removeListener(this);
    }
    
    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane(JScrollPane scrollPane) {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }

    private class BitTorrentTable extends MouseableTable {
        boolean torrentPartSelected = true;
        public BitTorrentTable(final DefaultEventTableModel<TorrentFileEntryWrapper> model) {
            super(model);
            setShowHorizontalLines(false);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setColumnSelectionAllowed(false);
            final CheckBoxRendererEditor checkBoxEditor = new CheckBoxRendererEditor();
            checkBoxEditor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (checkBoxEditor.getCellEditorValue() != null) {
                        checkBoxEditor.getCellEditorValue().setPriority(checkBoxEditor.isSelected() ? 1 : 0);
                        checkBoxEditor.cancelCellEditing();
                    }
                    
                    validateSelection();
                    
                    BitTorrentTable.this.repaint();
                }
            });
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellRenderer(new CheckBoxRendererEditor());
            getColumn(BitTorrentTableFormat.DOWNLOAD_INDEX).setCellEditor(checkBoxEditor);
            
            getColumn(BitTorrentTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());
            getColumn(BitTorrentTableFormat.SIZE_INDEX).setMaxWidth(72);

            getColumn(BitTorrentTableFormat.PERCENT_INDEX).setCellRenderer(new PercentRenderer());
            getColumn(BitTorrentTableFormat.PERCENT_INDEX).setMaxWidth(20);
            getColumn(BitTorrentTableFormat.NAME_INDEX).setCellRenderer(new DefaultLimeTableCellRenderer());
            getColumn(BitTorrentTableFormat.NAME_INDEX).setMinWidth(140);
            
            final PriorityRendererEditor editor = new PriorityRendererEditor();
            editor.getButton().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int oldPriority = editor.getCellEditorValue().getPriority();
                    if (oldPriority != DONT_DOWNLOAD) {
                        editor.getCellEditorValue().setPriority(
                                oldPriority + 1 > HIGHEST_PRIORITY ? LOWEST_PRIORITY
                                        : (oldPriority + 1));
                        editor.cancelCellEditing();
                        BitTorrentTable.this.repaint();
                    }
                }

            });
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellRenderer(
                    new PriorityRendererEditor());
            getColumn(BitTorrentTableFormat.PRIORITY_INDEX).setCellEditor(editor);

            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMaxWidth(30);
            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setMinWidth(30);

            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMaxWidth(50);
            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setMinWidth(50);

            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMaxWidth(60);
            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setMinWidth(60);
            
            boolean editable = torrent.isEditable();
            getColumnExt(BitTorrentTableFormat.PRIORITY_INDEX).setVisible(editable);
            getColumnExt(BitTorrentTableFormat.PERCENT_INDEX).setVisible(editable);
            getColumnExt(BitTorrentTableFormat.DOWNLOAD_INDEX).setVisible(editable);
            
        }
        
        private void validateSelection() {
            if ( isAnyTorrentPartSelected() != torrentPartSelected ) {
                torrentPartSelected = !torrentPartSelected;
                support.firePropertyChange(TORRENT_FILE_ENTRY_SELECTED, !torrentPartSelected, torrentPartSelected);
            }
        }
        
        @SuppressWarnings("unchecked")
        private boolean isAnyTorrentPartSelected() {
            DefaultEventTableModel<TorrentFileEntryWrapper> model = (DefaultEventTableModel<TorrentFileEntryWrapper>)getModel();
            for (int counter = 0; counter < model.getRowCount(); counter++) {
                TorrentFileEntryWrapper torrentFile = model.getElementAt(counter);
                if (torrentFile.getPriority() != 0)
                    return true;
            }
            return false;
        }
    }

    private class BitTorrentTableFormat extends AbstractTableFormat<TorrentFileEntryWrapper> implements EventListTableSortFormat, AdvancedTableFormat<TorrentFileEntryWrapper> {

        private static final int DOWNLOAD_INDEX = 0;
        private static final int NAME_INDEX = 1;
        private static final int SIZE_INDEX = 2;
        private static final int PERCENT_INDEX = 3;
        private static final int PRIORITY_INDEX = 4;

        public BitTorrentTableFormat() {
            super(I18n.tr("DL"), I18n.tr("Name"), I18n.tr("Size"), I18n.tr("%"), I18n.tr("Priority"));
        }

        @Override
        public Object getColumnValue(TorrentFileEntryWrapper baseObject, int column) {
            switch (column) {
            case DOWNLOAD_INDEX:
                return baseObject;
            case NAME_INDEX:
                return baseObject.getPath();
            case SIZE_INDEX:
                return baseObject.getSize();
            case PERCENT_INDEX:
                return baseObject;
            case PRIORITY_INDEX:
                return baseObject;
            }
            throw new IllegalStateException("Unknown column:" + column);
        }
        
        @Override
        public Comparator getColumnComparator(int column) {
            switch(column) {
            case DOWNLOAD_INDEX:
                return new SelectedComparator();
            case NAME_INDEX:
                return new NameComparator();
            case SIZE_INDEX:
                return Objects.getComparator(true);
            case PERCENT_INDEX:
                return new PercentComparator();
            case PRIORITY_INDEX:
                return new PriorityComparator();
            }
            throw new IllegalStateException("Unknown column:" + column);
        }

        @Override
        public List<SortKey> getDefaultSortKeys() {
            return Collections.emptyList();
        }

        @Override
        public List<SortKey> getPreSortColumns() {
            return Collections.emptyList();
        }

        @Override
        public List<Integer> getSecondarySortColumns(int column) {
            return Collections.emptyList();
        }

        @Override
        public Class getColumnClass(int column) {
            switch(column) {
            case DOWNLOAD_INDEX:
                return TorrentFileEntryWrapper.class;
            case NAME_INDEX:
                return String.class;
            case SIZE_INDEX:
                return Long.class;
            case PERCENT_INDEX:
                return TorrentFileEntryWrapper.class;
            case PRIORITY_INDEX:
                return TorrentFileEntryWrapper.class;
            }
            throw new IllegalStateException("Unknown column:" + column);
        }
    }
    
    /**
     * Compares whether two TorrentFileEntries are selected or not. 
     */
    private class SelectedComparator implements Comparator<TorrentFileEntryWrapper> {
        @Override
        public int compare(TorrentFileEntryWrapper o1, TorrentFileEntryWrapper o2) {
            boolean o1isSelected = getIsSelected(o1);
            boolean o2isSelected = getIsSelected(o2);
            
            if(o1isSelected && o2isSelected)
                return 0;
            else if(o1isSelected)
                return 1;
            else 
                return -1;
        }
        
        private boolean getIsSelected(TorrentFileEntryWrapper wrapper) {
            if (torrent.isFinished()) {
                return wrapper.getProgress() == 1.0f && wrapper.getPriority() > DONT_DOWNLOAD;
            } else if (wrapper.getProgress() == 1.0f) {
                return true;
            } else {
                return wrapper.getPriority() != DONT_DOWNLOAD;
            }
        }
    }
    
    /**
     * Compares the Percent complete of two TorrentFileEntries. If the file is
     * not being downloaded, any percent completed is ignored.
     */
    private class PercentComparator implements Comparator<TorrentFileEntryWrapper> {
        @Override
        public int compare(TorrentFileEntryWrapper o1, TorrentFileEntryWrapper o2) {
            int o1Percent = (int) o1.getProgress() * 100;
            int o2Percent = (int) o2.getProgress() * 100;
            
            if(o1.getPriority() == DONT_DOWNLOAD)
                o1Percent = -1;
            if(o2.getPriority() == DONT_DOWNLOAD)
                o2Percent = -1;

            return o1Percent - o2Percent;
        }       
    }
    
    /**
     * Compares the Priority of two TorrentFileEntries. Completed torrents
     * are considered higher priority than the max priority setting.
     */
    private class PriorityComparator implements Comparator<TorrentFileEntryWrapper> {
        @Override
        public int compare(TorrentFileEntryWrapper o1, TorrentFileEntryWrapper o2) {
            int o1Priority = getPriority(o1);
            int o2Priority = getPriority(o2);
            
            return o1Priority - o2Priority;
        }
        
        private int getPriority(TorrentFileEntryWrapper wrapper) {
            if (wrapper.getProgress() == 1.0f) {
                return 4;
            } else {
                return wrapper.getPriority();
            }
        }
    }
    
    /**
     * Comapares the path/filename of two TorrentFileEntries.
     */
    private class NameComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    }

    private class CheckBoxRendererEditor extends JCheckBox implements TableCellRenderer,
            TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();

        private TorrentFileEntryWrapper currentWrapper;

        public CheckBoxRendererEditor() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, column, column);
        }

        private Component getTableCellComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            if (value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;

                if (torrent.isFinished()) {
                    setEnabled(false);
                    setSelected(currentWrapper.getProgress() == 1.0f
                            && currentWrapper.getPriority() > DONT_DOWNLOAD);
                } else if (currentWrapper.getProgress() == 1.0f) {
                    setSelected(true);
                    setEnabled(false);
                } else {
                    setSelected(((TorrentFileEntryWrapper) value).getPriority() != DONT_DOWNLOAD);
                    setEnabled(!torrent.isFinished());
                }
            } else {
                currentWrapper = null;
                setSelected(false);
            }

            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
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

    private class PercentRenderer extends DefaultLimeTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

            if (value instanceof TorrentFileEntryWrapper) {
                float percent = ((TorrentFileEntryWrapper) value).getProgress();
                if (torrent.isFinished()) {
                    if (percent == 1.0f
                            && ((TorrentFileEntryWrapper) value).getPriority() > DONT_DOWNLOAD) {
                        setText(I18n.tr("Done"));
                    } else {
                        setText("");
                    }
                } else if (((TorrentFileEntryWrapper) value).getPriority() == DONT_DOWNLOAD) {
                    setText("");
                } else {
                    setText((int) (percent * 100) + "%");
                }
            } else {
                setText("");
            }

            return this;
        }
    }

    private class PriorityRendererEditor extends JPanel implements TableCellRenderer,
            TableCellEditor {

        /** List of cell editor listeners. */
        private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();

        @Resource
        private Icon lowestPriorityIcon;

        @Resource
        private Icon normalPriorityIcon;

        @Resource
        private Icon highestPriorityIcon;

        @Resource
        private Font textFont;

        @Resource
        private Color fontColor;

        private final JButton button;

        private TorrentFileEntryWrapper currentWrapper;

        public PriorityRendererEditor() {
            super(new MigLayout("align 50%"));
            GuiUtils.assignResources(this);

            button = new JButton();
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setFont(textFont);
            button.setFocusPainted(false);
            button.setForeground(fontColor);
            FontUtils.underline(button);

            add(button);
        }

        public JButton getButton() {
            return button;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return getTableCellComponent(table, value, isSelected, row, column);
        }

        private Component getTableCellComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            if (value instanceof TorrentFileEntryWrapper) {
                currentWrapper = (TorrentFileEntryWrapper) value;
                int priority = ((TorrentFileEntryWrapper) value).getPriority();
                if (((TorrentFileEntryWrapper) value).getProgress() == 1.0f) {
                    button.setIcon(null);
                    button.setText("");
                    // button.setText(I18n.tr("delete"));
                } else if (priority == DONT_DOWNLOAD) {
                    button.setIcon(null);
                    button.setText("");
                } else if (priority == LOWEST_PRIORITY) {
                    button.setIcon(lowestPriorityIcon);
                    button.setText("");
                } else if (priority == NORMAL_PRIORITY) {
                    button.setIcon(normalPriorityIcon);
                    button.setText("");
                } else if (priority == HIGHEST_PRIORITY) {
                    button.setIcon(highestPriorityIcon);
                    button.setText("");
                }
            } else {
                currentWrapper = null;
                button.setIcon(null);
                button.setText("");
            }
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener l) {
            if (!listenerList.contains(l)) {
                listenerList.add(l);
            }
        }

        @Override
        public void cancelCellEditing() {
            ChangeEvent event = new ChangeEvent(this);
            for (int i = 0, size = listenerList.size(); i < size; i++) {
                listenerList.get(i).editingCanceled(event);
            }
        }

        @Override
        public TorrentFileEntryWrapper getCellEditorValue() {
            return currentWrapper;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener l) {
            listenerList.remove(l);
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

    public void addPropertyChangeListener( PropertyChangeListener listener )
    {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        support.removePropertyChangeListener(listener);
    }
    
    @Override
    public void handleEvent(TorrentEvent event) {
        if (event.getType() == TorrentEventType.STATUS_CHANGED || event.getType() == TorrentEventType.COMPLETED) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    List<TorrentFileEntry> fileEntries = torrent.getTorrentFileEntries();
                    for (TorrentFileEntry newEntry : fileEntries) {
                        TorrentFileEntryWrapper wrapper = eventList.get(newEntry.getIndex());
                        wrapper.setTorrentFileEntry(newEntry);
                    }
                }
            });
        }
    }
       
    private final class SelectNone extends AbstractAction {
        private SelectNone(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            torrent.getLock().lock();
            try {
                if(!torrent.isFinished()) {
                    for (TorrentFileEntryWrapper wrapper : eventList) {
                        if(!(wrapper.getProgress() == 1.0f)) {
                            wrapper.setPriority(DONT_DOWNLOAD);
                        }
                    }
                }
            } finally {
                torrent.getLock().unlock();
            }
            
            table.repaint();
            table.validateSelection();
        }
    }

    private final class SelectAll extends AbstractAction {
        private SelectAll(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            torrent.getLock().lock();
            try {
                if(!torrent.isFinished()) {
                    for (TorrentFileEntryWrapper wrapper : eventList) {
                        if(wrapper.getPriority() == DONT_DOWNLOAD) {
                            wrapper.setPriority(LOWEST_PRIORITY);
                        }
                    }
                }
            } finally {
                torrent.getLock().unlock();
            }
            table.repaint();
            table.validateSelection();
        }
    }
}
