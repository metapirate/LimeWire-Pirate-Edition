package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;

public class LocalFileListTransferable extends LocalFileTransferable {

    public static final DataFlavor LOCAL_FILE_LIST_DATA_FLAVOR = new DataFlavor(LocalFileList.class, "Local File List Transferable");
    
    private final LocalFileList localFileList;

    public LocalFileListTransferable(LocalFileList localFileList, File[] files) {
        super(files);
        this.localFileList = localFileList;
    }

    public LocalFileList getLocalFileList() {
        return localFileList;
    }
    
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if(flavor.equals(LOCAL_FILE_LIST_DATA_FLAVOR)){
            return localFileList;
        } else {
            return super.getTransferData(flavor);
        }
    }
    
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        List<DataFlavor> dataFlavors = new ArrayList<DataFlavor>(Arrays.asList(super.getTransferDataFlavors()));
        dataFlavors.add(LOCAL_FILE_LIST_DATA_FLAVOR);
        return dataFlavors.toArray(new DataFlavor[0]);
    }
    
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(LOCAL_FILE_LIST_DATA_FLAVOR) || super.isDataFlavorSupported(flavor);
    }

}
