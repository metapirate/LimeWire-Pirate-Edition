package org.limewire.ui.swing.search;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.NECallable;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.AutoCompleter;
import org.limewire.ui.swing.components.CollectionBackedListModel;
import org.limewire.ui.swing.components.ExtendedCompoundBorder;
import org.limewire.ui.swing.components.SideLineBorder;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

/** An autocompleter that shows its suggestions in a list and can have new suggestions added. */
public class HistoryAndFriendAutoCompleter implements AutoCompleter {
    
    private final ListeningExecutorService queue = ExecutorsHelper.newProcessingQueue("AutoCompleteQueue");
    
    @Resource private Color selectionBackground;
    @Resource private Font font;
    
    private ListeningFuture<Boolean> lastFuture;
    
    private final JPanel entryPanel;
    
    private AutoCompleterCallback callback;
    private String currentText;
    
    private boolean showSuggestions = true;
    private AutoCompleteDictionary historyDictionary;
    private SmartAutoCompleteDictionary smartDictionary;
    
    private final AutoCompleteList entryList;
    private final KeywordAssistedSearchBuilder keywordSearchBuilder; 
    
    @Inject
    public HistoryAndFriendAutoCompleter(KeywordAssistedSearchBuilder keywordSearchBuilder) {
        this.keywordSearchBuilder = keywordSearchBuilder;
        
        GuiUtils.assignResources(this);
        
        entryPanel = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        entryPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        entryPanel.setBackground(UIManager.getColor("List.background"));
        
        entryList = new AutoCompleteList();
        
        JScrollPane entryScrollPane = new JScrollPane(entryList);
        entryScrollPane.setBorder(BorderFactory.createEmptyBorder());
        entryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        entryPanel.add(entryScrollPane, "grow");
    }

    public void setSuggestionsShown(boolean value) {
        this.showSuggestions = value;
    }
    
    public void setHistoryDictionary(AutoCompleteDictionary dictionary) {
        this.historyDictionary = dictionary;
    }

    public void setSmartDictionary(SmartAutoCompleteDictionary dictionary) {
        this.smartDictionary = dictionary;
    }
    
    @Override
    public void setAutoCompleterCallback(AutoCompleterCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean isAutoCompleteAvailable() {
        return entryList.getModel().getSize() != 0;
    }

    @Override
    public void decrementSelection() {
        entryList.decrementSelection();
    }

    @Override
    public JComponent getRenderComponent() {
        return entryPanel;
    }

    @Override
    public String getSelectedAutoCompleteString() {
        Object selection = entryList.getSelectedValue();
        if(selection != null) {
            return selection.toString();
        } else {
            return null;
        }
    }

    @Override
    public void incrementSelection() {
        entryList.incrementSelection();
    }

    @Override
    public ListeningFuture<Boolean> setInput(String input) {
        if(input == null) {
            input = "";
        }
        
        currentText = input;        
        // Only if both are immediate can we immediately show.
        if (historyDictionary.isImmediate() && smartDictionary.isImmediate()) {
            try {
                lookupAndSetItems(currentText);
            } catch(InterruptedException error) {
                throw new IllegalStateException(error);
            }
            return new SimpleFuture<Boolean>(true);
        } else {
            if(lastFuture != null) {
                lastFuture.cancel(true);
            }
            setItems(Collections.<Entry>emptyList());
            final String lookupText = currentText;
            lastFuture = queue.submit(new NECallable<Boolean>() {
                @Override
                public Boolean call() {
                    if(Thread.interrupted()) {
                        return false;
                    }
                    
                    try {
                        lookupAndSetItems(lookupText);
                        return true;
                    } catch (InterruptedException ignored) {
                        return false;
                    }
                }
            });
            return lastFuture;
        }
    }
    
    /**
     * Builds a list of auto-completion entries and sets them in the
     * displayable list.
     */
    private void lookupAndSetItems(final String lookupText) throws InterruptedException {
        // Create list of items.
        Collection<String> histories = historyDictionary.getPrefixedBy(lookupText);
        final ArrayList<Entry> items = new ArrayList<Entry>(histories.size());
        
        // Add search history items.
        boolean needFirstHistory = true;
        for (String string : histories) {
            if (needFirstHistory) {
                items.add(new Entry(string, Entry.Reason.FIRST_HISTORY));
                needFirstHistory = false;
            } else {
                items.add(new Entry(string, Entry.Reason.HISTORY));
            }
        }
        
        // Add smart suggestion items.
        if (showSuggestions) {
            Collection<SmartQuery> suggestions = smartDictionary.getPrefixedBy(lookupText);
            SearchCategory category = smartDictionary.getSearchCategory();
            items.ensureCapacity(items.size() + suggestions.size());
            boolean needFirstSuggestion = true;
            for (SmartQuery query : suggestions) {
                String queryText = keywordSearchBuilder.createCompositeQuery(query.getQueryData(), category);
                if (needFirstSuggestion) {
                    items.add(new Entry(queryText, query.toString(), Entry.Reason.FIRST_SUGGESTION));
                    needFirstSuggestion = false;
                } else {
                    items.add(new Entry(queryText, query.toString(), Entry.Reason.SUGGESTION));
                }
            }
        }
        
        SwingUtils.invokeNowOrWaitWithInterrupted(new Runnable() {
            @Override
            public void run() {
                setItems(items);
            }
        });
    }
    
    /**
     * Updates the auto-completion list with the specified entries.
     */
    private void setItems(Collection<Entry> items) {
        entryList.setModel(new CollectionBackedListModel(items));
        entryList.setVisibleRowCount(Math.min(8, items.size()));
    }
    
    /** A list that's used to show auto-complete items. */
    private class AutoCompleteList extends JList {
        AutoCompleteList() {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setFocusable(false);
            setCellRenderer(new Renderer());
            setFont(font);
            setSelectionBackground(selectionBackground);
            setSelectionForeground(Color.BLACK);
        }
        
        // override to return true always, to enforce '...' added
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        
        /**
         * Sets the text field's selection with the clicked item.
         */
        @Override
        protected void processMouseEvent(MouseEvent me) {
            super.processMouseEvent(me);
            
            if(me.getID() == MouseEvent.MOUSE_CLICKED) {
                int idx = locationToIndex(me.getPoint());
                if(idx != -1 && isSelectedIndex(idx)) {
                    callback.itemSuggested(getSelectedValue().toString(), false, true);
                }
            }
        }
        
        /**
         * Increments the selection by one.
         */
        void incrementSelection() {
            if(getSelectedIndex() == getModel().getSize() - 1) {
                callback.itemSuggested(currentText, true, false);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex() + 1;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                callback.itemSuggested(getSelectedValue().toString(), true, false);
            }
        }
        
        /**
         * Decrements the selection by one.
         */
        void decrementSelection() {
            if(getSelectedIndex() == 0) {
                callback.itemSuggested(currentText, true, false);
                clearSelection();
            } else {
                int selectedIndex = getSelectedIndex();
                if(selectedIndex == -1) {
                    selectedIndex = getModel().getSize() - 1;
                } else {
                    selectedIndex--;
                }
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                if (getSelectedValue() != null) {
                    callback.itemSuggested(getSelectedValue().toString(), true, false);
                }
            }
        }
    }
    
    /**
     * Cell renderer for entries in the auto-completion list.
     */
    private static class Renderer extends DefaultListCellRenderer {
        private final ExtendedCompoundBorder compoundBorder;
        private final JPanel firstSuggestionPanel;
        private final DefaultListCellRenderer firstSuggestionLabel;
        private final JLabel firstSuggestionTitle;
        private final Border firstSuggestionBorder = new SideLineBorder(Color.GRAY, SideLineBorder.Side.TOP);
        
        public Renderer() {
            compoundBorder = new ExtendedCompoundBorder(BorderFactory.createEmptyBorder(), BorderFactory.createEmptyBorder(0, 2, 0, 2));
            
            firstSuggestionPanel = new JPanel();
            firstSuggestionPanel.setLayout(new MigLayout("fill, gap 0, insets 0"));
            firstSuggestionPanel.setBorder(compoundBorder);
            
            firstSuggestionLabel = new DefaultListCellRenderer();
            firstSuggestionTitle = new DefaultListCellRenderer();
            FontUtils.changeSize(firstSuggestionTitle, -1);
            firstSuggestionTitle.setForeground(Color.GRAY);
            
            firstSuggestionPanel.add(firstSuggestionLabel, "alignx left, grow, wmin 0, gapright 10");            
            firstSuggestionPanel.add(firstSuggestionTitle, "alignx right");            
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            String render;
            if(value == null) {
                render = "";
            } else {
                render = value.toString();
                if(value instanceof Entry) {
                    render = ((Entry) value).getDisplayText();
                    if(((Entry)value).reason == Entry.Reason.FIRST_HISTORY ||
                            ((Entry)value).reason == Entry.Reason.FIRST_SUGGESTION) {
                        firstSuggestionLabel.getListCellRendererComponent(list, render, index, isSelected, cellHasFocus);
                        firstSuggestionLabel.setBorder(BorderFactory.createEmptyBorder());
                        if(isSelected) {
                            firstSuggestionTitle.setForeground(list.getSelectionForeground());
                        } else {
                            firstSuggestionTitle.setForeground(Color.GRAY);
                        }
                        if(index != 0) {
                            compoundBorder.setOuterBorder(firstSuggestionBorder);
                        } else {
                            compoundBorder.setOuterBorder(BorderFactory.createEmptyBorder());
                        }
                        if (((Entry)value).reason == Entry.Reason.FIRST_HISTORY) {
                            firstSuggestionTitle.setText(I18n.tr("History"));
                        } else {
                            firstSuggestionTitle.setText(I18n.tr("Smart Search"));
                        }
                        firstSuggestionTitle.setBackground(firstSuggestionLabel.getBackground());
                        firstSuggestionPanel.setBackground(firstSuggestionLabel.getBackground());
                        return firstSuggestionPanel;
                    }
                }
            }
            
            super.getListCellRendererComponent(list, render, index, isSelected, cellHasFocus);
            compoundBorder.setOuterBorder(getBorder());
            setBorder(compoundBorder);
            return this;
        }
    }
    
    /**
     * An entry in the auto-completion list.
     */
    private static class Entry {
        private static enum Reason { FIRST_HISTORY, HISTORY, FIRST_SUGGESTION, SUGGESTION };
        
        private final String query;
        private final String display;
        private final Reason reason;
        
        public Entry(String query, Reason reason) {
            this(query, query, reason);
        }
        
        public Entry(String query, String display, Reason reason) {
            this.query = query;
            this.display = display;
            this.reason = reason;
        }
        
        public String getDisplayText() {
            return display;
        }
        
        @Override
        public String toString() {
            return query;
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            } else {
                return ((Entry)obj).query.equals(query) && ((Entry)obj).reason == reason;
            }
        }
    }
    
}
