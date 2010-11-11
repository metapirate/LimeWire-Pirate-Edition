package org.limewire.ui.swing.search;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.FriendPresence;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RemoteHostMenuFactory {
    
    private final Provider<FriendPresenceActions> remoteHostActions;
    
    @Inject
    public RemoteHostMenuFactory(final Provider<FriendPresenceActions> remoteHostActions){
        this.remoteHostActions = remoteHostActions;
    }
    
    /**
     * @param allHosts the hosts to add to the menu - duplicates will be ignored
     */
    public JMenu createBrowseMenu(Collection<RemoteHost> allHosts){

        //list of unique presenceIds used to check for duplicate hosts
        final Set<RemoteHost> uniqueHosts = new TreeSet<RemoteHost>(new RemoteHostComparator());
        uniqueHosts.addAll(allHosts);
        
        JMenu browse = new JMenu(I18n.tr("Browse Files"));
        //hiding browse all users until memory issues are cleared up
//        if(uniqueHosts.size() > 1){
//            browse.add(new AbstractAction("All Users") {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    remoteHostActions.get().viewLibrariesOf(Collections2.transform(uniqueHosts, new Function<RemoteHost, FriendPresence>() {
//                        @Override
//                        public FriendPresence apply(RemoteHost from) {
//                            return from.getFriendPresence();
//                        }
//                    }));
//                }
//            });
//            browse.addSeparator();
//        }
        
        for (RemoteHost host : uniqueHosts) {
            browse.add(new BrowseAction(host));
        }                
        
        return browse;
    }
    
    private class BrowseAction extends AbstractAction {    
        private FriendPresence presence; 
        
        public BrowseAction(RemoteHost host) {     
            super(host.getFriendPresence().getFriend().getRenderName());
            this.presence = host.getFriendPresence(); 
            setEnabled(host.isBrowseHostEnabled());                
        }
        
        @Override                
        public void actionPerformed(ActionEvent e) {                    
            remoteHostActions.get().viewLibrariesOf(Collections.singleton(presence));               
        }            
    }    
    
    private static class RemoteHostComparator implements Comparator<RemoteHost> {

        @Override
        public int compare(RemoteHost o1, RemoteHost o2) { 
            return o1.getFriendPresence().getPresenceId().compareTo(o2.getFriendPresence().getPresenceId());
        }
        
    }
}
