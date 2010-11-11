package org.limewire.ui.swing.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.ui.swing.table.GlazedJXTable;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.sort.TableColumnComparator;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * This is essentially a copy of glazedLists.EventListJXTableSorting class. The 
 * main difference is this class enforces the column comparators within 
 * AdvancedTableFormat if the table is constructed with an AdvancedTableFormat.
 * Otherwise is reverts to the default GlazedListComparator.
 * 
 * <p>This class supports multiple column sorts with stable results so that 
 * equal elements are not reordered by sorts on successive columns.  In 
 * addition, any column may have secondary sort columns associated with it - 
 * a single sort request on such a column will result in a multiple-column
 * sort.  Secondary sort columns are specified by providing a non-null instance
 * of EventListTableSortFormat in the <code>install()</code> method.</p> 
 */
public class EventListJXTableSorting {
    
    /** Maximum number of sorted columns. */
    private static final int MAX_SORT_COLUMNS = 5;

    /** the sorted list behind the table being sorted */
    private final SortedList sortedList;
    private final GlazedJXTable table;

    /** adapters between SortedList and JXTable */
    private final SortController sortController;

    /** the original filter pipeline, used in {@link #uninstall} only */
    private final SortController originalSortController;
    
    /** Format for table sorts on event list. */
    private final EventListTableSortFormat tableSortFormat;

    /**
     * Usually, constructors shouldn't supposed to have side-effects, but this one
     * changes the table's filter pipeline. Therefore we use this private
     * constructor and call through it from the {@link #install} method.
     */
    private EventListJXTableSorting(GlazedJXTable table, SortedList sortedList,
            EventListTableSortFormat tableSortFormat) {
        this.table = table;
        this.sortedList = sortedList;
        this.tableSortFormat = tableSortFormat;
        this.originalSortController = table.getSortController();

        this.sortController = new EventListSortController();
        table.setSortController(sortController);
        
        // Apply default sort keys.
        if (tableSortFormat != null) {
            List<SortKey> sortKeys = tableSortFormat.getDefaultSortKeys();
            this.sortController.setSortKeys(sortKeys);
        }
    }

    /**
     * Install this {@link EventListJXTableSorting} to provide the sorting
     * behaviour for the specified {@link JXTable}.
     */
    public static EventListJXTableSorting install(GlazedJXTable table, SortedList sortedList) {
        return new EventListJXTableSorting(table, sortedList, null);
    }

    /**
     * Install this {@link EventListJXTableSorting} to provide the sorting
     * behaviour for the specified {@link JXTable}.  A non-null value for
     * <code>tableSortFormat</code> provides support for secondary sort columns.
     */
    public static EventListJXTableSorting install(GlazedJXTable table, SortedList sortedList, 
            EventListTableSortFormat tableSortFormat) {
        return new EventListJXTableSorting(table, sortedList, tableSortFormat);
    }

    /**
     * Remove this {@link EventListJXTableSorting} from the {@link JXTable}.
     */
    public void uninstall() {
        table.setSortController(originalSortController);
    }

    /**
     * Implement {@link SortController} to provide sorting for {@link JXTable}.
     */
    private class EventListSortController implements SortController {

        /** the active sort columns */
        private final List<SortKey> sortKeys = new ArrayList<SortKey>(MAX_SORT_COLUMNS);
        private final List<SortKey> sortKeysReadOnly = Collections.unmodifiableList(sortKeys);
        
        /** active sort columns that do not contain any preSort columns */
        private final List<SortKey> nonHiddenSortKeys = new ArrayList<SortKey>(MAX_SORT_COLUMNS);
        private final List<SortKey> nonHiddenKeysReadOnly = Collections.unmodifiableList(nonHiddenSortKeys);

        /** {@inheritDoc} */
        public void toggleSortOrder(int columnIndex) {
            toggleSortOrder(columnIndex, GlazedLists.comparableComparator());
        }

        /** {@inheritDoc} */
        @SuppressWarnings("cast")
        public void toggleSortOrder(int columnIndex, Comparator comparator) {
            List<? extends SortKey> sortKeys = getSortKeys();

            // see if we're already sorting with this column
            SortKey columnSortKey = null;
            for(Iterator<? extends SortKey> s = sortKeys.iterator(); s.hasNext(); ) {
                SortKey sortKey = (SortKey)s.next();
                if(sortKey.getSortOrder() == SortOrder.UNSORTED) continue;
                if(sortKey.getColumn() == columnIndex) {
                    columnSortKey = sortKey;
                    break;
                }
            }

            // create the new sort key
            if(columnSortKey == null) {
                columnSortKey = new SortKey(SortOrder.ASCENDING, columnIndex);
            } else {
                SortOrder sortOrder = columnSortKey.getSortOrder() == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
                columnSortKey = new SortKey(sortOrder, columnIndex);
            }
            
            // Create new list of sort keys based on existing list.
            List<? extends SortKey> newSortKeys = sortKeys;
            
            // Apply secondary sort columns if available.
            if (tableSortFormat != null) {
                List<Integer> columnList = tableSortFormat.getSecondarySortColumns(columnIndex);
                // Apply secondary sort columns in reverse order, from least 
                // significant to most significant.  If possible, we use the
                // existing sort key for the column.
                for (int i = columnList.size(); i > 0; i--) {
                    Integer secondaryColumn = columnList.get(i - 1);
                    SortKey sortKey = findSortKey(secondaryColumn, sortKeys);
                    newSortKeys = buildSortKeys(sortKey, newSortKeys);
                }
            }

            // Apply primary sort column, and set sort keys to perform sort.
            setSortKeys(buildSortKeys(columnSortKey, newSortKeys));
        }
        
        /**
         * Returns the SortKey corresponding to the specified column from the 
         * specified list.  If a meaningful SortKey is not found, then a 
         * default SortKey with ascending order is returned. 
         */
        private SortKey findSortKey(int column, List<? extends SortKey> sortKeys) {
            // Search for sort key with matching column.
            for (SortKey sortKey : sortKeys) {
                if ((sortKey.getSortOrder() != SortOrder.UNSORTED) && 
                    (sortKey.getColumn() == column)) {
                    return new SortKey(sortKey.getSortOrder(), column);
                }
            }
            
            // Not found so return default sort key.
            return new SortKey(SortOrder.ASCENDING, column);
        }

        /**
         * Returns a list of sort keys containing the specified first key, 
         * followed by the specified list of additional keys.  This method 
         * provides support for sorting over multiple columns.
         */
        private List<? extends SortKey> buildSortKeys(SortKey firstKey, 
                List<? extends SortKey> sortKeys) {
            
            // Create SortKey list and unique column set. 
            List<SortKey> newSortKeys = new ArrayList<SortKey>();
            Set<Integer> columnSet = new HashSet<Integer>();
            
            // Add first sort key.
            newSortKeys.add(firstKey);
            columnSet.add(firstKey.getColumn());
            
            // Add remaining sort keys.  Only keys with a meaningful sort order
            // are added, and duplicate columns are omitted.
            for (SortKey sortKey : sortKeys) {
                if (newSortKeys.size() >= MAX_SORT_COLUMNS) {
                    break;
                }
                if ((sortKey.getSortOrder() != SortOrder.UNSORTED) &&
                        !columnSet.contains(sortKey.getColumn())) {
                    newSortKeys.add(sortKey);
                    columnSet.add(sortKey.getColumn());
                }
            }

            // Return new list.
            return newSortKeys;
        }

        /** {@inheritDoc} */
        @SuppressWarnings({ "unchecked", "cast" })
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            if(sortKeys == sortKeysReadOnly) return;
            if(sortKeys == null) sortKeys = Collections.emptyList();

            this.sortKeys.clear();
            this.nonHiddenSortKeys.clear();
            if(tableSortFormat != null && tableSortFormat.getPreSortColumns() != null 
            	&& tableSortFormat.getPreSortColumns().size() > 0) {
                this.sortKeys.addAll(tableSortFormat.getPreSortColumns());
            }
            this.nonHiddenSortKeys.addAll(sortKeys);
            this.sortKeys.addAll(sortKeys);

            // rebuild the SortedList's comparator
            List<Comparator> comparators = new ArrayList<Comparator>(this.sortKeys.size());
            for(int k = 0; k < this.sortKeys.size(); k++) {
                SortKey sortKey = (SortKey)this.sortKeys.get(k);
                if(sortKey.getSortOrder() == SortOrder.UNSORTED) continue;

                Comparator comparator = getComparator(sortKey.getColumn());
                if(sortKey.getSortOrder() == SortOrder.DESCENDING) comparator = GlazedLists.reverseComparator(comparator);

                comparators.add(comparator);
            }

            // figure out the final comparator
            final Comparator comparator;
            if(comparators.isEmpty()) {
                comparator = null;
            } else if(comparators.size() == 1) {
                comparator = comparators.get(0);
            } else {
                comparator = GlazedLists.chainComparators((List)comparators);
            }

            // apply this comparator to the sortedlist
            sortedList.getReadWriteLock().writeLock().lock();
            try {
                sortedList.setComparator(comparator);
            } finally {
                sortedList.getReadWriteLock().writeLock().unlock();
            }
        }

        /**
         * We need to fix this implementation so it looks into the {@link JXTable}'s
         * {@link TableColumnExt} to find the appropriate column comparator. Failing
         * that, it could look for an {@link AdvancedTableFormat} and the column's
         * Comparator.
         */
        @SuppressWarnings("unchecked")
        private Comparator getComparator(int modelIndex) {
            DefaultEventTableModel tableModel = (DefaultEventTableModel)table.getModel();
            TableFormat tableFormat = tableModel.getTableFormat();
            // TODO: local changes compared to GlazedLists implementation
            // If the tableFormat is an AdvancedTableFormat, use the table column comparators
            // found in the AdvancedTableFormat for sorting.
            if(tableFormat instanceof AdvancedTableFormat) {
                return new EventListColumnComparator(tableFormat, modelIndex, ((AdvancedTableFormat)tableFormat).getColumnComparator(modelIndex));
            } else 
                return new TableColumnComparator(tableFormat, modelIndex);
        }

        /** {@inheritDoc} */
        public List<? extends SortKey> getSortKeys() {
            // return the sort keys which do not contain preSort columns. This ensures
            // the correct table header is used when displaying sort to the user.
            return nonHiddenKeysReadOnly;
        }

        /** {@inheritDoc} */
        public SortOrder getSortOrder(int columnIndex) {
            for(SortKey s : sortKeys) {
                if(s.getColumn() == columnIndex) return s.getSortOrder();
            }
            return SortOrder.UNSORTED;
        }
    }
}
