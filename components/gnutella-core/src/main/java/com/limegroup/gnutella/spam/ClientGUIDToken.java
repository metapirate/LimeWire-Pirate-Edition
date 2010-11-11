package com.limegroup.gnutella.spam;

/**
 * A token representing the client GUID of a responding client.
 */
public class ClientGUIDToken extends KeywordToken {

    /**
     * Unlike an IP address, a client GUID should never be shared by spammers
     * and non-spammers, so we can give it a high weight as a spam indicator.
     */
    private static final float CLIENT_GUID_WEIGHT = 0.6f;
    
    ClientGUIDToken(String guid) {
        super(guid);
    }
    
    @Override
    public float getWeight() {
        return CLIENT_GUID_WEIGHT;
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ClientGUIDToken))
            return false;
        return keyword.equals(((ClientGUIDToken)o).keyword);
    }
    
    @Override
    public String toString() {
        return "guid " + keyword;
    }
}
