package com.limegroup.gnutella.tigertree.dime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dime.DIMEGenerator;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.tigertree.ThexWriter;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeNodeManager;
import com.limegroup.gnutella.tigertree.HashTreeUtils;
import com.limegroup.gnutella.tigertree.HashTreeWriteHandler;
import com.limegroup.gnutella.util.UUID;

/**
 * @author Gregorio Roper
 * 
 * Class handling all the reading and writing of HashTrees to the network
 */
class TigerDimeWriteHandler implements HashTreeWriteHandler {
    private static final Log LOG = LogFactory.getLog(TigerDimeWriteHandler.class);
    
    private static final String OUTPUT_TYPE = "application/dime";

    private static final String XML_TYPE = "text/xml";

    private static final byte[] TREE_TYPE_BYTES =
        getBytes(TigerDimeUtils.SERIALIZED_TREE_TYPE);
    private static final byte[] XML_TYPE_BYTES =
        getBytes(XML_TYPE);

    private static final String XML_TREE_DESC_START =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<!DOCTYPE hashtree " + TigerDimeUtils.SYSTEM_STRING + " \"" + TigerDimeUtils.DTD_SYSTEM_ID + "\">"
        + "<hashtree>";
    private static final String XML_TREE_DESC_END = "</hashtree>";

    /**
     * Returns the bytes of a string in UTF-8 format, or in the default
     * format if UTF-8 failed for whatever reason.
     */
    private static byte[] getBytes(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch(UnsupportedEncodingException uee) {
            LOG.debug(string, uee);
            return string.getBytes();
        }
    }
        
    
    
    /////////////////////////       WRITING        ///////////////////////

    /** 
     * The generator containing the DIME message to send.
     */
    private final DIMEGenerator GENERATOR;

    /**
     * Constructs a new handler for sending
     * @param tree
     *            the <tt>HashTree</tt> to construct this message from
     */
    public TigerDimeWriteHandler(HashTree tree, HashTreeNodeManager hashTreeNodeManager) {
        LOG.trace("creating HashTreeHandler for sending");
        UUID uri = UUID.nextUUID();
        GENERATOR = new DIMEGenerator();
        GENERATOR.add(new XMLRecord(tree, uri));
        GENERATOR.add(new TreeRecord(tree, uri, hashTreeNodeManager));
    }

    /**
     * method for writing a HashTree to an OutputStream
     * 
     * @param os
     *            the <tt>OutputStream</tt> to write to.
     * @throws IOException
     *             if there was a problem writing to os.
     */
    public void write(OutputStream os) throws IOException {
        GENERATOR.write(os);
    }
    
    /**
     * Determines the length of the written data.
     */
    public int getOutputLength() {
        return GENERATOR.getLength();
    }

    /**
     * Determines the mime type of the output.
     */
    public String getOutputType() {
        return OUTPUT_TYPE;
    }

    public ThexWriter createAsyncWriter() {
        return new AsyncTigerTreeWriter(GENERATOR.createAsyncWriter());
    }    
    
    /**
     * A simple XML DIMERecord.
     */
    private static class XMLRecord extends DIMERecord {
        XMLRecord(HashTree tree, UUID uri) {
            super(DIMERecord.TYPE_MEDIA_TYPE, null, null,
                  XML_TYPE_BYTES, getXML(tree, uri));
        }
        
        /**
         * Constructs the XML bytes.
         */
        private static byte[] getXML(HashTree tree, UUID uri) {
            String xml =
                XML_TREE_DESC_START
                + "<file size='"
                + tree.getFileSize()
                + "' segmentsize='"
                + HashTreeUtils.BLOCK_SIZE
                + "'/>"
                + "<digest algorithm='"
                + TigerDimeUtils.DIGEST
                + "' outputsize='"
                + TigerDimeUtils.HASH_SIZE
                + "'/>"
                + "<serializedtree depth='"
                + tree.getDepth()
                + "' type='"
                + TigerDimeUtils.SERIALIZED_TREE_TYPE
                + "' uri='uuid:"
                + uri
                + "'/>"
                + XML_TREE_DESC_END;
            return getBytes(xml);
        }
    }
    
    /**
     * Private DIMERecord for a Tree.
     */
    private static class TreeRecord extends DIMERecord {
        /**
         * The tree of this record.
         */
        private final HashTree tigerTree;
        
        /**
         * The length of the tree.
         */
        private final int length;
        
        /** The manager to retrieve nodes from. */
        private final HashTreeNodeManager hashTreeNodeManager;
        
        TreeRecord(HashTree tree, UUID uri, HashTreeNodeManager hashTreeNodeManager) {
            super(DIMERecord.TYPE_ABSOLUTE_URI, null,
                  getBytes("uuid:" + uri),
                  TREE_TYPE_BYTES, null);
            tigerTree = tree;
            length = tigerTree.getNodeCount() * TigerDimeUtils.HASH_SIZE;
            this.hashTreeNodeManager = hashTreeNodeManager;
        }

        /**
         * Writes the tree's data to the specified output stream.
         */
        @Override
        public void writeData(OutputStream out) throws IOException {
            for(List<byte[]> list : hashTreeNodeManager.getAllNodes(tigerTree)) {
                for(byte[] b : list)
                    out.write(b);
            }
            writePadding(getDataLength(), out);
        }
    
        /**
         * Determines the length of the data.
         */
        @Override
        public int getDataLength() {
            return length;
        }
    }

}
