package org.limewire.core.impl.download;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * calculates
 */
class QueueTimeCalculator {
    
    /**
     * List of all items whose DownloadState is DOWNLOADING
     */
    private final EventList<DownloadItem> downloadingList;

    public QueueTimeCalculator(EventList<DownloadItem> downloadItems) {
        EventList<DownloadItem> list =
            GlazedListsFactory.filterList(downloadItems, new DownloadStateMatcher(
                DownloadState.DOWNLOADING));

        Comparator<DownloadItem> dlComparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return (int) (o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
            }
        };

        downloadingList = GlazedListsFactory.sortedList(list, dlComparator);

    }

    public long getRemainingQueueTime(DownloadItem queueItem) {
        downloadingList.getReadWriteLock().readLock().lock();
        try {
            if (queueItem.getState() != DownloadState.LOCAL_QUEUED) {
                return DownloadItem.UNKNOWN_TIME;
            }

            int priority = queueItem.getLocalQueuePriority();
            // top priority is 1 (but may briefly be 0 when resuming)
            int index = priority - 1;

            if (index >= downloadingList.size() || index < 0) {
                return DownloadItem.UNKNOWN_TIME;
            }
            return downloadingList.get(index).getRemainingDownloadTime();
        } finally {
            downloadingList.getReadWriteLock().readLock().unlock();
        }
    }
    
    private static class DownloadStateMatcher implements Matcher<DownloadItem> {
        private final Set<DownloadState> downloadStates;

        public DownloadStateMatcher(DownloadState first, DownloadState... rest) {
            downloadStates = EnumSet.of(first, rest);
        }

        @Override
        public boolean matches(DownloadItem item) {
            if (item == null) {
                return false;
            } else {
                return downloadStates.contains(item.getState());
            }
        }

    }

}
