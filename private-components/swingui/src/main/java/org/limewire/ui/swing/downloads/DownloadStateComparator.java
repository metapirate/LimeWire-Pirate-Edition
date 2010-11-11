package org.limewire.ui.swing.downloads;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

class DownloadStateComparator  implements Comparator<DownloadItem>{
    
    @Override
    public int compare(DownloadItem o1, DownloadItem o2) {
        if (o1 == o2){
            return 0;
        }

        return getSortPriority(o1.getState()) - getSortPriority(o2.getState());
    }  
            
    private int getSortPriority(DownloadState state){
        
        switch(state){
        case DONE: return 1;
        case FINISHING: return 2;
        case APPLYING_DEFINITION_UPDATE: return 2;
        case DOWNLOADING: return 3;
        case RESUMING: return 4;
        case CONNECTING: return 5;
        case PAUSED: return 6;
        case REMOTE_QUEUED: return 7;
        case LOCAL_QUEUED: return 8;
        case TRYING_AGAIN: return 9;
        case STALLED: return 10;
        case ERROR: return 11;       
        case CANCELLED: return 12;
        case DANGEROUS: return 13;
        case SCANNING: return 14;
        case SCANNING_FRAGMENT: return 15;
        case THREAT_FOUND: return 16;
        case SCAN_FAILED: return 17;
        }
        
       throw new IllegalArgumentException("Unknown DownloadState: " + state);
    }
}
