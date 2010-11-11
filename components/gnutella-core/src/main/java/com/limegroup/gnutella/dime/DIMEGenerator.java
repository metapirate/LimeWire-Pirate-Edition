package com.limegroup.gnutella.dime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Parser for writing DIMERecords to a stream.
 *
 * See: http://www-106.ibm.com/developerworks/library/ws-dime/
 * (or http://www.perfectxml.com/DIME.asp )
 * for information about DIME.
 * <pre>
 * To use this class, use:
 *     DIMEGenerator gen = new DIMEGenerator();
 *     gen.add(recordOne);
 *     gen.add(recordTwo);
 *     etc...
 *     gen.write(myOutputStream);
 * </pre>
 * To write the same records to another output stream, simply call
 *     gen.write(anotherOutputStream);
 * again.
 */
public class DIMEGenerator {
    
    /**
     * The list of records that will be written out.
     */
    private final List<DIMERecord> RECORDS = new LinkedList<DIMERecord>();
    
    /**
     * The amount of bytes that write(OutputStream) will write.
     */
    private int _length = 0;
    
    /**
     * Adds the given record to the internal list of records.
     */
    public void add(DIMERecord record) {
        RECORDS.add(record);
        _length += record.getRecordLength();
    }
    
    /**
     * Returns the amount of bytes that write(OutputStream) will write.
     */
    public int getLength() {
        return _length;
    }
    
    /**
     * Writes the given list of DIMERecords to a stream.
     * <p>
     * Does not do chunking.
     */
    public void write(OutputStream out) throws IOException {
        if(RECORDS.isEmpty())
            return;
        
        Iterator<DIMERecord> iter = RECORDS.iterator();
        int size = RECORDS.size();
        for(int i = 0; i < size; i++) {
            DIMERecord current = iter.next();
            if(i == 0)
                current.setFirstRecord(true);
            else
                current.setFirstRecord(false);

            if(i == size - 1)
                current.setLastRecord(true);
            else
                current.setLastRecord(false);
            current.write(out);
        }
    }

    public AsyncDimeWriter createAsyncWriter() {
        return new AsyncDimeWriter(RECORDS);
    }
    
}