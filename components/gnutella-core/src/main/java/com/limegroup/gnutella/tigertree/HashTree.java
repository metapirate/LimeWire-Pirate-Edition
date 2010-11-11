package com.limegroup.gnutella.tigertree;

import java.io.RandomAccessFile;
import java.util.List;

import org.limewire.collection.Range;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderValue;

/**
 * A Merkle Tree using the Tiger hash algorithm.
 */
public interface HashTree extends HTTPHeaderValue {

    /**
     * Checks whether the specific area of the file matches the hash tree. 
     */
    public boolean isCorrupt(Range in, byte[] data);

    /**
     * Checks whether the specific area of the file matches the hash tree.
     */
    public boolean isCorrupt(Range in, byte[] data, int length);

    /**
     * Checks whether the specified range in the provided file matches
     * the hash tree.
     * @param in the Range 
     * @param raf the RandomAccessFile to read from
     * @param tmp a byte [] to use as temp buffer
     * @return true if the data in the range is corrupt.
     */
    public boolean isCorrupt(Range in, RandomAccessFile raf, byte[] tmp);

    /**
     * @return Thex URI for this HashTree
     * @see com.limegroup.gnutella.http.HTTPHeaderValue#httpStringValue()
     */
    public String httpStringValue();

    /**
     * @return true if the DEPTH is ideal according to our own standards, else
     *         we know that we have to rebuild the HashTree
     */
    public boolean isGoodDepth();

    /**
     * @return true if the DEPTH is ideal enough according to our own standards
     */
    public boolean isDepthGoodEnough();

    /**
     * Determines if this tree is better than another.
     * <p>
     * A tree is considered better if the other's depth is not 'good',
     * and this depth is good, or if both are not good then the depth
     * closer to 'good' is best.
     */
    public boolean isBetterTree(HashTree other);

    /**
     * @return long Returns the FILE_SIZE.
     */
    public long getFileSize();

    /**
     * @return String Returns the Base32 encoded root hash
     */
    public String getRootHash();

    /**
     * @return Returns the root hash of the TigerTree
     */
    public byte[] getRootHashBytes();

    /**
     * @return an URN object with the root hash
     */
    public URN getTreeRootUrn();

    /**
     * @return String the THEX_URI.
     */
    public String getThexURI();

    /**
     * @return int the DEPTH
     */
    public int getDepth();

    /**
     * @return List the NODES.
     */
    public List<byte[]> getNodes();

    public int getNodeSize();

    /**
     * @return The number of nodes in the full tree.
     */
    public int getNodeCount();

}