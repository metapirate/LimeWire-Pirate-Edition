package org.limewire.ui.swing.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.TransferHandler.TransferSupport;

import org.limewire.util.OSUtils;
import org.limewire.util.URIUtils;

/**
 * Static helper class with DND tasks that provides methods for handling URI and
 * file drops and also provides default transfer handlers.
 */
public class DNDUtils {
    public static final DataFlavor URIFlavor = createURIFlavor();
    
    private static final List<DataFlavor> fileDataFlavors = new ArrayList<DataFlavor>();
    
    static {
        if(OSUtils.isLinux()) {
            //the uri flavor does not play well with itunes on windows
            fileDataFlavors.add(URIFlavor);
        }
        fileDataFlavors.add(DataFlavor.javaFileListFlavor);
    }

    /**
     * Returns array of URIs extracted from transferable.
     * 
     * @throws UnsupportedFlavorException
     * @throws IOException
     */
    public static URI[] getURIs(Transferable transferable) throws UnsupportedFlavorException,
            IOException {

        String lines = (String) transferable.getTransferData(URIFlavor);
        StringTokenizer st = new StringTokenizer(lines, System.getProperty("line.separator"));
        ArrayList<URI> uris = new ArrayList<URI>();
        while (st.hasMoreTokens()) {
            String line = st.nextToken().trim();
            if (line.length() == 0) {
                continue;
            }
            try {
                URI uri = URIUtils.toURI(line);
                uris.add(uri);
            } catch (URISyntaxException e) {
            }
        }
        return uris.toArray(new URI[uris.size()]);
    }

    /**
     * Checks for {@link DataFlavor#javaFileListFlavor} and
     * {@link DNDUtils#URIFlavor} for Unix systems.
     */
    public static boolean containsFileFlavors(TransferSupport transferSupport) {
        for(DataFlavor dataFlavor : getFileFlavors()) {
            if(transferSupport.isDataFlavorSupported(dataFlavor)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns array of all flavors we consider to be file flavors. 
     */
    public static DataFlavor[] getFileFlavors() {
        return fileDataFlavors.toArray(new DataFlavor[]{}); 
    }

    /**
     * Extracts the array of files from a transferable
     * 
     * @return an empty array if the transferable does not contain any data that
     *         can be interpreted as a list of files
     */
    @SuppressWarnings("unchecked")
    public static File[] getFiles(Transferable transferable) throws UnsupportedFlavorException,
            IOException {
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return ((List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor))
                    .toArray(new File[0]);
        } else if (transferable.isDataFlavorSupported(URIFlavor)) {
            return getFiles(getURIs(transferable));
        }
        return new File[0];
    }

    /**
     * Returns array of files for URIs that denote local paths.
     * 
     * @return empty array if no URI denotes a local file
     */
    public static File[] getFiles(URI[] uris) {
        ArrayList<File> files = new ArrayList<File>(uris.length);
        for (URI uri : uris) {
            String scheme = uri.getScheme();
            if (uri.isAbsolute() && scheme != null && scheme.equalsIgnoreCase("file")) {
                String path = uri.getPath();
                files.add(new File(path));
            }
        }
        return files.toArray(new File[files.size()]);
    }

    private static DataFlavor createURIFlavor() {
        try {
            return new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    /**
     * Returns true if the supplied flavor is what we consider to be a file flavor. 
     */
    public static boolean isFileFlavor(DataFlavor flavor) {
       return fileDataFlavors.contains(flavor);
    }

}
