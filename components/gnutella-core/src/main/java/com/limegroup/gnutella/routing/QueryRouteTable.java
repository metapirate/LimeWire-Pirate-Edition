package com.limegroup.gnutella.routing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.Utilities;
import com.limegroup.gnutella.xml.LimeXMLDocument;

//Please note that &#60; and &#62; are the HTML escapes for '<' and '>'.

/**
 * A list of query keywords that a connection can respond to, as well as the
 * minimum TTL for a response.  More formally, a QueryRouteTable is a (possibly
 * infinite!) list of keyword TTL pairs, [ &#60;keyword_1, ttl_1&#62;, ...,
 * &#60;keywordN, ttl_N&#62; ]  <p>
 * 
 * 10/08/2002 - A day after Susheel's birthday, he decided to change this class
 * for the heck of it.  Kidding.  Functionality has been changed so that keyword
 * depth is 'constant' - meaning that if a keyword is added, then any contains
 * query regarding that keyword will return true.  This is because this general
 * idea of QRTs is only used in a specialized way in LW - namely, UPs use it for
 * their leaves ONLY, so the depth is always 1.  If you looking for a keyword
 * and it is in the table, a leaf MAY have it, so return true.  This only
 * needed a one line change.
 * <p>
 * 12/05/2003 - Two months after Susheel's birthday, this class was changed to
 * once again accept variable infinity values.  Over time, optimizations had
 * removed the ability for a QueryRouteTable to have an infinity that wasn't
 * 7.  However, nothing outright checked that, so patch messages that were
 * based on a non-7 infinity were silently failing (always stayed empty).
 * In practice, we could probably even change the infinity to 2, and change
 * change the number of entryBits to 2, with the keywordPresent and
 * keywordAbsent values going to 1 and -1, cutting the size of our patch
 * messages further in half (a quarter of the original size).  This would
 * probably require upgrading the X-Query-Routing to another version.
 *<p>
 * <b>This class is NOT synchronized.</b>
 */
public class QueryRouteTable {
    /** 
     * The suggested default max table TTL.
     */
    public static final byte DEFAULT_INFINITY=(byte)7;
    /** What should come across the wire if a keyword status is unchanged. */
    public static final byte KEYWORD_NO_CHANGE=(byte)0;
    /** The maximum size of patch messages, in bytes. */
    public static final int MAX_PATCH_SIZE=1<<12;      //4 KB
    
    private static final AtomicInteger DEFAULT_SIZE = new AtomicInteger(-1);
    /**
     * The current infinity this table is using.  Necessary for creating
     * ResetTableMessages with the correct infinity.
     */
    private byte infinity;
    
    /**
     * What should come across the wire if a keyword is present.
     * The nature of this value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordPresent;
    
    /**
     * What should come across the wire if a keyword is absent.
     * The nature of this value is dependent on the infinity of the
     * ResetTableMessage.
     */
    private byte keywordAbsent;

    /** The *new* table implementation.  The table of keywords - each value in
     *  the BitSet is either 'true' or 'false' - 'true' signifies that a keyword
     *  match MAY be at a leaf 1 hop away, whereas 'false' signifies it isn't.
     *  QRP is really not used in full by the Gnutella Ultrapeer protocol, hence
     *  the easy optimization of only using BitSets.
     */
    
    private volatile QRTTableStorage storage;
    
    /** The 'logical' length of the BitSet.  Needed because the BitSet accessor
     *  methods don't seem to offer what is needed.
     */
    private int bitTableLength;

    /** The last message received of current sequence, or -1 if none. */
    private int sequenceNumber;
    /** The size of the current sequence, or -1 if none. */
    private int sequenceSize;

    /** The index of the next table entry to patch. */
    private int nextPatch;
    
    /** The uncompressor. This state must be maintained to implement chunked
     *  PATCH messages.  (You may need data from message N-1 to apply the patch
     *  in message N.) */
    private volatile Inflater uncompressor;
    
    /////////////////////////////// Basic Methods ///////////////////////////


    /** Creates a QueryRouteTable with default sizes. */
    public QueryRouteTable() {
        DEFAULT_SIZE.compareAndSet(-1, (int)ConnectionSettings.QRT_SIZE_IN_KIBI_ENTRIES.getValue());
        long byteCount = 1024 * DEFAULT_SIZE.get();
        if (byteCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Default QRT size cannot be expressed as an int.");
        }
        initialize((int)byteCount, DEFAULT_INFINITY);
    }

    /**
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size.  This <tt>QueryRouteTable</tt> will be completely empty with
     * no keywords -- no queries will have hits in this route table until
     * patch messages are received.
     *
     * @param size the size of the query routing table
     */
    public QueryRouteTable(int size) {
        this(size, DEFAULT_INFINITY);
    }
    
    /**
     * Creates a new <tt>QueryRouteTable</tt> instance with the specified
     * size and infinity.  This <tt>QueryRouteTable</tt> will be completely 
     * empty with no keywords -- no queries will have hits in this route 
     * table until patch messages are received.
     *
     * @param size the size of the query routing table
     * @param infinity the infinity to use
     */
    public QueryRouteTable(int size, byte infinity) {
        initialize(size, infinity);
    }    

    /**
     * Initializes this <tt>QueryRouteTable</tt> to the specified size.
     * This table will be empty until patch messages are received.
     *
     * @param size the size of the query route table
     */
    private void initialize(int size, byte infinity) {
        this.bitTableLength = size;
        this.storage = new DynamicQRTStorage(bitTableLength);
        this.sequenceNumber = -1;
        this.sequenceSize = -1;
        this.nextPatch = 0;
        this.keywordPresent = (byte)(1 - infinity);
        this.keywordAbsent = (byte)(infinity - 1);
        this.infinity = infinity;
    }
    
    /**
     * Returns the size of this QueryRouteTable.
     */
    public int getSize() {
        return bitTableLength;
    }    
    
    /**
     * Returns the percentage of slots used in this QueryRouteTable's BitTable.
     * The return value is from 0 to 100.
     */
    public double getPercentFull() {
        return storage.getPercentFull();
	}
	
	/**
	 * Returns the number of empty elements in the table.
	 */
	public int getEmptyUnits() {
	    return storage.getUnusedUnits();
	}
	
	/**
	 * Returns the total number of units allocated for storage.
	 */
	public int getUnitsInUse() {
	    return storage.getUnitsInUse();
	}

	/**
     * @return the number of units with specified load.
     */
    public int getUnitsWithLoad(int load) {
        return storage.numUnitsWithLoad(load);
    }
    
    /**
     * Returns true if a response could be generated for qr.  Note that a return
     * value of true does not necessarily mean that a response will be
     * generated--just that it could.  It is assumed that qr's TTL has already
     * been decremented, i.e., is the outbound not inbound TTL.  
     */
    public boolean contains(QueryRequest qr) {
        byte bits=Utilities.log2(bitTableLength);

        //1. First we check that all the normal keywords of qr are in the route
        //   table.  Note that this is done with zero allocations!  Also note
        //   that HashFunction.hash() takes cares of the capitalization.
        String query = qr.getQuery();
        LimeXMLDocument richQuery = qr.getRichQuery();
        if (query.length() == 0 && richQuery == null && !qr.hasQueryUrns()) {
            return false;
        }
        if (qr.hasQueryUrns()) {
            Set<URN> urns = qr.getQueryUrns();
            for (URN qurn : urns) {
                int hash = HashFunction.hash(qurn.toString(), bits);
                if (contains(hash)) {
                    // we note a match if any one of the hashes matches
                    return true;
                }
            }
            return false;
        }
        for (int i=0 ; ; ) {
            //Find next keyword...
            //    _ _ W O R D _ _ _ A B
            //    i   j       k
            int j=HashFunction.keywordStart(query, i);     
            if (j<0)
                break;
            int k=HashFunction.keywordEnd(query, j);

            //...and look up its hash.
            int hash=HashFunction.hash(query, j, k, bits);
            if (!contains(hash))
                return false;
            i=k+1;
        }        
        
        //2. Now we extract meta information in the query.  If there isn't any,
        //   declare success now.  Otherwise ensure that the URI is in the 
        //   table.
        if (richQuery == null) //Normal case for matching query with no metadata.
            return true;
        String docSchemaURI = richQuery.getSchemaURI();
        int hash = HashFunction.hash(docSchemaURI, bits);
        if (!contains(hash))//don't know the URI? can't answer query
            return false;
            
        //3. Finally check that "enough" of the meta information keywords are in
        //   the table: 2/3 or 3, whichever is more.
        int wordCount=0;
        int matchCount=0;
        for(String words : richQuery.getKeyWords()) {
            //getKeyWords only returns all the fields, so we still need to split
            //the words.  The code is copied from part (1) above.  It could be
            //factored, but that's slightly tricky; the above code terminates if
            //a match fails--a nice optimization--while this code simply counts
            //the number of words and matches.
            for (int i=0 ; ; ) {
                //Find next keyword...
                //    _ _ W O R D _ _ _ A B
                //    i   j       k
                int j=HashFunction.keywordStart(words, i);     
                if (j<0)
                    break;
                int k=HashFunction.keywordEnd(words, j);
                
                //...and look up its hash.
                int wordHash = HashFunction.hash(words, j, k, bits);
                if (contains(wordHash))
                    matchCount++;
                wordCount++;
                i=k+1;
            }
        }

        // some parts of the query are indivisible, so do some nonstandard
        // matching
        for(String str : richQuery.getKeyWordsIndivisible()) {
            hash = HashFunction.hash(str, bits);
            if (contains(hash))
                matchCount++;
            wordCount++;
        }

        if (wordCount<3)
            //less than three word? 100% match required
            return wordCount==matchCount;
        else 
            //a 67% match will do...
            return ((float)matchCount/(float)wordCount) > 0.67;
    }
    
    // In the new version, we will not accept TTLs for methods.  Tables are only
    // 1 hop deep....
    private final boolean contains(int hash) {
        return storage.get(hash);
    }

    /**
     * <xmp>
     * For all keywords k in filename, adds <k> to this.
     * </xmp>
     */
    public void add(String filePath) {
        addBTInternal(filePath);
    }


    private void addBTInternal(String filePath) {
        String[] words = HashFunction.keywords(filePath);
        String[] keywords=HashFunction.getPrefixes(words);
		byte log2 = Utilities.log2(bitTableLength);
        for (int i=0; i<keywords.length; i++) {
            int hash=HashFunction.hash(keywords[i], log2);
            if (!storage.get(hash)) {
                storage.set(hash);
            }
        }
    }


    public void addIndivisible(String iString) {
        final int hash = HashFunction.hash(iString, 
                                           Utilities.log2(bitTableLength));
        if (!storage.get(hash)) {
            storage.set(hash);
        }
    }


    /**
     * <xmp>
     * For all <keyword_i> in qrt, adds <keyword_i> to this.
     * (This is useful for unioning lots of route tables for propagation.)
     *</xmp>
     *    @modifies this
     */
    public void addAll(QueryRouteTable qrt) {
        this.storage.or( qrt.storage.resize(this.bitTableLength) );
        this.storage.compact();
    }
    


    /** True if o is a QueryRouteTable with the same entries of this. */
    @Override
    public boolean equals(Object o) {
        if ( this == o )
            return true;
            
        if (! (o instanceof QueryRouteTable))
            return false;

        //TODO: two qrt's can be equal even if they have different TTL ranges.
        QueryRouteTable other=(QueryRouteTable)o;
        if (this.bitTableLength!=other.bitTableLength)
            return false;
        
        if (!this.storage.equals(other.storage))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return storage.hashCode() * 17;
    }


    @Override
    public String toString() {
        return "QueryRouteTable: " + storage.toString();
    }


    ////////////////////// Core Encoding and Decoding //////////////////////


    /**
     * Resets this <tt>QueryRouteTable</tt> to the specified size with
     * no data.  This is done when a RESET message is received.
     *
     * @param rtm the <tt>ResetTableMessage</tt> containing the size
     *  to reset the table to
     */
    public void reset(ResetTableMessage rtm) {
        initialize(rtm.getTableSize(), rtm.getInfinity());
    }

    /**
     * Adds the specified patch message to this query routing table.
     *
     * @param patch the <tt>PatchTableMessage</tt> containing the new
     *  data to add
     * @throws <tt>BadPacketException</tt> if the sequence number or size
     *  is incorrect
     */
    public void patch(PatchTableMessage patch) throws BadPacketException {
        handlePatch(patch);        
    }


    //All encoding/decoding works in a pipelined manner, by continually
    //modifying a byte array called 'data'.  TODO2: we could avoid a lot of
    //allocations here if memory is at a premium.

    private void handlePatch(PatchTableMessage m) throws BadPacketException {
        //0. Verify that m belongs in this sequence.  If we haven't just been
        //RESET, ensure that m's sequence size matches last message
        if (sequenceSize!=-1 && sequenceSize!=m.getSequenceSize())
            throw new BadPacketException("Inconsistent seq size: "
                                         +m.getSequenceSize()
                                         +" vs. "+sequenceSize);
        //If we were just reset, ensure that m's sequence number is one.
        //Otherwise it should be one greater than the last message received.
        if (sequenceNumber==-1 ? m.getSequenceNumber()!=1 //reset
                               : sequenceNumber+1!=m.getSequenceNumber())
            throw new BadPacketException("Inconsistent seq number: "
                                         +m.getSequenceNumber()
                                         +" vs. "+sequenceNumber);

        byte[] data=m.getData();

        //1. Start pipelined uncompression.
        //TODO: check that compression is same as last message.
        if (m.getCompressor()==PatchTableMessage.COMPRESSOR_DEFLATE) {
            try {
                //a) If first message, create uncompressor (if needed).
                if (m.getSequenceNumber()==1) {
                    uncompressor = new Inflater();
                }       
                assert uncompressor!=null : 
                    "Null uncompressor.  Sequence: "+m.getSequenceNumber();
                data=uncompress(data);            
            } catch (IOException e) {
                throw new BadPacketException("Couldn't uncompress data: "+e);
            }
        } else if (m.getCompressor()!=PatchTableMessage.COMPRESSOR_NONE) {
            throw new BadPacketException("Unknown compressor");
        }
        
        //2. Expand nibbles if necessary.
        if (m.getEntryBits()==4) 
            data=unhalve(data);
        else if (m.getEntryBits()!=8)
            throw new BadPacketException("Unknown value for entry bits");

        //3. Add data[0...] to table[nextPatch...]            
        for (int i=0; i<data.length; i++) {
            if(nextPatch >= bitTableLength)
                throw new BadPacketException("Tried to patch "+nextPatch
                                             +" on a bitTable of size "
                                             + bitTableLength);
            // All negative values indicate presence
            if (data[i] < 0) {
                storage.set(nextPatch);
            }
            // All positive values indicate absence
            else if (data[i] > 0) {
                storage.clear(nextPatch);
            }
            nextPatch++;
        }
        storage.compact();

        //4. Update sequence numbers.
        this.sequenceSize=m.getSequenceSize();
        if (m.getSequenceNumber()!=m.getSequenceSize()) {            
            this.sequenceNumber=m.getSequenceNumber();
        } else {
            //Sequence complete.
            this.sequenceNumber=-1;
            this.sequenceSize=-1;
            this.nextPatch=0; //TODO: is this right?
            // if this last message was compressed, release the uncompressor.
            if( this.uncompressor != null ) {
                IOUtils.close(uncompressor);
                this.uncompressor = null;
            }
        }   
    }
    
    /**
     * Stub for calling encode(QueryRouteTable, true).
     */
    public List<RouteTableMessage> encode(QueryRouteTable prev) {
        return encode(prev, true);
    }

    /**
     * Returns an List of RouteTableMessage that will convey the state of
     * this.  If that is null, this will include a reset.  Otherwise it will
     * include only those messages needed to to convert that to this.  More
     * formally, for any non-null QueryRouteTable's m and that, the following 
     * holds:
     *
     * <pre>
     * for (Iterator iter=m.encode(); iter.hasNext(); ) 
     *    prev.update((RouteTableUpdate)iter.next());
     * Assert.that(prev.equals(m)); 
     * </pre> 
     */
    public List<RouteTableMessage> encode(
      QueryRouteTable prev, boolean allowCompression) {
        List<RouteTableMessage> buf=new LinkedList<RouteTableMessage>();
        if (prev==null)
            buf.add(new ResetTableMessage(bitTableLength, infinity));
        else
            assert prev.bitTableLength==this.bitTableLength :
                        "TODO: can't deal with tables of different lengths";

        //1. Calculate patch array
        byte[] data=new byte[bitTableLength];
        // Fill up data with KEYWORD_NO_CHANGE, since the majority
        // of elements will be that.
        // Because it is already filled, we do not need to iterate and
        // set it anywhere.
        Utilities.fill(data, 0, bitTableLength, KEYWORD_NO_CHANGE);
        boolean needsPatch=false;
        
        //1a. If there was a previous table, determine if it was the same one.
        //    If so, we can prevent BitTableLength calls to BitSet.get(int).
        if( prev != null ) {
            //1a-I. If they are not equal, xOr the tables and loop
            //      through the different bits.  This avoids
            //      bitTableLength*2 calls to BitSet.get
            //      at the cost of the xOr'd table's cardinality
            //      calls to both BitSet.nextSetBit and BitSet.get.
            //      Generally it is worth it, as our BitTables don't
            //      change very rapidly.
            //      With the xOr'd table, we know that all 'clear'
            //      values have not changed.  Thus, we can use
            //      nextSetBit on the xOr'd table & this.bitTable.get
            //      to determine whether or not we should set
            //      data[x] to keywordPresent or keywordAbsent.
            //      Because this is an xOr, we know that if 
            //      this.bitTable.get is true, prev.bitTable.get
            //      is false, and vice versa.            
            if(!this.storage.equals(prev.storage) ) {
                QRTTableStorage xOr = null;
                try {
                    xOr = this.storage.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
                xOr.xor(prev.storage);
                for (int i : xOr) {
                    data[i] = this.storage.get(i) ?
                            keywordPresent : keywordAbsent;
                    needsPatch = true;
                }
            }
            // Else the two tables are equal, and we don't need to do anything
            // because all elements already contain KEYWORD_NO_CHANGE.
        }
        //1b. If there was no previous table, scan through the table using
        //    nextSetBit, avoiding bitTableLength calls to BitSet.get(int).
        else {
            for (int i : storage) {
                data[i] = keywordPresent;
                needsPatch = true;
            }
        }
        //Optimization: there's nothing to report.  If prev=null, send a single
        //RESET.  Otherwise send nothing.
        if (!needsPatch) {
            return buf;
        }


        //2. Try compression.
        //TODO: Should this not be done if compression isn't allowed?
        byte bits=8;
        // Only halve if our values require 4 signed bits at most.
        // keywordPresent will always be negative and
        // keywordAbsent will always be positive.
        if( keywordPresent >= -8 && keywordAbsent <= 7 ) {
            bits = 4;
            data = halve(data);
        }

        byte compression=PatchTableMessage.COMPRESSOR_NONE;
        //Optimization: If we are told it is safe to compress the message,
        //then attempt to compress it.  Reasons it is not safe include
        //the outgoing stream already being compressed.
        if( allowCompression ) {
            byte[] patchCompressed = IOUtils.deflate(data);
            if (patchCompressed.length<data.length) {
                //...Hooray!  Compression was efficient.
                data=patchCompressed;
                compression=PatchTableMessage.COMPRESSOR_DEFLATE;
            }
        }
                   

        //3. Break into 1KB chunks and send.  TODO: break size limits if needed.
        final int chunks=(int)Math.ceil((float)data.length/(float)MAX_PATCH_SIZE);
        int chunk=1;
        for (int i=0; i<data.length; i+=MAX_PATCH_SIZE) {
            //Just past the last position of data to copy.
            //Note special case for last chunk.  
            int stop=Math.min(i+MAX_PATCH_SIZE, data.length);
            buf.add(new PatchTableMessage((short)chunk, (short)chunks,
                                          compression, bits,
                                          data, i, stop));
            chunk++;
        }        
        return buf;        
    }


    ///////////////// Helper Functions for Codec ////////////////////////

    /** Returns the uncompressed version of the given defaulted bytes, using
     *  any dictionaries in uncompressor.  Throws IOException if the data is
     *  corrupt.
     *      @requires inflater initialized 
     *      @modifies inflater */
    private byte[] uncompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        uncompressor.setInput(data);
        
        try {
            byte[] buf=new byte[1024];
            while (true) {
                int read=uncompressor.inflate(buf);
                //Needs input?
                if (read==0)
                    break;
                baos.write(buf, 0, read);                
            }
            baos.flush();
            return baos.toByteArray();
        } catch (DataFormatException e) {
            throw new IOException("Bad deflate format");
        }
    }

    /**
     * @return a byte[] copy of this routing table.  
     */
    public byte [] getRawDump() {
        byte [] ret = new byte[bitTableLength / 8];
        for(int i : storage) 
            ret[i / 8] = (byte) (ret[i / 8] | (1 << (7 - i % 8)));
        return ret;
    }
    
    /** Returns an array R of length array.length/2, where R[i] consists of the
     *  low nibble of array[2i] concatenated with the low nibble of array[2i+1].
     *  Note that unhalve(halve(array))=array if all elements of array fit can 
     *  fit in four signed bits.
     *      @requires array.length is a multiple of two */
    static byte[] halve(byte[] array) {
        byte[] ret=new byte[array.length/2];
        for (int i=0; i<ret.length; i++)
            ret[i]=(byte)((array[2*i]<<4) | (array[2*i+1]&0xF));
        return ret;
    }

    /** Returns an array of R of length array.length*2, where R[i] is the the
     *  sign-extended high nibble of floor(i/2) if i even, or the sign-extended
     *  low nibble of floor(i/2) if i odd. */        
    static byte[] unhalve(byte[] array) {
        byte[] ret=new byte[array.length*2];
        for (int i=0; i<array.length; i++) {
            ret[2*i]=(byte)(array[i]>>4);     //sign extension
            ret[2*i+1]=extendNibble((byte)(array[i]&0xF));
        }
        return ret;
    }    
    
    /** Sign-extends the low nibble of b, i.e., 
     *  returns (from MSB to LSB) b[3]b[3]b[3]b[3]b[3]b[2]b[1]b[0]. */
    static byte extendNibble(byte b) {
        if ((b&0x8)!=0)   //negative nibble; sign-extend.
            return (byte)(0xF0 | b);
        else
            return b;        
    }
}
