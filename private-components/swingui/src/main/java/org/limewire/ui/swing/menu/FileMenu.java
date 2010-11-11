package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.event.MenuListener;

import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

class FileMenu extends MnemonicMenu implements DelayedMenuItemCreator {
    
    private final JMenu recentDownloadsMenu;
    
    private Provider<OpenFileAction> openFileActionProvider; 
    private Provider<OpenLinkAction> openLinkActionProvider;
    private Provider<AddFileAction> addFileActionProvider;
    private Provider<FixStalledDownloadsAction> fixStalledDownloadsActionProvider;
    private Provider<ExitAfterTransferAction> exitAfterTransferActionProvider;
    private Provider<ExitAction> exitActionProvider;
    
    @Inject
    public FileMenu(RecentDownloadsMenu recentDownloadsMenu,
            Provider<OpenFileAction> openFileActionProvider, 
            Provider<OpenLinkAction> openLinkActionProvider,
            Provider<AddFileAction> addFileActionProvider, 
            Provider<FixStalledDownloadsAction> fixStalledDownloadsActionProvider,
            Provider<ExitAfterTransferAction> exitAfterTransferActionProvider,
            Provider<ExitAction> exitActionProvider) {
        
        super(I18n.tr("&File"));

        this.recentDownloadsMenu = recentDownloadsMenu;
        this.openFileActionProvider = openFileActionProvider; 
        this.openLinkActionProvider = openLinkActionProvider; 
        this.addFileActionProvider = addFileActionProvider;
        this.fixStalledDownloadsActionProvider = fixStalledDownloadsActionProvider;
        this.exitAfterTransferActionProvider = exitAfterTransferActionProvider;
        this.exitActionProvider = exitActionProvider;
    }
    
    @Override
    public void createMenuItems() {
        add(openFileActionProvider.get());
        add(openLinkActionProvider.get());
        add(recentDownloadsMenu);
        addSeparator();
        add(addFileActionProvider.get());
        addSeparator();
        add(fixStalledDownloadsActionProvider.get());

        // Add exit actions.
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(exitAfterTransferActionProvider.get());
            add(exitActionProvider.get());
        }        
    }
    
    @Override
    public void addMenuListener(MenuListener listener) {
        super.addMenuListener(listener);
        recentDownloadsMenu.addMenuListener(listener);
    }
}
