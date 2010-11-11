package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

class FixStalledDownloadsAction extends AbstractAction {

    private final DownloadMediator downloadMediator;
    
    @Inject
    public FixStalledDownloadsAction(DownloadMediator downloadMediator) {
        super(I18n.tr("Fix Stalled Downloads"));
        this.downloadMediator = downloadMediator;
    }

    @Inject
    public void register(DownloadMediator downloadMediator) {
        EventList<DownloadItem> stalledList = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), 
                new DownloadStateMatcher(DownloadState.STALLED));
        stalledList.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                setEnabled(listChanges.getSourceList().size()  != 0);
            }
        });       
        setEnabled(stalledList.size() != 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        downloadMediator.fixStalled();
    }
}
