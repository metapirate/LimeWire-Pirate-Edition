package com.limegroup.gnutella.tigertree.dime;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.Base32;
import org.limewire.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.security.Tiger;
import com.limegroup.gnutella.tigertree.HashTreeUtils;

public class TigerDimeReadUtils {
    
    private static final Log LOG = LogFactory.getLog(TigerDimeReadUtils.class);

    /**
     * @author Gregorio Roper
     * 
     * private class holding the XML Tree description
     */
    private static class XMLTreeDescription {
        private static final int UNKNOWN = 0;
        private static final int VALID = 1;
        private static final int INVALID = 2;
        private int _parsed = UNKNOWN;
        private long _fileSize = 0;
        private int _blockSize = 0;
        private String _algorithm = null;
        private int _hashSize = 0;
        private String _serializationType = null;
//        private String _uri;
        private String data;        
    
        protected XMLTreeDescription(String xml) {
            data = xml;
        }
    
        /*
         * Accessor for the _fileSize;
         */
        long getFileSize() {
            return _fileSize;
        }
    
        /**
         * Check if the xml tree description if the tree is what we expected
         */
        boolean isValid() {
            if (_parsed == UNKNOWN) {
                _parsed = parse() ? VALID : INVALID;
            }
            
            if(_parsed == INVALID) {
                return false;
            } else if (_blockSize != HashTreeUtils.BLOCK_SIZE) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!TigerDimeUtils.DIGEST.equals(_algorithm)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unsupported digest algorithm: " + _algorithm);
                return false;
            } else if (_hashSize != TigerDimeUtils.HASH_SIZE) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected block size: " + _blockSize);
                return false;
            } else if (!TigerDimeUtils.SERIALIZED_TREE_TYPE.equals(_serializationType)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("unexpected serialization type: " + 
                              _serializationType);
                return false;
            }
            return true;
        }
    
        /*
         * A simple parsing method for reading the xml tree description.
         */
        private boolean parse() {
            // hack!
            // Shareaza sends invalid XML,
            int offset = data.indexOf("system");
            if (offset > 0 && offset < data.indexOf(TigerDimeUtils.DTD_SYSTEM_ID)) {
                data = data.substring(0, offset) + 
                       TigerDimeUtils.SYSTEM_STRING +
                       data.substring(offset + "system".length());
            }
            
            if (LOG.isDebugEnabled())
                LOG.debug("XMLTreeDescription read: " + data);
    
    
    
            Document doc = null;
            try {
                doc = XMLUtils.getDocument(data, new Resolver(), new XMLUtils.LogErrorHandler(LOG));
            } catch (IOException ioe) {
                LOG.debug(ioe);
                return false;
            }
    
            Node treeDesc = doc.getElementsByTagName("hashtree").item(0);
            if (treeDesc == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't find hashtree element: " + data);
                return false;
            }
    
            NodeList nodes = treeDesc.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;
                    if (el.getTagName().equals("file"))
                        parseFileElement(el);
                    else if (el.getTagName().equals("digest"))
                        parseDigestElement(el);
                    else if (el.getTagName().equals("serializedtree"))
                        parseSerializedtreeElement(el);
                }
            }
            return true;
        }
    
        private void parseFileElement(Element e) {
            try {
                _fileSize = Long.parseLong(e.getAttribute("size"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse file size: " + e.getNodeValue(), 
                              nfe);
            }
    
            try {
                _blockSize = Integer.parseInt(e.getAttribute("segmentsize"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse block size: " + e.getNodeValue(),
                              nfe);
            }
        }
    
        private void parseDigestElement(Element e) {
            _algorithm = e.getAttribute("algorithm");
            try {
                _hashSize = Integer.parseInt(e.getAttribute("outputsize"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse hash size: " + e.getNodeValue(),
                              nfe);
            }
        }
    
        private void parseSerializedtreeElement(Element e) {
            _serializationType = e.getAttribute("type");
//            _uri = e.getAttribute("uri");
            try {
                // value is ignored, but if it can't be parsed we should add
                // a notice to the Log
                Integer.parseInt(e.getAttribute("depth"));
            } catch (NumberFormatException nfe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("couldn't parse depth: " + e.getNodeValue(),
                              nfe);
            }
    
        }
    }

    /**
     * A custom EntityResolver so we don't hit a website for resolving.
     */
    private static final class Resolver implements EntityResolver {
        public Resolver() {}
    
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            if (systemId.equals(TigerDimeUtils.DTD_SYSTEM_ID)) {
                InputSource is = new InputSource(new StringReader(TigerDimeUtils.DTD_ENTITY));
                is.setPublicId(TigerDimeUtils.DTD_PUBLIC_ID);//optional
                is.setSystemId(TigerDimeUtils.DTD_SYSTEM_ID);//required
                return is;
            }
            //the parser will open a regular URI connection to the systemId
            //if we return null. Here we don't want this to occur...
            if (publicId == null)
                throw new SAXException("Can't resolve SYSTEM entity at '" +
                                       systemId + "'");
            else
                throw new SAXException("Can't resolve PUBLIC entity '" +
                                       publicId + "' at '" +
                                       systemId + "'");
        }
    }

    /**
     * @author Gregorio Roper
     * 
     * private class holding serialized HashTree
     */
    private static class HashTreeDescription {
        private final byte[] DATA;
        
        protected HashTreeDescription(byte[] data) {
            DATA = data;
        }
    
        /*
         * Accessor for root hash.
         */
        byte[] getRoot() throws IOException {
            if (DATA.length < TigerDimeUtils.HASH_SIZE)
                throw new IOException("invalid data");
            byte[] ret = new byte[TigerDimeUtils.HASH_SIZE];
            System.arraycopy(DATA, 0, ret, 0, TigerDimeUtils.HASH_SIZE);
            return ret;
        }
    
        /*
         * Returns a List containing a generation for nodes from the hash tree
         * 
         * @throws IOException if the hashes did not match.
         */
        List<List<byte[]>> getAllNodes(long fileSize) throws IOException {
            int depth = HashTreeUtils.calculateDepth(fileSize);
            List<byte[]> hashes = new ArrayList<byte[]>();
            byte[] data = DATA;
    
            if (data.length % TigerDimeUtils.HASH_SIZE != 0) {
                if (LOG.isDebugEnabled())
                    LOG.debug("illegal size of data field for HashTree");
                throw new IOException("corrupted hash tree detected");
            }
    
            // read the hashes from the data field
            for (int i = 0; i + TigerDimeUtils.HASH_SIZE <= data.length; i += TigerDimeUtils.HASH_SIZE) {
                byte[] hash = new byte[TigerDimeUtils.HASH_SIZE];
                System.arraycopy(data, i, hash, 0, TigerDimeUtils.HASH_SIZE);
                hashes.add(hash);
            }
    
            // iterator of all hashes we read
            Iterator<byte[]> hashIterator = hashes.iterator();
            // the current generation we are working on
            List<byte[]> generation = new ArrayList<byte[]>(1);
            // stores the last verified generation
            List<byte[]> parent = null;
            // index of the generation we are working on.
            int genIndex = 0;
            // whether or not the current row is verified.
            boolean verified = false;
            
            List<List<byte[]>> allNodes = new ArrayList<List<byte[]>>(depth+1);
            
            // Iterate through the read elements and see if they match
            // what we calculate.
            // Only calculate when we've read enough of the current
            // generation that it may be a full generation.
            // Imagine the trees:
            //           A
            //        /     \
            //       B       C
            //      / \       \
            //     D  E        C
            //    /\  /\        \
            //   F G H I         C
            //              or
            //           A
            //        /     \
            //       B       C
            //      / \     / \
            //     D  E    F   G
            //    /\  /\  /\   /\
            //   I H J K L M  N O
            //
            // In both cases, we only have read the full child gen.
            // when we've read parent.size()*2 or parent.size()*2-1
            // child nodes.
            // If it didn't match on parent.size()*2, and
            // the child has greater than that, then the tree is
            // corrupt.
            
            while (genIndex <= depth && hashIterator.hasNext()) {
                verified = false;
                byte[] hash = hashIterator.next();
                generation.add(hash);
                if (parent == null) {
                    verified = true;
                    // add generation 0 containing the root hash
                    genIndex++;
                    parent = generation;
                    allNodes.add(generation);
                    generation = new ArrayList<byte[]>(2);
                } else if (generation.size() > parent.size() * 2) {
                    // the current generation is already too big => the hash
                    // tree is corrupted, abort at once!
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("parent");
                        String str = "";
                        for(byte[] b : parent)
                            str = str + Base32.encode(b) + "; "; 
                        LOG.debug(str);
                        str = "";
                        LOG.debug("newparent");
                        List<byte[]> newparent = HashTreeUtils.createParentGeneration(generation, new Tiger());
                        for(byte[] b : newparent)
                            str = str + Base32.encode(b) + "; ";
                        LOG.debug(str);
                        str = "";
                        LOG.debug("generation");
                        for(byte[] b : generation)
                            str = str + Base32.encode(b) + "; ";
                        LOG.debug(str);
                        str = "";
    
                    }
                    throw new IOException("corrupted hash tree detected");
                } else if (generation.size() == parent.size() * 2 - 1 ||
                           generation.size() == parent.size() * 2) {
                    List<byte[]> calculatedParent =
                        HashTreeUtils.createParentGeneration(generation, new Tiger());
                    if(isMatching(parent, calculatedParent)) {
                        // the current generation is complete and verified!
                        genIndex++;
                        parent = generation;
                        allNodes.add(Collections.unmodifiableList(generation));
                        // only create room for a new generation if one exists
                        if(genIndex <= depth && hashIterator.hasNext())
                            generation = new ArrayList<byte[]>(parent.size() * 2);
                        verified = true;
                    }
                }
            } // end of while
            
            // If the current row was unable to verify, fail.
            // In mostly all cases, this will occur with the inner if
            // statement in the above loop.  However, if the last row
            // is the one that had the problem, the loop will not catch it.
            if(!verified)
                throw new IOException("corrupted hash tree detected");
    
            LOG.debug("Valid hash tree received.");
            return allNodes;
        }
        
        /**
         * Determines if two lists of byte arrays completely match.
         */
        private boolean isMatching(List<byte[]> a, List<byte[]> b) {
            if (a.size() == b.size()) {
                for (int i = 0; i < a.size(); i++) {
                    byte[] one = a.get(i);
                    byte[] two = b.get(i);
                    if(!Arrays.equals(one, two))
                        return false;
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Reads a HashTree in DIME format from an input stream.
     * Returns the list of all nodes of the tree.
     * 
     * @param is
     *            the <tt>InputStream</tt> to read from
     * @param fileSize
     *            the size of the file we expect the hash tree for
     * @param root32
     *            Base32 encoded root hash
     * @return The list of all nodes in this tree.
     * @throws IOException
     *             in case of a problem reading from the InputStream
     */
    public static List<List<byte[]>> read(InputStream is, long fileSize, String root32)
      throws IOException {
        LOG.trace("creating HashTreeHandler from network");
        DIMEParser parser = new DIMEParser(is);
        return nodesFromRecords(parser, fileSize, root32);
    }

    /**
     * Returns a list of nodes from a list of dime records.
     * 
     * @param xmlRecord
     * @param treeRecord
     * @param fileSize
     * @param root32d
     * @return
     * @throws IOException
     */
    static List<List<byte[]>> nodesFromRecords(Iterator<DIMERecord> iterator, long fileSize, String root32) throws IOException {
        if(!iterator.hasNext())
            throw new IOException("no xml record");
        DIMERecord xmlRecord = iterator.next();
        if(!iterator.hasNext())
            throw new IOException("no tree record");
        DIMERecord treeRecord = iterator.next();
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("xml id: [" + xmlRecord.getIdentifier() + "]");
            LOG.debug("xml type: [" + xmlRecord.getTypeString() + "]");
            LOG.debug("tree id: [" + treeRecord.getIdentifier() + "]");
            LOG.debug("tree type: [" + treeRecord.getTypeString() + "]");
            LOG.debug("xml type num: [" + xmlRecord.getTypeId() + "]");
            LOG.debug("tree type num: [" + treeRecord.getTypeId() + "]");
        }
        
        while(iterator.hasNext()) {
            if(LOG.isWarnEnabled())
                LOG.warn("more elements in the dime record.");
            iterator.next(); // ignore them.
        }
                
        String xml = new String(xmlRecord.getData(), "UTF-8");
        byte[] hashTree = treeRecord.getData();
        
        XMLTreeDescription xtd = new XMLTreeDescription(xml);
        if (!xtd.isValid())
            throw new IOException(
                "invalid XMLTreeDescription " + xtd.toString());
        if (xtd.getFileSize() != fileSize)
            throw new IOException(
                "file size attribute was "
                    + xtd.getFileSize()
                    + " expected "
                    + fileSize);
    
        HashTreeDescription htr = new HashTreeDescription(hashTree);
    
        if (!Base32.encode(htr.getRoot()).equals(root32))
            throw new IOException("Root hashes do not match");
    
        return htr.getAllNodes(fileSize);
    }

}
