package com.limegroup.gnutella.tigertree;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.security.Tiger;
import com.limegroup.gnutella.tigertree.dime.TigerDimeReadUtils;

/**
 * Default implementation of {@link HashTreeFactory}.
 */
/* This is public for tests, but should always be referenced by its interface in real code. */
@Singleton
public class HashTreeFactoryImpl implements HashTreeFactory {

    private static final Log LOG = LogFactory.getLog(HashTreeFactoryImpl.class);
    
    private final HashTreeNodeManager hashTreeNodeManager;
    
    @Inject
    public HashTreeFactoryImpl(HashTreeNodeManager hashTreeNodeManager) {
        this.hashTreeNodeManager = hashTreeNodeManager;
    }

    public HashTree createHashTree(FileDesc fd) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("creating hashtree for file " + fd);
        InputStream in = null;
        try {
            // not buffered purposely, because the stream is already
            // read in blocks inside of createHashTree.
            in = new FileInputStream(fd.getFile());
            return createHashTree(fd.getFileSize(), in, fd.getSHA1Urn());
        } finally {
            IOUtils.close(in);
        }
    }

    /**
     * Reads a new HashTree from the network. It is expected that the data is in
     * DIME format, the first record being an XML description of the tree's
     * structure, and the second record being the breadth-first tree.
     * 
     * @param is the <tt>InputStream</tt> to read from
     * @param sha1 a <tt>String</tt> containing the sha1 URN for the same file
     * @param root32 a <tt>String</tt> containing the Base32 encoded expected
     *        root hash
     * @param fileSize the long specifying the size of the File
     * @return HashTree if we successfully read from the network
     * @throws IOException if there was an error reading from the network or if
     *         the data was corrupted or invalid in any way.
     */
    HashTree createHashTree(InputStream is, String sha1, String root32, long fileSize)
            throws IOException {
        if (LOG.isTraceEnabled())
            LOG.trace("reading " + sha1 + "." + root32 + " dime data.");
        return createHashTree(TigerDimeReadUtils.read(is, fileSize, root32), sha1, fileSize);
    }

    public HashTree createHashTree(List<List<byte[]>> allNodes, String sha1, long fileSize) {
        return createHashTree(allNodes, sha1, fileSize, HashTreeUtils.calculateNodeSize(fileSize,
                allNodes.size() - 1));
    }

    /**
     * Creates a new HashTree for the given file size, input stream and SHA1.
     * <p>
     * Exists as a hook for tests, to create a HashTree from a File when no
     * FileDesc exists.
     */
    public HashTree createHashTree(long fileSize, InputStream is, URN sha1) throws IOException {
        // do the actual hashing
        int nodeSize = HashTreeUtils.calculateNodeSize(fileSize, HashTreeUtils.calculateDepth(fileSize));
        List<byte[]> nodes = HashTreeUtils.createTreeNodes(nodeSize, fileSize, is, new Tiger());

        // calculate the intermediary nodes to get the root hash & others.
        List<List<byte[]>> allNodes = HashTreeUtils.createAllParentNodes(nodes, new Tiger());
        return createHashTree(allNodes, sha1.toString(), fileSize, nodeSize);
    }

    private HashTree createHashTree(List<List<byte[]>> allNodes, String sha1, long fileSize, int nodeSize) {
        HashTree tree = new HashTreeImpl(allNodes, sha1.toString(), fileSize, nodeSize);
        hashTreeNodeManager.register(tree, allNodes);
        return tree;
    }

}
