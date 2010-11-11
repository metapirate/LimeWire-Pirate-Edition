package org.limewire.bittorrent;

import java.net.URI;
import java.util.List;
import java.util.Set;


/** Defines an interface from which all data about a .torrent file 
 * can be retrieved. 
 */
public interface BTData {

    /** A structure for storing information about files within the .torrent. */
    public static class BTFileData {
        private final Long length;
        private final String path;
        
        /**
         * public for testing
         */
        public BTFileData(Long length, String path) {
            this.length = length;
            this.path = path;
        }
    
        public Long getLength() {
            return length;
        }
    
        /**
         * Returns the path to the file relative to the root 
         * directory of the torrent in unix form. This form work for
         * appending the path to urls.
         * 
         *  path/to/file instead of path\to\file
         */
        public String getPath() {
            return path;
        }
    }

    /**
     * @return at least one valid tracker uri 
     */
    public List<URI> getTrackerUris();
    
    /**
     * Returns a list of potential webseed addresses.
     */
    public URI[] getWebSeeds();

    /**
     * @return a list of subfiles of this torrent if multiple files, or null 
     * if the torrent is a single file. 
     * <p>
     * From <a href="http://wiki.theory.org/BitTorrentSpecification#Info_in_Multiple_File_Mode">
     * BitTorrentSpecification wiki</a>:
     * <p>
     * "For the case of the multi-file mode, the info dictionary contains the 
     * following structure:
     * <p>
     * name: the filename of the directory in which to store all the files. 
     * This is purely advisory. (string)
     * <p>
     * files: a list of dictionaries, one for each file. Each dictionary in 
     * this list contains the following keys:
     * <ul>
     * <li> length: length of the file in bytes (integer)
     * <li>md5sum: (optional) a 32-character hexadecimal string corresponding 
     * to the MD5 sum of the file. This is not used by BitTorrent at all, but 
     * it is included by some programs for greater compatibility.
     * <li>path: a list containing one or more string elements that together 
     * represent the path and filename. Each element in the list corresponds to 
     * either a directory name or (in the case of the final element) the 
     * filename. For example, a the file "dir1/dir2/file.ext" would consist of 
     * three string elements: "dir1", "dir2", and "file.ext". This is 
     * encoded as a bencoded list of strings such as l4:dir14:dir28:file.exte" 
     */
    public List<BTData.BTFileData> getFiles();

    public Set<String> getFolders();

    /**
     * A private tracker (see http://en.wikipedia.org/wiki/BitTorrent_tracker#Private_trackers) 
     * restricts who can use the tracker.
     * @return whether or not the torrent is private
     */
    public boolean isPrivate();

    /**
     * An hash is a string of alphanumeric characters in the .torrent file 
     * that the client uses to verify the data that is being transferred.
     */
    public byte[] getInfoHash();

    /**
     * @return the length of the torrent if one file, or null if multiple files
     */
    public Long getLength();

    /**
     * @return the name of the torrent file (if one file) or parent folder (if multiple files)
     */
    public String getName();

    /**
     * A torrent is divided up into equal size chunks called pieces. 
     */
    public Long getPieceLength();

    /**
     * All the pieces of the divided up torrent as one array. 
     */
    public byte[] getPieces();

    /**
     * Empties the array of pieces (divided up torrents).
     */
    public void clearPieces();

}