package com.limegroup.gnutella.dime;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Parser for creating DIMERecords from input.
 * <p>
 * See: http://www.gotdotnet.com/team/xml_wsspecs/dime/dime.htm
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 */
public class DIMEParser implements Iterator<DIMERecord> {
    
    /**
     * The input stream this parser is working off of.
     */
    private final InputStream IN;
    
    /**
     * Whether or not we've read the last record.
     */
    private boolean _lastRead = false;
    
    /**
     * Whether or not we've read the first record.
     */
    private boolean _firstRead = false;
    
    /**
     * Constructs a new DIMEParser.
     */
    public DIMEParser(InputStream in) {
        IN = in;
    }
    
    /**
     * Returns the next element.
     */
    public DIMERecord next() {
        try {
            return nextRecord();
        } catch(IOException ioe) {
            throw new NoSuchElementException(ioe.getMessage());
        }
    }
    
    /**
     * Returns the next record we can parse.
     */
    public DIMERecord nextRecord() throws IOException {
        return getNext();
    }
    
    /**
     * Return a list of all possible records we can still read from the stream.
     * <p>
     * If all records are already read, returns an empty list.
     */
    public List<DIMERecord> getRecords() throws IOException {
        if(_lastRead)
            return Collections.emptyList();
        
        List<DIMERecord> records = new LinkedList<DIMERecord>();
        while(!_lastRead)
            records.add(getNext());
        
        return records;
    }
    
    /**
     * Determines if this has more records to read.
     */
    public boolean hasNext() {
        return !_lastRead;
    }
    
    /**
     * Unsupported operation.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Reads the next record from the stream, updating the internal variables.
     * If the read record is the first and doesn't have the ME flag set,
     * throws IOException.
     * If this is called when _lastRead is already set, throws IOException.
     */
    private DIMERecord getNext() throws IOException {
        if(_lastRead)
            throw new IOException("already read last message.");
                
        DIMERecord next = DIMERecord.createFromStream(IN);
        if(next.isLastRecord())
            _lastRead = true;
            
        if(!_firstRead && !next.isFirstRecord())
            throw new IOException("middle of stream.");
        else if(_firstRead && next.isFirstRecord())
            throw new IOException("two first records.");
            
        _firstRead = true;
        
        return next;
    }
}