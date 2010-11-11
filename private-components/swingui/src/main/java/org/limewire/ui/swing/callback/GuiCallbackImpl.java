package org.limewire.ui.swing.callback;

import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;

import org.jdesktop.application.Application;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.inject.EagerSingleton;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.properties.FileInfoPanelFactory;
import org.limewire.ui.swing.properties.TorrentDownloadSelector;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@EagerSingleton
public class GuiCallbackImpl implements GuiCallback {
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;
    private final Provider<MagnetHandler> magnetHandler;
    private final Provider<FileInfoPanelFactory> fileInfoPanelFactory;

    @Inject
    public GuiCallbackImpl(Provider<DownloadExceptionHandler> downloadExceptionHandler,
            Provider<MagnetHandler> magnetHandler,
            Provider<FileInfoPanelFactory> fileInfoPanelFactory) {
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.magnetHandler = magnetHandler;
        this.fileInfoPanelFactory = fileInfoPanelFactory;
    }

    @Inject
    void register(GuiCallbackService guiCallbackService) {
        guiCallbackService.setGuiCallback(this);
    }

    @Override
    public void handleDownloadException(DownloadAction downLoadAction, DownloadException e,
            boolean supportsNewSaveDir) {
        downloadExceptionHandler.get().handleDownloadException(downLoadAction, e,
                supportsNewSaveDir);
    }

    @Override
    public void restoreApplication() {
        SwingUtils.invokeNowOrLater(new Runnable() {
            @Override
            public void run() {
                ActionMap actionMap = Application.getInstance().getContext().getActionMap();
                Action restoreView = actionMap.get("restoreView");
                restoreView.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                "restoreView"));
            }
        });
    }

    @Override
    public String translate(String s) {
        return I18n.tr(s);
    }

    @Override
    public void handleMagnet(MagnetLink magnetLink) {
        magnetHandler.get().handleMagnet(magnetLink);
    }

    @Override
    public boolean promptUserQuestion(final String marktr) {
        final MultiLineLabel label = new MultiLineLabel(I18n.tr(marktr), 400);
        final String title = I18n.tr("Warning");
        final AtomicInteger result = new AtomicInteger(JOptionPane.YES_OPTION);
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
                result.set(FocusJOptionPane.showConfirmDialog(
                        GuiUtils.getMainFrame(), label, title,
                        JOptionPane.YES_NO_OPTION));
            }
        });
        return result.get() == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean promptTorrentFilePriorities(final Torrent torrent) {
        final AtomicInteger result = new AtomicInteger(JOptionPane.OK_OPTION);
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
                result.set(TorrentDownloadSelector.showBittorrentSelector(torrent,
                        fileInfoPanelFactory.get()));
            }
        });
        return result.get() == JOptionPane.OK_OPTION;
    }

    /**
     * Asks the user whether to continue with a torrent download that contains
     * files with banned extensions.
     * @return true if the download should continue.
     */
    @Override
    public boolean promptAboutTorrentWithBannedExtensions(Torrent torrent,
            Set<String> bannedExtensions) {
        String extensions = StringUtils.explode(bannedExtensions, ", ");
        String warning = I18n.tr("This torrent contains files with the following extensions, which LimeWire is configured not to download: {0}.", extensions);
        String prompt = I18n.tr("Downloading this torrent could damage your computer. Are you sure you want to continue?");
        final MultiLineLabel label = new MultiLineLabel(warning + "\n\n" + prompt, 400);
        final String title = I18n.tr("Warning: {0}", torrent.getName());
        final AtomicInteger result = new AtomicInteger(JOptionPane.YES_OPTION);
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
                result.set(FocusJOptionPane.showConfirmDialog(
                        GuiUtils.getMainFrame(), label, title,
                        JOptionPane.YES_NO_OPTION));
            }
        });
        return result.get() == JOptionPane.YES_OPTION;
    }
    
    /**
     * Asks the user whether to continue with a torrent download if the torrent file could not be scanned.
     * @return true if the download should continue.
     */
    @Override
    public boolean promptAboutTorrentDownloadWithFailedScan() {
        String warning = I18n.tr("The torrent file download could not be virus scanned.");
        String prompt = I18n.tr("Do you want to continue and download the torrent?");
        final MultiLineLabel label = new MultiLineLabel(warning + "\n\n" + prompt, 400);
        final String title = I18n.tr("Warning");
        final AtomicInteger result = new AtomicInteger(JOptionPane.YES_OPTION);
        SwingUtils.invokeNowOrWait(new Runnable() {
            @Override
            public void run() {
                result.set(FocusJOptionPane.showConfirmDialog(
                        GuiUtils.getMainFrame(), label, title,
                        JOptionPane.YES_NO_OPTION));
            }
        });
        return result.get() == JOptionPane.YES_OPTION;
    }
}
