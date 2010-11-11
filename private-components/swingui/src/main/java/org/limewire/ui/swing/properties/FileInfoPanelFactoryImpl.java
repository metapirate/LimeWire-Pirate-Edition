package org.limewire.ui.swing.properties;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class FileInfoPanelFactoryImpl implements FileInfoPanelFactory {

    private final Provider<IconManager> iconManager;
    private final Provider<MagnetLinkFactory> magnetLinkFactory;
    private final Provider<CategoryIconManager> categoryIconManager;
    private final Provider<PropertyDictionary> propertyDictionary;
    private final Provider<SpamManager> spamManager;
    private final Provider<SharedFileListManager> sharedFileListManager;
    private final Provider<MetaDataManager> metaDataManager;
    private final Provider<LibraryMediator> libraryMediator;
    private final Provider<LibraryManager> libraryManager;
    private final Provider<BarPainterFactory> barPainterFactory;
    private final Provider<TableDecorator> tableDecorator;
    private final @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings;
    
    @Inject
    public FileInfoPanelFactoryImpl(Provider<IconManager> iconManager, Provider<MagnetLinkFactory> magnetLinkFactory, 
            Provider<CategoryIconManager> categoryIconManager,
            Provider<PropertyDictionary> propertyDictionary, Provider<SpamManager> spamManager,
            Provider<SharedFileListManager> sharedFileListManager, Provider<MetaDataManager> metaDataManager,
            Provider<LibraryMediator> libraryMediator, Provider<LibraryManager> libraryManager,
            Provider<BarPainterFactory> barPainterFactory, Provider<TableDecorator> tableDecorator,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.iconManager = iconManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.categoryIconManager = categoryIconManager;
        this.propertyDictionary = propertyDictionary;
        this.spamManager = spamManager;
        this.sharedFileListManager = sharedFileListManager;
        this.metaDataManager = metaDataManager;
        this.libraryMediator = libraryMediator;
        this.libraryManager = libraryManager;
        this.barPainterFactory = barPainterFactory;
        this.tableDecorator = tableDecorator;
        this.torrentSettings = torrentSettings;
    }
    
    @Override
    public FileInfoBittorrentPanel createBittorentPanel(Torrent torrent) {
        return new FileInfoBittorrentPanel(torrent);
    }
    
    @Override
    public FileInfoPanel createOverviewPanel(Torrent torrent) {
        return new FileInfoBittorrentOverviewPanel(torrent);
    }

    @Override
    public FileInfoPanel createGeneralPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoGeneralPanel(type, propertiableFile, propertyDictionary.get(), spamManager.get(), metaDataManager.get(), libraryMediator.get());
    }
    
    @Override
    public FileInfoPanel createLimitsPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoLimitsPanel(propertiableFile, torrentSettings);
    }

    @Override
    public FileInfoPanel createOverviewPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoOverviewPanel(type, propertiableFile, iconManager, magnetLinkFactory.get(), categoryIconManager.get(), libraryManager.get(), barPainterFactory.get());
    }

    @Override
    public FileInfoPanel createSharingPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoSharingPanel(type, propertiableFile, sharedFileListManager.get());
    }

    @Override
    public FileInfoPanel createTrackersPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoTrackersPanel(propertiableFile, tableDecorator.get());
    }
    
    @Override
    public FileInfoPanel createTransferPanel(FileInfoType type, PropertiableFile propertiableFile) {
        return new FileInfoTransfersPanel(type, propertiableFile, tableDecorator.get());
    }

    @Override
    public FileInfoPanel createPiecesPanel(FileInfoType type, DownloadItem propertiableFile) {
        return new FileInfoPiecesPanel(propertiableFile);
    }
    
}
