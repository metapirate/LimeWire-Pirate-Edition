package org.limewire.ui.swing.table;

import java.util.Vector;

import javax.swing.ListSelectionModel;
import javax.swing.SizeSequence;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.SelectionMapper;
import org.jdesktop.swingx.decorator.SizeSequenceMapper;
import org.jdesktop.swingx.decorator.SortController;

/**
 * A JXTable for use with glazed lists event models. See
 * http://sites.google.com/site/glazedlists/documentation/swingx for issues with
 * SwingX. 
 */
public class GlazedJXTable extends BasicJXTable {
    
    private SizeSequenceMapper simpleRowModelMapper;
    private SelectionMapper simpleSelectionMapper;
    private SortController sortController;
    /** A hack required for pretending that there's no row heights while calling tableChanged. */
    private boolean inTableChangeRowHeightHack;

    public GlazedJXTable() {
        super();
        initialize();
    }

    public GlazedJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        initialize();
    }

    public GlazedJXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        initialize();
    }

    public GlazedJXTable(TableModel dm) {
        super(dm);
        initialize();
    }

    public GlazedJXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    private void initialize() {
        // Add initialization steps here.
    }
    
    // overriden to return false while doing a tableChanged event, 
    // so that we don't null out the parent's SizeSequence,
    // this is designed to allow JTable to properly control its SizeSequence
    // instead of requiring a mapper between model/view, where
    // we have no model distinction.
    @Override
    public boolean isRowHeightEnabled() {
        if(inTableChangeRowHeightHack) {
            return false;
        } else { 
            return super.isRowHeightEnabled();
        }
    }

    // overriden to to turn off isRowHeightEnabled while doing a tableChanged, 
    // so that we don't null out the parent's SizeSequence,
    // this is designed to allow JTable to properly control its SizeSequence
    // instead of requiring a mapper between model/view, where
    // we have no model distinction.
    @Override
    public void tableChanged(TableModelEvent e) {
        inTableChangeRowHeightHack = true;
        try {
            super.tableChanged(e);
        } finally {
            inTableChangeRowHeightHack = false;
        }
    }
    
    @Override
    protected SizeSequenceMapper getRowModelMapper() {
        if(simpleRowModelMapper == null) {
            simpleRowModelMapper = new SimpleSizeSequenceMapper();
        }
        return simpleRowModelMapper;
    }
    
    @Override
    public SelectionMapper getSelectionMapper() {
        if(simpleSelectionMapper == null) {
            simpleSelectionMapper = new SimpleSelectionMapper();
        }
        return simpleSelectionMapper;
    }
    
    @Override
    protected boolean shouldSortOnChange(TableModelEvent e) {
        return false;
    }
    
    @Override
    public void setFilters(FilterPipeline pipeline) {
        if(pipeline != null) {
            throw new UnsupportedOperationException("do not use filters.");
        }
    }
    
    @Override
    public FilterPipeline getFilters() {
        return null;
    }
    
    @Override
    public SortController getSortController() {
        return sortController;
    }
    
    public void setSortController(SortController sortController) {
        this.sortController = sortController;
    }
    
    /** Stub out all size sequence mapping. */
    private static class SimpleSizeSequenceMapper extends SizeSequenceMapper {
        @Override public void setViewSizeSequence(SizeSequence selection, int height) {}
        @Override public SizeSequence getViewSizeSequence() { return null; }
        @Override public void setFilters(FilterPipeline pipeline) {}
        @Override public void clearModelSizes() {}
        @Override public void insertIndexInterval(int start, int length, int value) {}
        @Override public void removeIndexInterval(int start, int length) {}        
        @Override public void restoreSelection() {}
        @Override protected void updateFromPipelineChanged() {}
    }
    
    /** Don't do any selection mapping. */
    private static class SimpleSelectionMapper implements SelectionMapper {
        @Override public ListSelectionModel getViewSelectionModel() { return null; }
        @Override public boolean isEnabled() { return false; }
        @Override public void setViewSelectionModel(ListSelectionModel viewSelectionModel) {}
        @Override public void setFilters(FilterPipeline pipeline) {}
        @Override public void setEnabled(boolean enabled) {}
        @Override public void clearModelSelection() {}
        @Override public void insertIndexInterval(int start, int length, boolean before) {}
        @Override public void removeIndexInterval(int start, int end) {}
    }


}
