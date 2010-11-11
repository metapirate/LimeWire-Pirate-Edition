package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.apache.commons.logging.Log;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.warnings.LibraryWarningController;

/**
 * An abstract class for transferring {@link LocalFileItem} through a
 * {@link TransferHandler}.
 */
public abstract class LocalFileListTransferHandler extends TransferHandler {
    private static final Log LOG = LogFactory.getLog(LocalFileListTransferHandler.class);

    private final WeakHashMap<Transferable, Map<LocalFileList, Boolean>> canImportCache = new WeakHashMap<Transferable, Map<LocalFileList, Boolean>>();

    private final LibraryWarningController librarySupport;

    public LocalFileListTransferHandler(LibraryWarningController librarySupport) {
        this.librarySupport = librarySupport;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        List<File> files = getSelectedFiles();
        if (!files.isEmpty()) {
            return new LocalFileListTransferable(getLocalFileList(), files.toArray(new File[files
                    .size()]));
        } else {
            return null;
        }
    }

    /** Returns all files that want to be transferred. */
    protected abstract List<File> getSelectedFiles();

    /** Returns the LocalFileList that items should be transfered to or from. */
    protected abstract LocalFileList getLocalFileList();

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        try {
            Transferable t = info.getTransferable();
            LocalFileList localFileList = getLocalFileList();
            Map<LocalFileList, Boolean> canImportMap = canImportCache.get(t);
            if (canImportMap == null) {
                canImportMap = new HashMap<LocalFileList, Boolean>();
                canImportCache.put(t, canImportMap);
            }

            Boolean canImport = canImportMap.get(localFileList);
            if (canImport == null) {
                canImport = canImportInternal(info);
                canImportMap.put(localFileList, canImport);
            }
            return canImport;
        } catch (RuntimeException e) {
            // catching and logging since internal sun code will just eat the
            // exception, its better that we can at least turn on logging to
            // find out what is going on.
            LOG.error("Error importing drop data.", e);
            throw e;
        } catch (Error e) {
            LOG.error("Error importing drop data.", e);
            throw e;
        }
    }

    private boolean canImportInternal(TransferHandler.TransferSupport info) {
        if (getLocalFileList() == null || !DNDUtils.containsFileFlavors(info)) {
            return false;
        }

        // don't allow dragging and dropping ot the same list.
        Transferable t = info.getTransferable();
        if (t.isDataFlavorSupported(LocalFileListTransferable.LOCAL_FILE_LIST_DATA_FLAVOR)) {
            try {
                LocalFileList localFileList = (LocalFileList) t
                        .getTransferData(LocalFileListTransferable.LOCAL_FILE_LIST_DATA_FLAVOR);
                if (localFileList == getLocalFileList()) {
                    return false;
                }
            } catch (IOException e) {
                LOG.debug("Error get Trasferable contents.", e);
            } catch (UnsupportedFlavorException e) {
                LOG.debug("Error get Trasferable contents.", e);
            }
        }

        List<File> files = Collections.emptyList();
        if (DNDUtils.containsFileFlavors(info)) {
            try {
                files = Arrays.asList(DNDUtils.getFiles(t));
            } catch (Throwable e) {
                LOG.debug("Error get Trasferable contents.", e);
                return true;
            }
        }

        LocalFileList localFileList = getLocalFileList();

        for (File file : files) {
            if (localFileList.isFileAllowed(file) || localFileList.isDirectoryAllowed(file)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        try {
            if (!info.isDrop()) {
                return false;
            }

            Transferable t = info.getTransferable();
            canImportCache.remove(t);

            List<File> files = Collections.emptyList();
            if (DNDUtils.containsFileFlavors(info)) {
                try {
                    files = Arrays.asList(DNDUtils.getFiles(t));
                } catch (Throwable failed) {
                    return false;
                }
            }

            handleFiles(files);
            return true;
        } catch (RuntimeException e) {
            // catching and logging since internal sun code will just eat the
            // exception, its better that we can at least turn on logging to
            // find out what is going on.
            LOG.error("Error importing drop data.", e);
            throw e;
        } catch (Error e) {
            LOG.error("Error importing drop data.", e);
            throw e;
        }
    }

    private void handleFiles(final List<File> files) {
        LocalFileList localFileList = getLocalFileList();
        librarySupport.addFiles(localFileList, files);
    }
}
