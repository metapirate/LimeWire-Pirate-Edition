package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.limewire.ui.swing.util.DNDUtils;


public class LocalFileTransferable implements Transferable {
    
    private File[] files;
    
    public LocalFileTransferable(File[] files){
        this.files = files;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if( flavor.equals(DNDUtils.URIFlavor)) {
            String seperator = System.getProperty("line.separator"); 
            StringBuffer lines = new StringBuffer();
            for(File file : files) {
                lines.append(file.toURI().toString());
                lines.append(seperator);
            }
            lines.append(seperator);
            return lines.toString();
        } else if(flavor.equals(DataFlavor.javaFileListFlavor)) {
            return Arrays.asList(files);
        }
        
        throw new UnsupportedFlavorException(flavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return DNDUtils.getFileFlavors();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DNDUtils.isFileFlavor(flavor);
    }

}
