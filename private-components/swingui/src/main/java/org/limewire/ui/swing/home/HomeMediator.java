package org.limewire.ui.swing.home;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;

import com.google.inject.Inject;

@LazySingleton
public class HomeMediator implements NavMediator<HomePanel> {

    public static final String NAME = "Home";
    
    private HomePanel home;
    
    @Inject
    public HomeMediator(HomePanel homePanel) {
        this.home = homePanel;
    }
    
    @Override
    public HomePanel getComponent() {
        return home;
    }
}
