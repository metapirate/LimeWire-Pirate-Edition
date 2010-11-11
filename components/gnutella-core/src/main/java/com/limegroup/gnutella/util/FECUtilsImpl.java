package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

/**
 * Encapsulates lots of the FEC-related ugliness.
 */
@Singleton
public class FECUtilsImpl implements FECUtils {
    
    /** The factory that creates the FEC code.  Inited lazily */
    private FECCodeFactory fecFactory;

    @Inject
    public FECUtilsImpl() {
        System.setProperty("com.onionnetworks.fec.keys", "pure8,pure16"); // disable native libs
    }
    
    public List<byte[]> encode(byte[] data, int packetSize, float redundancy) {
        if (fecFactory == null)
            fecFactory = FECCodeFactory.getDefault();
        
        int numPackets = data.length / packetSize + (data.length % packetSize == 0 ? 0 : 1);
        int numChecksums = (int)Math.ceil(numPackets * redundancy);
        
        FECCode code = fecFactory.createFECCode(numPackets, numChecksums);
        Buffer [] chunks = new Buffer[numPackets];
        Buffer [] out = new Buffer[numChecksums];
        int [] indices = new int[numChecksums];
        for (int i = 0; i < numPackets; i++) {
            byte [] b = new byte[packetSize];
            System.arraycopy(data, i * packetSize, b, 0, Math.min(packetSize, data.length - i * packetSize));
            chunks[i] = new Buffer(b);
            out[i] = new Buffer(new byte[packetSize]);
            indices[i] = i;
        }
        for (int i = numPackets; i < numChecksums; i++) {
            out[i] = new Buffer(new byte[packetSize]);
            indices[i] = i;
        }
        
        code.encode(chunks, out, indices);
        
        List<byte []> ret = new ArrayList<byte[]>(numChecksums);
        for (Buffer buf : out)
            ret.add(buf.b);
        
        return ret;
    }
    
    public byte[] decode(List<byte []> packets, int size) {
        if (fecFactory == null)
            fecFactory = FECCodeFactory.getDefault();

        int packetSize = -1;
        List<Integer> indices = new ArrayList<Integer>(packets.size());
        List<Buffer> buffers = new ArrayList<Buffer>(packets.size());
        for (int i = 0; i < packets.size(); i++) {
            if (packets.get(i) == null)
                continue;
            buffers.add(new Buffer(packets.get(i)));
            indices.add(i);
            if (packetSize == -1)
                packetSize = packets.get(i).length;
            else if (packets.get(i).length != packetSize)
                return null; // not gonna work.
                
        }
        
        if (packetSize == -1)
            return null; // no packets at all?
        
        int [] indicesArray = new int[indices.size()];
        for (int i = 0; i < indicesArray.length; i++)
            indicesArray[i] = indices.get(i);
        Buffer [] bufferArray = new Buffer[buffers.size()];
        buffers.toArray(bufferArray);
        
        int numPackets = size / packetSize + (size % packetSize == 0 ? 0 : 1);
        
        FECCode code = fecFactory.createFECCode(numPackets, packets.size());
        try {
            code.decode(bufferArray, indicesArray);
        } catch (Throwable decodeFailed) { // bad API
            return null; 
        }
        
        byte [] ret = new byte[size];
        for (int i = 0; i < numPackets; i++) {
            byte [] b = bufferArray[i].b;
            System.arraycopy(b,0,ret, i * packetSize, Math.min(packetSize, size - i * packetSize));
        }
        return ret;
    }
}
