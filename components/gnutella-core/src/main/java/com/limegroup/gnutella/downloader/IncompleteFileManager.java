package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.collection.Comparators;
import org.limewire.collection.Range;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.InvalidDataException;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.library.IncompleteFileCollection;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.tigertree.HashTreeCache;

/** 
 * A repository of temporary filenames.  Gives out file names for temporary
 * files, ensuring that two duplicate files always get the same name.  This
 * enables smart resumes across hosts.  Also keeps track of the blocks 
 * downloaded, for smart downloading purposes.  <b>Thread safe.</b><p>
 */
@Singleton
public class IncompleteFileManager  {

    /** The delimiter to use between the size and a real name of a temporary
     * file.  To make it easier to break the temporary name into its constituent
     * parts, this should not contain a number. */
    static final String SEPARATOR="-";
    /** The prefix added to preview copies of incomplete files. */
    public static final String PREVIEW_PREFIX="Preview-";
    public static final String INCOMPLETE_PREFIX="T-";
    
    /**
     * A mapping from incomplete files (File) to the blocks of the file stored
     * on disk (VerifyingFile).  Needed for resumptive smart downloads.
     * INVARIANT: all blocks disjoint, no two intervals can be coalesced into
     * one interval.  Note that blocks are not sorted; there are typically few
     * blocks so performance isn't an issue.
     */
    private final Map<File, VerifyingFile> blocks=
        new TreeMap<File, VerifyingFile>(Comparators.fileComparator());
    /**
     * Bijection between sha1 hashes (URN) and incomplete files (File).  This is
     * used to ensure that any two RemoteFileDesc with the same hash get the
     * same incomplete file, regardless of name.  The inverse of this map is
     * used to get the hash of an incomplete file for query-by-hash and
     * resuming.  Note that the hash is that of the desired completed file, not
     * that of the incomplete file.<p>
     * 
     * Entries are added to hashes before the temp file is actually created on
     * disk.  For this reason, there can be files in the value set of hashes
     * that are not in the key set of blocks.  These entries are not serialized
     * to disk in the downloads.dat file.  Similarly there may be files in the
     * key set of blocks that are not in the value set of hashes.  This happens
     * if we received RemoteFileDesc's without hashes, or when loading old
     * downloads.dat files without hash info.       
     * <p>
     * INVARIANT: the range (value set) of hashes contains no duplicates.  <p>
     * INVARIANT: for all keys k in hashes, k.isSHA1() 
     */
    private final Map<URN, File> hashes = new HashMap<URN, File>();
    
    private final Provider<Library> library;
    private final Provider<IncompleteFileCollection> incompleteFileCollection;
    private final Provider<HashTreeCache> tigerTreeCache;
    private final VerifyingFileFactory verifyingFileFactory;
    private final Provider<TorrentManager> torrentManager;
    
    @Inject
    public IncompleteFileManager(
            Provider<Library> library,
            Provider<IncompleteFileCollection> incompleteFileCollection,
            Provider<HashTreeCache> tigerTreeCache,
            VerifyingFileFactory verifyingFileFactory, Provider<TorrentManager> torrentManager) {
        this.library = library;
        this.incompleteFileCollection = incompleteFileCollection;
        this.tigerTreeCache = tigerTreeCache;
        this.verifyingFileFactory = verifyingFileFactory;
        this.torrentManager = torrentManager;
    }
    
    /**
     * Removes entries in this for which there is no file on disk.
     * 
     * @return true iff any entries were purged 
     */
    public synchronized boolean purge() {
        boolean ret=false;
        //Remove any blocks for which the file doesn't exist.
        for (Iterator<File> iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file = iter.next();
            if (!file.exists() ) {
                ret=true;
                library.get().remove(file);
                file.delete();  //always safe to call; return value ignored
                iter.remove();
            }
        }
        return ret;
    }
    
     /** 
      * Deletes incomplete files more than INCOMPLETE_PURGE_TIME days old from disk
      * Then removes entries in this for which there is no file on disk.
      * 
      * @param activeFiles which files are currently being downloaded.
      * @return true iff any entries were purged
      */
    public synchronized boolean initialPurge(Collection<File> activeFiles) {
        //Remove any files that are old.
        boolean ret = false;
        for (Iterator<File> iter=blocks.keySet().iterator(); iter.hasNext(); ) {
            File file = iter.next();
            try {
                file = FileUtils.getCanonicalFile(file);
            } catch (IOException iox) {
                file = file.getAbsoluteFile();
            }
            if (!file.exists() || (isOld(file) && !activeFiles.contains(file))) {
                ret=true;
                library.get().remove(file);
                file.delete();
                iter.remove();
            }
        }
        for (Iterator<File> iter=hashes.values().iterator(); iter.hasNext(); ) {
            File file = iter.next();
            if (!file.exists()) {
                iter.remove();
                ret=true;
            }
        }
        
        return ret;
    }

    /** Returns true iff file is "too old". */
    private static final boolean isOld(File file) {
        //Inlining this method allows some optimizations--not that they matter.
        long days=SharingSettings.INCOMPLETE_PURGE_TIME.getValue();
        //Back up a couple days. 
        //24 hour/day * 60 min/hour * 60 sec/min * 1000 msec/sec
        long purgeTime=System.currentTimeMillis()-days*24l*60l*60l*1000l;
        return file.lastModified() < purgeTime;            
    }


    /*
     * Returns true if both rfd "have the same content".  Currently
     * rfd1~=rfd2 iff either of the following conditions hold:
     * 
     * <ul>
     * <li>Both files have the same hash, i.e., 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn().  Note that this (almost)
     *     always means that rfd1.getSize()==rfd2.getSize(), though rfd1 and
     *     rfd2 may have different names.
     * <li>Both files have the same name and size and don't have conflicting
     *     hashes, i.e., rfd1.getName().equals(rfd2.getName()) &&
     *     rfd1.getSize()==rfd2.getSize() && (rfd1.getSHA1Urn()==null ||
     *     rfd2.getSHA1Urn()==null || 
     *     rfd1.getSHA1Urn().equals(rfd2.getSHA1Urn())).
     * </ul>
     * Note that the second condition allows risky resumes, i.e., resumes when 
     * one (or both) of the files doesn't have a hash.  
     *
     * @see getFile
     */
    static boolean same(RemoteFileDesc rfd1, RemoteFileDesc rfd2) {
        return same(rfd1.getFileName(), rfd1.getSize(), rfd1.getSHA1Urn(),
                       rfd2.getFileName(), rfd2.getSize(), rfd2.getSHA1Urn());
    }
    
    /** @see similar(RemoteFileDesc, RemoteFileDesc) */
    static boolean same(String name1, long size1, URN hash1,
                        String name2, long size2, URN hash2) {
        //Either they have the same hashes...
        if (hash1!=null && hash2!=null)
            return hash1.equals(hash2);
        //..or same name and size and no conflicting hashes.
        else
            return size1==size2 && name1.equals(name2);
    }
    
    /**
     * Canonicalization is not as important on windows,
     * and is causing problems.
     * Therefore, don't do it.
     */
    private static File canonicalize(File f) throws IOException {
        f = f.getAbsoluteFile();
        if(OSUtils.isWindows())
            return f;
        else
            return f.getCanonicalFile();
    }       

    /**
     * Same as getFile(String, urn, int), except taking the values from the RFD.
     * <p>
     *    getFile(rfd) == getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
     */
    public synchronized File getFile(RemoteFileDesc rfd) throws IOException {
        return getFile(rfd.getFileName(), rfd.getSHA1Urn(), rfd.getSize());
    }

    /** 
     * Stub for calling
     *  getFile(String, URN, int, SharingSettings.INCOMPLETE_DIRECTORY.getValue());
     */
    public synchronized File getFile(String name, URN sha1, long size) throws IOException {
        return getFile(name, sha1, size, SharingSettings.INCOMPLETE_DIRECTORY.get());
    }
    
    /** 
     * Returns the fully-qualified temporary download file for the given
     * file/location pair.  If an incomplete file already exists for this
     * URN, that file is returned.  Otherwise, the location of the file is
     * determined by the "incDir" variable.   For example, getFile("test.txt", 1999)
     * may return "C:\Program Files\LimeWire\Incomplete\T-1999-Test.txt" if
     * "incDir" is "C:\Program Files\LimeWire\Incomplete".  The
     * disk is not modified, except for the file possibly being created.<p>
     *
     * This method gives duplicate files the same temporary file, which is
     * critical for resume and swarmed downloads.  That is, for all rfd_i and 
     * rfd_j
     * <pre>
     *      similar(rfd_i, rfd_j) <==> getFile(rfd_i).equals(getFile(rfd_j))<p>  
     * </pre>
     *
     * It is imperative that the files are compared as in their canonical
     * formats to preserve the integrity of the file system.  Otherwise,
     * multiple downloads could be downloading to "FILE A", and "file a",
     * although only "file a" exists on disk and is being written to by
     * both.
     *
     * @throws IOException if there was an IOError while determining the
     * file's name.
     */
    public synchronized File getFile(String name, URN sha1, long size, File incDir) throws IOException {
        boolean dirsMade = false;
        File baseFile = null;
        File canonFile = null;
        
        // make sure its created.. (the user might have deleted it)
        dirsMade = incDir.mkdirs();

        String convertedName = CommonUtils.convertFileName(name);

        try {

        if (sha1!=null) {
            File file = hashes.get(sha1);
            if (file!=null) {
                //File already allocated for hash
                return file;
            } else {
                //Allocate unique file for hash.  By "unique" we mean not in
                //the value set of HASHES.  Because we allow risky resumes,
                //there's no need to look at BLOCKS as well...
                for (int i=1 ; ; i++) {
                    file = new File(incDir, tempName(convertedName, size, i));
                    baseFile = file;
                    file = canonicalize(file);
                    canonFile = file;
                    if (! hashes.values().contains(file)) 
                        break;
                }
                //...and record the hash for later.
                hashes.put(sha1, file);
                //...and make sure the file exists on disk, so that
                //   future File.getCanonicalFile calls will match this
                //   file.  This was a problem on OSX, where
                //   File("myfile") and File("MYFILE") aren't equal,
                //   but File("myfile").getCanonicalFile() will only return
                //   a File("MYFILE") if that already existed on disk.
                //   This means that in order for the canonical-checking
                //   within this class to work, the file must exist on disk.
                FileUtils.touch(file);
                
                return file;
            }
        } else {
            //No hash.
            File f = new File(incDir, 
                        tempName(convertedName, size, 0));
            baseFile = f;
            f = canonicalize(f);
            canonFile = f;
            return f;
        }
        
        } catch(IOException ioe) {
            IOException ioe2 = new IOException(
                                    "dirsMade: " + dirsMade
                                + "\ndirExist: " + incDir.exists()
                                + "\nbaseFile: " + baseFile
                                + "\ncannFile: " + canonFile);
            ioe2.initCause(ioe);
            throw ioe2;
        }
    }
    
    /**
     * Returns the file associated with the specified URN.  If no file matches,
     * returns null.
     *
     * @return the file associated with the URN, or null if none.
     */
    public synchronized File getFileForUrn(URN urn) {
        if( urn == null )
            throw new NullPointerException("null urn");
        
        return hashes.get(urn);
    }

    /** 
     * Returns the unqualified file name for a file with the given name
     * and size, with an optional suffix to make it unique.
     */
    private static String tempName(String filename, long size, int suffix) {
        if (suffix<=1) {
            //a) No suffix
            return INCOMPLETE_PREFIX+size+"-"+filename;
        }
        int i=filename.lastIndexOf('.');
        if (i<0) {
            //b) Suffix, no extension
            return INCOMPLETE_PREFIX+size+"-"+filename+" ("+suffix+")";
        } else {
            //c) Suffix, file extension
            String noExtension=filename.substring(0,i);
            String extension=filename.substring(i); //e.g., ".txt"
            return INCOMPLETE_PREFIX+size+"-"+noExtension+" ("+suffix+")"+extension;
        }            
    }

    /** 
     * Removes the block and hash information for the given incomplete file.
     * Typically this is called after incompleteFile has been deleted.
     * @param incompleteFile a temporary file returned by getFile
     */
    public synchronized void removeEntry(File incompleteFile) {
        //Remove downloaded blocks.
        blocks.remove(incompleteFile);
        //Remove any key k from hashes for which hashes[k]=incompleteFile.
        //There should be at most one value of k.
        for (Iterator<Map.Entry<URN, File>> iter=hashes.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<URN, File> entry = iter.next();
            if (incompleteFile.equals(entry.getValue()))
                iter.remove();
        }
        
        //Remove the entry from FileManager
        library.get().remove(incompleteFile);
    }
    
    /**
     * Initializes entries with URNs, Files & Ranges.
     */
    public synchronized void initEntry(File incompleteFile, List<Range> ranges, URN sha1, boolean publish) throws InvalidDataException {
        try {
            incompleteFile = canonicalize(incompleteFile);
        } catch(IOException iox) {
            throw new InvalidDataException(iox);
        }
        
        VerifyingFile verifyingFile;
        try {
            verifyingFile = verifyingFileFactory.createVerifyingFile(getCompletedSize(incompleteFile));        
        } catch(IllegalArgumentException iae) {
            throw new InvalidDataException(iae);
        }
        if(ranges != null) {
            for(Range range : ranges) {
                verifyingFile.addInterval(range);
            }
        }
        if(ranges == null || ranges.isEmpty()) {
            try {
                verifyingFile.setScanForExistingBlocks(true, incompleteFile.length());
            } catch(IOException iox) {
                throw new InvalidDataException(iox);
            }
        }
        blocks.put(incompleteFile, verifyingFile);
        if(sha1 != null)
            hashes.put(sha1, incompleteFile);
        if(publish)
            registerIncompleteFile(incompleteFile);
        
    }

    /**
     * Associates the incompleteFile with the VerifyingFile vf.
     * Notifies FileManager about a new Incomplete File.
     */
    public synchronized void addEntry(File incompleteFile, VerifyingFile vf, boolean publish) {
        // We must canonicalize the file.
        try {
            incompleteFile = canonicalize(incompleteFile);
        } catch(IOException ignored) {}

        blocks.put(incompleteFile,vf);
        
        if (publish)
            registerIncompleteFile(incompleteFile);
    }
    
    public synchronized VerifyingFile getEntry(File incompleteFile) {
        return blocks.get(incompleteFile);
    }
    
    public synchronized long getBlockSize(File incompleteFile) {
        VerifyingFile vf = blocks.get(incompleteFile);
        if(vf==null)
            return 0;
        else
            return vf.getBlockSize();
    }
    
    /**
     * Notifies file manager about all incomplete files.
     */
    public synchronized void registerAllIncompleteFiles() {
        for(File file : blocks.keySet()) {
            if (file.exists() && !isOld(file)) 
                registerIncompleteFile(file);
        }
    }
    
    /**
     * Notifies file manager about a single incomplete file.
     */
    private synchronized void registerIncompleteFile(File incompleteFile) {
        // Only register if it has a SHA1 -- otherwise we can't share.
        Set<URN> completeHashes = getAllCompletedHashes(incompleteFile);
        if( completeHashes.size() == 0 ) return;
        
        incompleteFileCollection.get().addIncompleteFile(
            incompleteFile,
            completeHashes,
            getCompletedName(incompleteFile),
            getCompletedSize(incompleteFile),
            getEntry(incompleteFile)
        );
    }

    /**
     * Returns the name of the complete file associated with the given
     * incomplete file, i.e., what incompleteFile will be renamed to
     * when the download completes (without path information).  Slow; runs
     * in linear time with respect to the number of hashes in this.
     * @param incompleteFile a file returned by getFile
     * @return the complete file name, without path
     * @exception IllegalArgumentException incompleteFile was not the
     *  return value from getFile
     */
    public static String getCompletedName(File incompleteFile) 
            throws IllegalArgumentException {
    	
        //Given T-<size>-<name> return <name>.
        //       i      j
        //This is not as strict as it could be.  TODO: what about (x) suffix?
        String name=incompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        if (j==name.length()-1)
            throw new IllegalArgumentException("No name after last separator");
        return name.substring(j+1);
    }
    
    /**
     * Returns the size of the complete file associated with the given
     * incomplete file, i.e., the number of bytes in the file when the
     * download completes.
     * @param incompleteFile a file returned by getFile
     * @return the complete file size
     * @exception IllegalArgumentException incompleteFile was not
     *  returned by getFile 
     */
    public static long getCompletedSize(File incompleteFile) 
            throws IllegalArgumentException {
        //Given T-<size>-<name>, return <size>.
        //       i      j
        String name=incompleteFile.getName();
        int i=name.indexOf(SEPARATOR);
        if (i<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        int j=name.indexOf(SEPARATOR, i+1);
        if (j<0)
            throw new IllegalArgumentException("Missing separator: "+name);
        try {
            return Long.parseLong(name.substring(i+1, j));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format: "+name);
        }
    }

    /**
     * Returns the hash of the complete file associated with the given
     * incomplete file, i.e., the hash of incompleteFile when the 
     * download is complete.
     * @param incompleteFile a file returned by getFile
     * @return a SHA1 hash, or null if unknown
     */
    public synchronized URN getCompletedHash(File incompleteFile) {
        //Return a key k s.t., hashes.get(k)==incompleteFile...
        for(Map.Entry<URN, File> entry : hashes.entrySet()) {
            if (incompleteFile.equals(entry.getValue()) && entry.getKey().isSHA1())
                return entry.getKey();
        }
        return null; //...or null if no such k.
    }
    
    /**
     * Returns any known hashes of the complete file associated with the given
     * incomplete file, i.e., the hashes of incompleteFile when the 
     * download is complete.
     * @param incompleteFile a file returned by getFile
     * @return a set of known hashes
     */
    public synchronized Set<URN> getAllCompletedHashes(File incompleteFile) {
        Set<URN> urns = new UrnSet();
        //Return a set S s.t. for each K in S, hashes.get(k)==incpleteFile
        for(Map.Entry<URN, File> entry : hashes.entrySet()) {
            if (incompleteFile.equals(entry.getValue())) {
                urns.add(entry.getKey());
                URN ttroot = tigerTreeCache.get().getHashTreeRootForSha1(entry.getKey());
                if (ttroot != null)
                    urns.add(ttroot);
            }
        }
        return urns;
    }    

    @Override
    public synchronized String toString() {
        StringBuilder buf=new StringBuilder();
        buf.append("{");
        boolean first=true;
        for(File file : blocks.keySet()) {
            if (! first)
                buf.append(", ");

            List<Range> intervals= blocks.get(file).getVerifiedBlocksAsList();
            buf.append(file);
            buf.append(":");
            buf.append(intervals.toString());            

            first=false;
        }
        buf.append("}");
        return buf.toString();
    }

    public synchronized String dumpHashes () {
        return hashes.toString();
    }

    public Collection<File> getUnregisteredIncompleteFilesInDirectory(File value) {
        if(value == null) {
            return Collections.emptyList();
        }
        
        File[] files = value.listFiles(new FileFilter() {
            @Override
            public boolean accept(File incompleteFile) {
                if(!incompleteFile.isFile()) {
                    return false;
                }
                
                String name = incompleteFile.getName();
                
                if(isTorrentFile(incompleteFile)) {
                    Torrent torrent = torrentManager.get().getTorrent(incompleteFile); 
                    return torrent == null;
                } else {
                    if(!name.startsWith(INCOMPLETE_PREFIX)) {
                        return false;
                    }
                    
                    int i = name.indexOf(SEPARATOR);
                    if (i < 0 || i == name.length() - 1) {
                        return false;
                    }
                    int j = name.indexOf(SEPARATOR, i + 1);
                    if (j < 0 || j == name.length() - 1) {
                        return false;
                    }                
                    try {
                        Long.parseLong(name.substring(i + 1, j));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                
                synchronized(IncompleteFileManager.this) {
                    return !blocks.containsKey(FileUtils.canonicalize(incompleteFile));
                }
            }
        });
        
        if(files == null) {
            return Collections.emptyList(); 
        } else {
            return Arrays.asList(files);
        }
    }

    public static boolean isTorrentFile(File incompleteFile) {
        return "torrent".equals(FileUtils.getFileExtension(incompleteFile));
    }
}
