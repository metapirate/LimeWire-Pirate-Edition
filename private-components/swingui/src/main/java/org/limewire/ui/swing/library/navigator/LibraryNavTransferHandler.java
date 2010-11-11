package org.limewire.ui.swing.library.navigator;

import java.awt.Point;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.warnings.LibraryWarningController;

import com.google.inject.Inject;

class LibraryNavTransferHandler extends LocalFileListTransferHandler {

    private static final Log LOG = LogFactory.getLog(LibraryNavTransferHandler.class);

    private LocalFileList localFileList = null;

    @Inject
    public LibraryNavTransferHandler(LibraryWarningController librarySupport) {
        super(librarySupport);
    }

    @Override
    public boolean canImport(TransferSupport info) {
        try {
            if (info.getComponent() instanceof LibraryNavigatorTable) {
                LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info
                        .getComponent();
                DropLocation dropLocation = info.getDropLocation();
                Point point = dropLocation.getDropPoint();

                int column = libraryNavigatorTable.columnAtPoint(point);
                int row = libraryNavigatorTable.rowAtPoint(point);
                if (column < 0 || row < 0) {
                    return false;
                }

                LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(
                        row, column);
                if (libraryNavItem != null) {
                    this.localFileList = libraryNavItem.getLocalFileList();
                    return super.canImport(info);
                }
            }

            return false;
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

    @Override
    public boolean importData(TransferSupport info) {
        try {
            if (info.getComponent() instanceof LibraryNavigatorTable && info.isDrop()) {
                LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info
                        .getComponent();
                DropLocation dropLocation = info.getDropLocation();
                Point point = dropLocation.getDropPoint();
                int column = libraryNavigatorTable.columnAtPoint(point);
                int row = libraryNavigatorTable.rowAtPoint(point);

                if (column < 0 || row < 0) {
                    return false;
                }

                LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(
                        row, column);
                if (libraryNavItem != null) {
                    this.localFileList = libraryNavItem.getLocalFileList();
                    return super.importData(info);
                }
            }

            return false;
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

    @Override
    public LocalFileList getLocalFileList() {
        return localFileList;
    }

    @Override
    protected List<File> getSelectedFiles() {
        return Collections.emptyList();
    }
}
