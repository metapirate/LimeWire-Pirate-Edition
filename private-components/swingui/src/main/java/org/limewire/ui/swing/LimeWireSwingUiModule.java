package org.limewire.ui.swing;

import org.limewire.ui.swing.browser.LimeWireUiBrowserModule;
import org.limewire.ui.swing.callback.GuiCallbackImpl;
import org.limewire.ui.swing.components.LimeWireUiComponentsModule;
import org.limewire.ui.swing.dock.LimeWireUiDockModule;
import org.limewire.ui.swing.downloads.LimeWireUiDownloadsModule;
import org.limewire.ui.swing.friends.LimeWireUiFriendsModule;
import org.limewire.ui.swing.images.LimeWireUiImagesModule;
import org.limewire.ui.swing.library.LimeWireUiLibraryModule;
import org.limewire.ui.swing.mainframe.LimeWireUiMainframeModule;
import org.limewire.ui.swing.nav.LimeWireUiNavModule;
import org.limewire.ui.swing.options.LimeWireUiOptionsModule;
import org.limewire.ui.swing.painter.LimeWireUiPainterModule;
import org.limewire.ui.swing.player.LimeWireUiPlayerModule;
import org.limewire.ui.swing.properties.LimeWireUiPropertiesModule;
import org.limewire.ui.swing.search.LimeWireUiSearchModule;
import org.limewire.ui.swing.search.resultpanel.LimeWireUiSearchResultPanelModule;
import org.limewire.ui.swing.statusbar.LimeWireUiStatusbarModule;
import org.limewire.ui.swing.table.LimeWireUiTableModule;
import org.limewire.ui.swing.tray.LimeWireUiTrayModule;
import org.limewire.ui.swing.upload.LimeWireUiUploadModule;
import org.limewire.ui.swing.util.LimeWireUiUtilModule;
import org.limewire.ui.swing.wizard.LimeWireUiWizardModule;

import com.google.inject.AbstractModule;

public class LimeWireSwingUiModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(GuiCallbackImpl.class);
        install(new LimeWireUiBrowserModule());
        install(new LimeWireUiComponentsModule());
        install(new LimeWireUiDockModule());
        install(new LimeWireUiDownloadsModule());
        install(new LimeWireUiFriendsModule());
        install(new LimeWireUiImagesModule());
        install(new LimeWireUiLibraryModule());
        install(new LimeWireUiMainframeModule());
        install(new LimeWireUiNavModule());
        install(new LimeWireUiOptionsModule());
        install(new LimeWireUiPainterModule());
        install(new LimeWireUiPlayerModule());
        install(new LimeWireUiPropertiesModule());
        install(new LimeWireUiSearchModule());
        install(new LimeWireUiSearchResultPanelModule());
        install(new LimeWireUiStatusbarModule());
        install(new LimeWireUiTableModule());
        install(new LimeWireUiTrayModule());
        install(new LimeWireUiUploadModule());
        install(new LimeWireUiUtilModule());
        install(new LimeWireUiWizardModule());
    }
}