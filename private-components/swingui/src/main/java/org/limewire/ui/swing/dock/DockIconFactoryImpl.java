package org.limewire.ui.swing.dock;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.util.OSUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Creates a DockIcon instance for the target
 * platform.
 */
@Singleton
public class DockIconFactoryImpl implements DockIconFactory
{
    private final ServiceRegistry registry;
    private final DownloadMediator downloadMediator;
    private DockIcon dockIcon;
    
    @Inject
    public DockIconFactoryImpl (ServiceRegistry registry, DownloadMediator downloadMediator) {
        this.registry = registry;
        this.downloadMediator = downloadMediator;
    }
    
    
    public DockIcon createDockIcon () {      
        if(dockIcon == null) {
            if (OSUtils.isMacOSX()) {
                dockIcon = new DockIconMacOSXImpl();
                ((DockIconMacOSXImpl)dockIcon).register(registry);
                registry.start("UIHack");
                setupListener();
            } else {
                dockIcon = new DockIconNoOpImpl();
            }
        }
        return dockIcon;
    }
    
    private void setupListener() {
        final EventList<DownloadItem> list = GlazedListsFactory.filterList(downloadMediator.getDownloadList(), new DownloadCountListener());
        list.addListEventListener(new ListEventListener<DownloadItem>(){
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                dockIcon.draw(list.size());
            }
        });
    }
    
    /**
     * Matcher that returns all non-stalled, non-completed and non-cancelled downloads.
     */
    private class DownloadCountListener implements Matcher<DownloadItem> {

        @Override
        public boolean matches(DownloadItem item) {
            if (item == null)
                return false;

            DownloadState state = item.getState();
            return state != DownloadState.CANCELLED && 
                    state != DownloadState.ERROR && !state.isFinished();
        }
    }

}
