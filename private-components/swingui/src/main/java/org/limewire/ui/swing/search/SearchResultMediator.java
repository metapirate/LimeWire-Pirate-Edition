package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.ui.swing.nav.NavMediator;

public class SearchResultMediator implements NavMediator<JComponent> {

    private final JComponent searchPanel;
    
    public SearchResultMediator(JComponent searchPanel) {
        this.searchPanel = searchPanel;
    }
    
    @Override
    public JComponent getComponent() {
        return searchPanel;
    }
}
