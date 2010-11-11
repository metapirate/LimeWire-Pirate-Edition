package org.limewire.ui.swing.downloads.table;

import java.util.EnumSet;
import java.util.Set;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.matchers.Matcher;

public class DownloadStateMatcher implements Matcher<DownloadItem> {

    private final Set<DownloadState> downloadStates;

    public DownloadStateMatcher(DownloadState first, DownloadState... rest) {
        downloadStates = EnumSet.of(first, rest);
    }

    @Override
    public boolean matches(DownloadItem item) {
        if (item == null)
            return false;

        return downloadStates.contains(item.getState());
    }

}
