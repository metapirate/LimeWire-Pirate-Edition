package org.limewire.ui.swing.browser;

import org.mozilla.interfaces.nsIHistoryEntry;

public class HistoryEntry {
    
    private final int index;
    private final String name;
    private final String uri;
    
    public HistoryEntry(int idx, nsIHistoryEntry historyEntry) {
        this.index = idx;
        this.name = historyEntry.getTitle();
        this.uri = historyEntry.getURI().getAsciiSpec();
    }
    
    public String getName() {
        return name;
    }
    
    public String getUri() {
        return uri;
    }
    
    public int getIndex() {
        return index;
    }

}
