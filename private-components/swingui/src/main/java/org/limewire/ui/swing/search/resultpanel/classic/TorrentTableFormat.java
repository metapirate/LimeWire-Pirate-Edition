package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentScrapeScheduler;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 */
public class TorrentTableFormat extends ResultsTableFormat<VisualSearchResult> {
    
    static final int FROM_INDEX = 0;
    static final int TITLE_INDEX = 1;
    public static final int FILES_INDEX = 2;
    public static final int TRACKERS_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    static final int SEEDERS_INDEX = 5;
    static final int LEECHERS_INDEX = 6;
    static final int DOWNLOADED_INDEX = 7;
    static final int IS_SPAM_INDEX = 8;
    
    private final TorrentScrapeScheduler scrapeAdaptor;
    
    public TorrentTableFormat(TorrentScrapeScheduler scrapeAdaptor) {
        super("CLASSIC_SEARCH_TORRENT_TABLE", TITLE_INDEX, FROM_INDEX, IS_SPAM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_TORRENT_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "CLASSIC_SEARCH_TORRENT_TITLE", I18n.tr("Name"), 255, true, true),     
                new ColumnStateInfo(FILES_INDEX, "CLASS_SEARCH_TORRENT_FILES", I18n.tr("Files"), 180, true, true),
                new ColumnStateInfo(TRACKERS_INDEX, "CLASS_SEARCH_TORRENT_TRACKERS", I18n.tr("Trackers"), 255, false, true),
                new ColumnStateInfo(SIZE_INDEX, "CLASS_SEARCH_TORRENT_SIZE", I18n.tr("Size"), 20, true, true),
                new ColumnStateInfo(SEEDERS_INDEX, "CLASS_SEARCH_TORRENT_SEEDERS", I18n.tr("Seeders"), 20, SearchSettings.USE_TORRENT_SCRAPER.getValue(), true),
                new ColumnStateInfo(LEECHERS_INDEX, "CLASS_SEARCH_TORRENT_LEECHERS", I18n.tr("Leechers"), 20, SearchSettings.USE_TORRENT_SCRAPER.getValue(), true),
                new ColumnStateInfo(DOWNLOADED_INDEX, "CLASS_SEARCH_TORRENT_DOWNLOADED", I18n.tr("Downloaded"), 20, SearchSettings.USE_TORRENT_SCRAPER.getValue(), true),
                new ColumnStateInfo(IS_SPAM_INDEX, "CLASSIC_SEARCH_TORRENT_IS_SPAM", "", 10, false, false)
        });
        
        this.scrapeAdaptor = scrapeAdaptor;
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case TITLE_INDEX: return Component.class;
        case FROM_INDEX: return VisualSearchResult.class;
        case SIZE_INDEX: return Long.class;
        }
        return super.getColumnClass(column);
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
        default:
            return super.getColumnComparator(column);
        }
    }    
    
    private static boolean shouldScrape(VisualSearchResult vsr, Torrent torrent) {
        return !vsr.isSpam() && torrent.getTrackerURIS().size() > 0; 
    }

    
    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case FROM_INDEX: return vsr;
            case TITLE_INDEX: return vsr;
            case IS_SPAM_INDEX: return vsr;
            case SIZE_INDEX: return vsr.getSize();
        }

        Torrent torrent = (Torrent) vsr.getProperty(FilePropertyKey.TORRENT);
        if (torrent != null) {
            if (shouldScrape(vsr, torrent)) {
                scrapeAdaptor.queueScrapeIfNew(torrent);
            }

            TorrentScrapeData data = null;
            
            switch (column) {
            case FILES_INDEX:
                return torrent.getTorrentFileEntries();
            case TRACKERS_INDEX:
                return torrent.getTrackers();
            case SEEDERS_INDEX:
                if (shouldScrape(vsr, torrent)) {
                    data = scrapeAdaptor.getScrapeDataIfAvailable(torrent);
                    if (data != null) {
                        return data.getComplete();
                    } else {
                        return "";
                    }
                }
            case LEECHERS_INDEX:
                if (shouldScrape(vsr, torrent)) {
                    data = scrapeAdaptor.getScrapeDataIfAvailable(torrent);
                    if (data != null) {
                        return data.getIncomplete();
                    } else {
                        return "";
                    }
                }
            case DOWNLOADED_INDEX:
                if (shouldScrape(vsr, torrent)) {
                    data = scrapeAdaptor.getScrapeDataIfAvailable(torrent);
                    if (data != null) {
                        return data.getDownloaded();
                    } else {
                        return "";
                    }
                }
            }
        }
        
        return "";
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.DESCENDING, FROM_INDEX),
                    new SortKey(SortOrder.ASCENDING, TITLE_INDEX));
        else
            return super.getDefaultSortKeys();
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        default:
            return Collections.emptyList();
        }
    }

}