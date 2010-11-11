package com.limegroup.gnutella.messages;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.util.ByteUtils;



/**
 * Contains the logic for writing and reading IntervalSets
 * to and from a GGEP field.
 */
public class IntervalEncoder {
    
    public static void encode(long size, GGEP g, IntervalSet s) {
        List<Integer> bytes = new ArrayList<Integer>();
        List<Integer> shorts = new ArrayList<Integer>();
        List<Integer> b24 = new ArrayList<Integer>();
        List<Integer> ints = new ArrayList<Integer>();
        
        for (int i : s.encode(size)) {
            long l = i & 0xFFFFFFFF;
            if (l > 0xFFFFFFL)
                ints.add(i);
            else if (l > 0xFFFF)
                b24.add(i);
            else if (l > 0xFF)
                shorts.add(i);
            else
                bytes.add(i);
        }
        
        byte [] bytesB = new byte[bytes.size()];
        for (int i = 0; i < bytesB.length; i++)
            bytesB[i] = (byte)bytes.get(i).intValue();
        
        byte [] shortsB = new byte[shorts.size() * 2];
        for (int i = 0; i < shorts.size(); i++)
            ByteUtils.short2beb(shorts.get(i).shortValue(), shortsB, i * 2);
        
        byte [] b24B = new byte[b24.size() * 3];
        for (int i = 0; i < b24.size(); i++) {
            int value = b24.get(i);
            b24B[i*3] = (byte)((value & 0xFF0000) >> 16);
            b24B[i*3 + 1] = (byte)((value & 0xFF00) >> 8);
            b24B[i*3 + 2] = (byte)(value & 0xFF);
        }
        
        byte [] intsB = new byte[ints.size() * 4];
        for (int i = 0; i < ints.size(); i++) 
            ByteUtils.int2beb(ints.get(i).intValue(), intsB, i * 4);
        
        int availableSpace = SharingSettings.MAX_PARTIAL_ENCODING_SIZE.getValue();
        availableSpace = addIfSpace(bytesB,g,1,availableSpace);
        availableSpace = addIfSpace(shortsB,g,2,availableSpace);
        availableSpace = addIfSpace(b24B,g,3,availableSpace);
        addIfSpace(intsB,g,4,availableSpace);
        
        // special case - for an empty interval set we add an extention
        if (bytes.size() + shorts.size() + b24.size() + ints.size() == 0)
            g.put(GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX+"0");
    }
    
    /**
     * adds the byte array to the appropriate ggep value if there is enough space 
     * @param dataSize the size of each entry
     * @param available how much space we have available
     * @return how much space is left after adding
     */
    private static int addIfSpace(byte [] toAdd, GGEP ggep, int dataSize, int available) {
        if (toAdd.length == 0 || available <= 0)
            return available;
        assert toAdd.length % dataSize == 0;
        if (toAdd.length > available) {
            byte [] tmp = new byte[available - (available % dataSize)];
            if (tmp.length == 0)
                return available;
            System.arraycopy(toAdd,0,tmp,0,tmp.length);
            toAdd = tmp;
        }
        ggep.put(GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX+dataSize,toAdd);
        return available - toAdd.length;
    }
    
    /**
     * @return an IntervalSet contained in this GGEP.  Null if none.
     */
    public static IntervalSet decode(long size, GGEP ggep) throws BadGGEPPropertyException{
        if (ggep.hasKey(GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX+"0"))
            return new IntervalSet();
        IntervalSet ret = null;
        for (int i = 1; i <= 4; i++ ) {
            String key = GGEPKeys.GGEP_HEADER_PARTIAL_RESULT_PREFIX+i;
            if (ggep.hasValueFor(key)) {
                byte [] b = ggep.get(key);                
                if (ret == null)
                    ret = new IntervalSet();

                // is data valid?
                if (b.length % i != 0)
                    return null;

                for (int j = 0; j < b.length; j+=i) {
                    int nodeId = 0;
                    for (int k = 0; k < i; k++) {
                        nodeId <<= 8;
                        nodeId |= (b[j + k] & 0xFF);
                    }
                    
                    ret.decode(size, nodeId);
                }
            }
        }
        return ret;
    }
}
