package org.limewire.ui.swing.advanced.connection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.monitor.IncomingSearchManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList;
import ca.odell.glazedlists.swing.DefaultEventListModel;

import com.google.inject.Inject;

/**
 * Display panel for the incoming search list.
 */
public class IncomingSearchPanel extends JPanel implements Disposable {
    
    /** Manager instance for incoming search data. */
    private IncomingSearchManager incomingManager;
    /** The swing-thread wrapped event list, to dispose of later. */
    private SwingThreadProxyEventList<String> swingThreadList;
    
    /** Handler instance for outgoing searches. */
    private SearchHandler searchHandler;

    private JLabel incomingLabel = new JLabel();
    private JScrollPane scrollPane = new JScrollPane();
    private JList incomingList = new JList();
    
    /**
     * Constructs the IncomingSearchPanel to display incoming search phrases.
     */
    @Inject
    public IncomingSearchPanel(
            IncomingSearchManager incomingManager,
            final SearchHandler searchHandler) {
        
        this.incomingManager = incomingManager;
        this.searchHandler = searchHandler;
        
        setLayout(new BorderLayout());
        setOpaque(false);

        incomingLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        incomingLabel.setText(I18n.tr("Incoming Searches - people on the P2P Network are now searching for..."));

        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        
        incomingList.setPrototypeCellValue("                                      ");
        incomingList.setFixedCellHeight(16);
        
        // This no longer installs a transfer handler to support drag/drop
        // operations.  Earlier versions installed one, but there was no 
        // apparent reason for doing so. 
        
        // Add listener to perform text search when double-clicked.
        incomingList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    int index = incomingList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        Object text = incomingList.getModel().getElementAt(index);
                        doSearch(String.valueOf(text));
                    }
                }
            }
        });

        add(incomingLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(incomingList);
    }

    /**
     * Initializes the data models in the container.
     */
    public void initData() {
        if (!(incomingList.getModel() instanceof DefaultEventListModel)) {
            // Get list of incoming search queries.
            EventList<String> incomingSearchList = incomingManager.getIncomingSearchList();
            swingThreadList = GlazedListsFactory.swingThreadProxyEventList(incomingSearchList);
                
            // Set the list model.  EventListModel automatically wraps the 
            // actual list in a Swing list to ensure that all events are fired 
            // on the UI thread.
            incomingList.setModel(new DefaultEventListModel<String>(swingThreadList));
        }
    }
    
    /**
     * Clears the data models in the container.
     */
    @Override
    public void dispose() {
        // Get list model and dispose resources.
        ListModel listModel = incomingList.getModel();
        if (listModel instanceof DefaultEventListModel) {
            ((DefaultEventListModel) listModel).dispose();
        }
        
        if(swingThreadList != null) {
            swingThreadList.dispose();
            swingThreadList = null;
        }
        
        // Set default model to remove old reference.
        incomingList.setModel(new DefaultListModel());
    }
    
    /**
     * Performs a search using the specified query text.
     */
    private void doSearch(String text) {
        // Get default search category.
        SearchCategory defaultCategory = SearchCategory.forId(
                SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue());
        
        // Perform search using input text.
        searchHandler.doSearch(DefaultSearchInfo.createKeywordSearch(text, defaultCategory));
        
        // Activate main frame to display search results.
        GuiUtils.getMainFrame().toFront();
    }
}
