package com.limegroup.gnutella.tigertree;

import java.util.List;

public interface HashTreeNodeManager {

    /**
     * Returns all intermediary nodes for the tree.
     */
    public List<List<byte[]>> getAllNodes(HashTree tree);

    /**
     * Registers the given list of nodes for the tree.
     */
    public void register(HashTree tree, List<List<byte[]>> nodes);

}