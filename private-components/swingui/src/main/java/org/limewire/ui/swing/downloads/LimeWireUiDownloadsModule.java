package org.limewire.ui.swing.downloads;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.FinishedDownloadSelected;
import org.limewire.ui.swing.downloads.table.LimeWireUiDownloadsTableModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class LimeWireUiDownloadsModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new LimeWireUiDownloadsTableModule());
    }
    
    
    @Provides @FinishedDownloadSelected List<File> selectedFiles(MainDownloadPanel downloadPanel) {
        List<DownloadItem> items = downloadPanel.getSelectedDownloadItems();
        List<File> files = new ArrayList<File>();
        
        for(DownloadItem item : items){
            DownloadState state = item.getState();
            if(state == DownloadState.DONE ||
                    state == DownloadState.SCAN_FAILED){
                files.addAll(item.getCompleteFiles());
            }
        }
        
        return files;
    } 

}
