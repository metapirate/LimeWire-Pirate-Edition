package com.limegroup.gnutella.filters;

import java.util.Arrays;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;

public class ClientGuidFilter implements SpamFilter {

    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            byte[] clientGUID = q.getClientGUID();
            if(isAllZeroesOrAllAToF(clientGUID))
                return false;
            if(Arrays.equals(clientGUID, q.getGUID()))
                return false;
        }
        return true;
    }

    private static boolean isAllZeroesOrAllAToF(byte[] guid) {
        boolean allZeroes = true, allAToF = true;
        for(byte b : guid) {
            if(b == (byte)0) {
                allAToF = false;
                if(!allZeroes)
                    return false;
            } else {
                allZeroes = false;
                if(!allAToF)
                    return false;
                int unsigned = b < 0 ? b + 255 : b;
                if((unsigned & 0x0F) < 0x0A)
                    return false;
                if((unsigned & 0xF0) < 0xA0)
                    return false;
            }
        }
        return true;
    }
}
