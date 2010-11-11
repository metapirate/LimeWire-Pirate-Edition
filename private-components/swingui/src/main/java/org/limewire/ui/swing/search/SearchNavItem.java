package org.limewire.ui.swing.search;

import org.limewire.ui.swing.nav.NavItem;

public interface SearchNavItem extends NavItem {

    void sourceCountUpdated(int resultCount);

}
