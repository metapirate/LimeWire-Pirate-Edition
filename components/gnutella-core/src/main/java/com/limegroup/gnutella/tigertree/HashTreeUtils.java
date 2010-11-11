package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.SystemUtils;

import com.limegroup.gnutella.security.MerkleTree;

public class HashTreeUtils {
    
    private static final Log LOG = LogFactory.getLog(HashTreeUtils.class);

    public static final long  KB                  = 1024;
    public static final long  MB                  = 1024 * KB;
    public static final int  BLOCK_SIZE           = 1024;
    public static final byte INTERNAL_HASH_PREFIX = 0x01;

    private static final ThreadLocal<byte[]> BUFFER = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[BLOCK_SIZE * 128];
        }
    };
    
    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, containing a byte[] of the root hash.
     */
    public static List<List<byte[]>> createAllParentNodes(List<byte[]> nodes, MessageDigest messageDigest) {
        List<List<byte[]>> allNodes = new ArrayList<List<byte[]>>();
        allNodes.add(Collections.unmodifiableList(nodes));
        while (nodes.size() > 1) {
            nodes = HashTreeUtils.createParentGeneration(nodes, messageDigest);
            allNodes.add(0, nodes);
        }
        return allNodes;
    }

    /*
     * Create the parent generation of the Merkle HashTree for a given child
     * generation
     */
    public static List<byte[]> createParentGeneration(List<byte[]> nodes, MessageDigest md) {
        md.reset();
        int size = nodes.size();
        size = size % 2 == 0 ? size / 2 : (size + 1) / 2;
        List<byte[]> ret = new ArrayList<byte[]>(size);
        Iterator<byte[]> iter = nodes.iterator();
        while (iter.hasNext()) {
            byte[] left = iter.next();
            if (iter.hasNext()) {
                byte[] right = iter.next();
                md.reset();
                md.update(HashTreeUtils.INTERNAL_HASH_PREFIX);
                md.update(left, 0, left.length);
                md.update(right, 0, right.length);
                byte[] result = md.digest();
                ret.add(result);
            } else {
                ret.add(left);
            }
        }
        return ret;
    }

    /*
     * Create a generation of nodes. It is very important that nodeSize equals
     * 2^n (n>=10) or we will not get the expected generation of nodes of a
     * Merkle HashTree
     */
    public static List<byte[]> createTreeNodes(int nodeSize, long fileSize, InputStream is, MessageDigest messageDigest)
            throws IOException {
        List<byte[]> ret = new ArrayList<byte[]>((int) Math.ceil((double) fileSize / nodeSize));
        MessageDigest tt = new MerkleTree(messageDigest);
        
        byte[] block = BUFFER.get();
        long offset = 0;
        int read = 0;
        while (offset < fileSize) {
            int nodeOffset = 0;
            // reset our TigerTree instance
            tt.reset();
            // hashing nodes independently
            while (nodeOffset < nodeSize && (read = is.read(block)) != -1) {
                long time = System.currentTimeMillis();
                tt.update(block, 0, read);
                // update offsets
                nodeOffset += read;
                offset += read;
                if(SystemUtils.getIdleTime() < SharingSettings.MIN_IDLE_TIME_FOR_FULL_HASHING.getValue()
                        && SharingSettings.FRIENDLY_HASHING.getValue()) {
                    long sleep = (System.currentTimeMillis() - time) * 2;
                    if (sleep > 0)
                        try {
                            Thread.sleep(sleep);
                        } catch (InterruptedException ie) {
                            throw new IOException("interrupted during hashing operation");
                        }
                    else
                        Thread.yield();
                }
            }
            // node hashed, add the hash to our internal List.
            ret.add(tt.digest());
            
            // verify sanity of the hashing.
            if(offset == fileSize) {
                // if read isn't already -1, the next read MUST be -1.
                // it wouldn't already be -1 if the fileSize was a multiple
                // of BLOCK_SIZE * 128
                if(read != -1 && is.read() != -1) {
                    LOG.warn("More data than fileSize!");
                    throw new IOException("unknown file size.");
                }
            } else if(read == -1 && offset != fileSize) {
                if(LOG.isWarnEnabled()) {
                    LOG.warn("couldn't hash whole file. " +
                             "read: " + read + 
                           ", offset: " + offset +
                           ", fileSize: " + fileSize);
                }
                throw new IOException("couldn't hash whole file.");
            }
        }
        return ret;
    }

    /**
     * Calculates which depth we want to use for the HashTree. For small files
     * we can save a lot of memory by not creating such a large HashTree.
     * 
     * @param size
     *            the fileSize
     * @return int the ideal generation depth for the fileSize
     */    
    public static int calculateDepth(long size) {
        if (size < 256 * HashTreeUtils.KB) // 256KB chunk, 0b tree
            return 0;
        else if (size < 512 * HashTreeUtils.KB) // 256KB chunk, 24B tree
            return 1;
        else if (size < HashTreeUtils.MB)  // 256KB chunk, 72B tree
            return 2;
        else if (size < 2 * HashTreeUtils.MB) // 256KB chunk, 168B tree
            return 3;
        else if (size < 4 * HashTreeUtils.MB) // 256KB chunk, 360B tree
            return 4;
        else if (size < 8 * HashTreeUtils.MB) // 256KB chunk, 744B tree
            return 5;
        else if (size < 16 * HashTreeUtils.MB) // 256KB chunk, 1512B tree
            return 6;
        else if (size < 32 * HashTreeUtils.MB) // 256KB chunk, 3048B tree
            return 7;
        else if (size < 64 * HashTreeUtils.MB) // 256KB chunk, 6120B tree
            return 8;
        else if (size < 256 * HashTreeUtils.MB) // 512KB chunk, 12264B tree
            return 9;
        else if (size < 1024 * HashTreeUtils.MB) // 1MB chunk, 24552B tree 
            return 10;
        else if (size < 4096 * HashTreeUtils.MB) // 2MB chunks, 49128B tree 
            return 11; 
        else if (size < 64 * 1024 * HashTreeUtils.MB) 
            return 12; // 80kb tree
        else 
            return 13; // 160KB tree, 8k * 128MB chunks for 1TB file
    }

    /**
     *  Calculates a the node size based on the file size and the target depth.
     *  <p>
     *   A tree of depth n has 2^(n-1) leaf nodes, so ideally the file will be
     *   split in that many chunks.  However, since chunks have to be powers of 2,
     *   we make the size of each chunk the closest power of 2 that is bigger than
     *   the ideal size.
     *   <p>
     *   This ensures the resulting tree will have between 2^(n-2) and 2^(n-1) nodes.
     */
    public static int calculateNodeSize(long fileSize, int depth) {
        
        // don't create more than this many nodes
        long maxNodes = 1 << depth;        
        // calculate ideal node size, 
        long idealNodeSize = fileSize / maxNodes;
        // rounding up!
        if (fileSize % maxNodes != 0)
            idealNodeSize++;
        // calculate nodes size, node size must equal to 2^n, n in {10,11,...}
        int n = MerkleTree.log2Ceil(idealNodeSize);
        // 2^n
        int nodeSize = 1 << n;
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("fileSize " + fileSize);
            LOG.debug("depth " + depth);
            LOG.debug("nodeSize " + nodeSize);
        }
    
        // this is just to make sure we have the right nodeSize for our depth
        // of choice
        assert nodeSize * maxNodes >= fileSize :
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", maxNode: " + maxNodes;
        assert nodeSize * maxNodes <= fileSize * 2 :
                    "nodeSize: " + nodeSize + 
                    ", fileSize: " + fileSize + 
                    ", maxNode: " + maxNodes;
    
        return nodeSize;
    }
}
