package com.limegroup.gnutella.spam;

/**
 * A token representing an IP address.
 */
public class AddressToken extends KeywordToken {

    /**
     * A node is unlikely to return spam and non-spam results in the same
     * session. However, the user may mark unwanted results as spam even if
     * they don't come from a professional spammer, and the same address may
     * be shared by spammers and non-spammers due to NATs or dynamic IPs. We
     * can't use the port number to distinguish between NATed nodes because
     * it's too easy for a spammer to use multiple ports. Therefore we should
     * be a little bit cautious about using an address to identify spammers.  
     */
    private static final float ADDRESS_WEIGHT = 0.2f;

    public AddressToken(String address) {
        super(address);
    }

    @Override
    protected float getWeight() {
        return ADDRESS_WEIGHT;
    }

    @Override public boolean equals(Object o) {
        if(!(o instanceof AddressToken))
            return false;
        return keyword.equals(((AddressToken)o).keyword);
    }

    @Override
    public String toString() {
        return "address " + keyword;
    }
}
