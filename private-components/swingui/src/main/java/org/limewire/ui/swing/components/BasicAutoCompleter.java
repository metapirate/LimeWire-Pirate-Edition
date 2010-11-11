package org.limewire.ui.swing.components;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.collection.NECallable;
import org.limewire.collection.StringTrieSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.ui.swing.util.SwingUtils;

/** An autocompleter that shows its suggestions in a list and can have new suggestions added. */
public class BasicAutoCompleter implements AutoCompleter {
    
    private final ListeningExecutorService queue = ExecutorsHelper.newProcessingQueue("AutoCompleteQueue");
    
    private final JPanel entryPanel;
    private final AutoCompleteList entryList;
    
    private ListeningFuture<Boolean> lastFuture;
    private AutoCompleterCallback callback;
    private String currentText;
    private AutoCompleteDictionary dictionary;
    
    public BasicAutoCompleter() {
        this(new StringTrieSet(true));
    }
    
    public BasicAutoCompleter(AutoCompleteDictionary dictionary) {
        this.dictionary = dictionary;
        
        entryPanel = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        entryPanel.setBorder(UIManager.getBorder("List.border"));
        entryPanel.setBackground(UIManager.getColor("List.background"));
        
        entryList = new AutoCompleteList();
        JScrollPane entryScrollPane = new JScrollPane(entryList);
        entryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        entryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        entryPanel.add(entryScrollPane, "grow");
    }

    public void setDictionary(AutoCompleteDictionary dictionary) {
        this.dictionary = dictionary;
    }    

    public void addAutoCompleteSuggestion(String text) {
        dictionary.addEntry(text);
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
        return (String)entryList.getSelectedValue();
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
        
        NECallable<Boolean> runner = new NECallable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    final Collection<String> suggestions = dictionary.getPrefixedBy(currentText);
                    SwingUtils.invokeNowOrWaitWithInterrupted(new Runnable() {
                        @Override
                        public void run() {
                            setSuggestions(suggestions);
                        }
                    });
                    return true;
                } catch (InterruptedException ignored) {
                    return false;
                }
            }
        };
        
        if(dictionary.isImmediate()) {
            runner.call();
            return new SimpleFuture<Boolean>(true);
        } else {
            if(lastFuture != null) {
                lastFuture.cancel(true);
            }
            setSuggestions(Collections.<String>emptyList());
            lastFuture = queue.submit(runner);
            return lastFuture;
        }
    }
    
    private void setSuggestions(Collection<String> suggestions) {
        ListModel model = entryList.getModel();
        boolean different = suggestions.size() != model.getSize();
        if(!different) {
            // Look closer...
            int i = 0;
            for(String item : suggestions) {   
                if(i < model.getSize() && item.equals(model.getElementAt(i))) {
                    different = true;
                    break;
                }
                i++;
            }
        }
    
        // if things were different, reset the data.
        if(different) {
            entryList.setModel(new CollectionBackedListModel(suggestions));
            entryList.setVisibleRowCount(Math.min(8, suggestions.size()));
            entryList.clearSelection();
        }        
    }
    
    /** A list that's used to show auto-complete items. */
    private class AutoCompleteList extends JList {
        AutoCompleteList() {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setFocusable(false);
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
                    callback.itemSuggested((String)getSelectedValue(), false, true);
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
                callback.itemSuggested((String)getSelectedValue(), true, false);
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
                if(selectedIndex == -1)
                    selectedIndex = getModel().getSize() - 1;
                else
                    selectedIndex--;
                setSelectedIndex(selectedIndex);
                ensureIndexIsVisible(selectedIndex);
                callback.itemSuggested((String)getSelectedValue(), true, false);
            }
        }
    }
    
}
